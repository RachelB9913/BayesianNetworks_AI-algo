import java.io.*;
import java.util.*;

public class Ex1 {
    public static void main(String[] args) throws IOException {
        XMLParser parser = new XMLParser();
        String filePath = "output.txt";
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true));

        try (BufferedReader reader = new BufferedReader(new FileReader("input3.txt"))) {
            String networkName = reader.readLine();
            Network mainNetwork = parser.parseXML(networkName);

            String line;
            while ((line = reader.readLine()) != null) {
                // VARIABLE ELIMINATION query
                if (line.startsWith("P(")) {
                    int startIndex = line.indexOf('(') + 1;
                    int endIndex = line.indexOf(')');
                    String innerQuery = line.substring(startIndex, endIndex);

                    // Split the inner part to get queryVar and evidenceVars
                    String[] parts = innerQuery.split("\\|");
                    String queryVar = parts[0];
                    List<String> evidenceVars = new ArrayList<>();
                    if (parts.length > 1) {
                        evidenceVars = Arrays.asList(parts[1].split(","));
                    }

                    // Split the part after the parentheses to get hiddenVars
                    String hiddenVarsPart = line.substring(endIndex + 2);  // Skip the space after ')'
                    List<String> hiddenVars = Arrays.asList(hiddenVarsPart.split("-"));

                    //the salvation algorithm of the query:
                    String VEans = VarElSolve(networkName, queryVar, evidenceVars, hiddenVars);//, originalFactors);
                    writer.append(VEans);
                    writer.newLine();
                    System.out.println(VEans);
                    Factor.initializeCounters();

                } else { //BAYES BALL query
                    Variable targetNode = new Variable();
                    Variable srcNode = new Variable();

                    String[] parts = line.split("\\|"); // Split the query by '|'
                    if (parts.length >= 1) {
                        String[] nodes = parts[0].split("-");
                        if (nodes.length == 2) {
                            srcNode = parser.findVariable(nodes[0], mainNetwork.getVariables());
                            targetNode = parser.findVariable(nodes[1], mainNetwork.getVariables());
                        }
                    }
                    // Parse the variables
                    for (Variable var : mainNetwork.getVariables()) { //reset all the variables to be not evidence
                        var.setEvi(false);
                    }
                    if (parts.length == 2) {
                        String[] givenConditions = parts[1].split(","); // the part after "|"
                        for (String condition : givenConditions) {
                            String[] variableParts = condition.split("=");
                            if (variableParts.length == 2) {
                                String var = variableParts[0];
                                Variable variable = parser.findVariable(var, mainNetwork.getVariables());
                                variable.setEvi(true);
                            }
                        }
                    }
                    String BBans = independent(srcNode,targetNode,mainNetwork);
                    writer.append(BBans);
                    writer.newLine();
                    System.out.println(BBans);
                }
            }
        } catch (IOException e) {
            System.out.println("Something went wrong in reading the input file: " + e.getMessage());
        }
        finally {
            writer.close();
        }
    }

    public static String independent(Variable source, Variable target, Network network) {
        Network newNetwork = network.copy(network); // Create a copy of the network to avoid modifying the original network
        resetColors(newNetwork);
        Variable targetVariable = BBSolve(newNetwork, source, target.getName());
        String ans = "";
        boolean flag = targetVariable != target;
        if (flag) { //if the target variable is reached, it means they are not independent
            ans = "yes";
        } else {
            ans = "no";
        }
        return ans;
    }

    public static Variable BBSolve(Network network, Variable source, String target) {
        Queue<Variable> queue = new LinkedList<>();
        Variable curr = source;
        source.setColor(1);
        queue.add(curr);
        while (!queue.isEmpty()) {
            curr = queue.remove();
            if (curr.getName().equals(target)) //if we reached the target
                return curr;

            //if you have been visited in from children, and you are not an evidence node
            if (curr.getColor() == 1 && !curr.isEvi()) {
                if (!curr.getChildren().isEmpty()) {
                    for (String childName : curr.getChildren()) {
                        Variable child = network.getVariable(childName);
                        if (child.getColor() == 0) {
                            child.setColor(2);
                            queue.add(child);
                        }
                    }
                }
                if (!curr.getParents().isEmpty()) {
                    for (String parentName : curr.getParents()) {
                        Variable parent = network.getVariable(parentName);
                        if (parent.getColor() == 0) {
                            parent.setColor(1);
                            queue.add(parent);
                        }
                    }
                }
                //if you have been visited in from parent, and you are not an evidence node
            } else if (curr.getColor() == 2 && !curr.isEvi()) {
                if (!curr.getChildren().isEmpty()) {
                    for (String childName : curr.getChildren()) {
                        Variable child = network.getVariable(childName);
                        if (child.getColor() != 2) {
                            child.setColor(2);
                            queue.add(child);
                        }
                    }
                }
                //if you have been visited in from parent, and you are an evidence node
            } else if (curr.getColor() == 2 && curr.isEvi()) {
                if (!curr.getParents().isEmpty()) {
                    for (String parentName : curr.getParents()) {
                        Variable parent = network.getVariable(parentName);
                        if (parent.getColor() != 1) {
                            parent.setColor(1);
                            queue.add(parent);
                        }
                    }
                }
            }
        }
        return null; //we didn't reach the target
    }

    //reset color for all variables in the network
    private static void resetColors(Network network) {
        for (Variable var : network.getVariables()) {
            var.setColor(0);
        }
    }

    /////////////////////Variable Elimination/////////////////////
    public static ArrayList<Factor> preProcess(Network network, String queryVar, List<String> evidenceVars) {
        ArrayList<Factor> theRelevantFactors = new ArrayList<>();

        //find not reachable variables
        List<Variable> notReachable = new ArrayList<>();
        for (Variable var : network.getVariables()) {
            var.setCPT(var.getName(), network);
            if (evidenceVars.contains(var.getName())) {
                var.setEvi(true);
            }
        }
        // If the variable is independent of the queryVar, it is not reachable
        for (Variable var :  network.getVariables()) {
            if (independent(network.getVariable(queryVar), var, network).equals("yes")) {
                notReachable.add(var);
            }
        }
        //find non-relevant variables
        List<Variable> nonRelevantVars = new ArrayList<>();
        for (Variable var : network.getVariables()) {
            boolean isAvOfQueryOrEvidence = var.isAncestorOf(network.getVariable(queryVar), network);
            if (queryVar.equals(var.getName())) {
                isAvOfQueryOrEvidence = true;
            }

            if (!isAvOfQueryOrEvidence) {
                for (String evidenceVar : evidenceVars) {
                    Variable evi = network.getVariable(evidenceVar);
                    if (var.isAncestorOf(evi, network) || evidenceVar.equals(var.getName())) {
                        isAvOfQueryOrEvidence = true;
                        break;
                    }
                }
            }
            if (!isAvOfQueryOrEvidence) {
                nonRelevantVars.add(var);
            }
        }

        //add relevant factors (CPTs) of reachable variables that are ancestors of the query or evidence variables
        for (Variable var : network.getVariables()) {
            var = network.getVariable(var.getName());
            var.setCPT(var.getName(), network);
            if (!nonRelevantVars.contains(var) && !notReachable.contains(var)) {
                boolean isAncestorOfQueryOrEvidence = var.isAncestorOf(network.getVariable(queryVar), network);
                if (queryVar.equals(var.getName())) {
                    isAncestorOfQueryOrEvidence = true;
                }
                if (!isAncestorOfQueryOrEvidence) {
                    for (String evidenceVar : evidenceVars) {
                        if (var.isAncestorOf(network.getVariable(evidenceVar), network) || evidenceVar.equals(var.getName())) {
                            isAncestorOfQueryOrEvidence = true;
                            break;
                        }
                    }
                }
                if (isAncestorOfQueryOrEvidence) {
                    Factor add = new Factor(var.getName(), network);
                    if (!theRelevantFactors.contains(add)) {
                        theRelevantFactors.add(add);
                    }
                }
            }
        }
        return theRelevantFactors;
    }

    public static String VarElSolve(String networkName, String queryVar, List<String> evidenceVars, List<String> hiddenVars) {//, ArrayList<Factor> originalFactors) {
        XMLParser parser = new XMLParser();
        Network currNet = parser.parseXML(networkName);
        String p;
        String ans;
        String queryName = queryVar.split("=")[0];

        ArrayList<Factor> originalFactors = new ArrayList<>();
        for (Variable var : currNet.getVariables()) {
            var = currNet.getVariable(var.getName());
            var.setCPT(var.getName(), currNet);
            Factor factor = new Factor(var.getName(), currNet);
            originalFactors.add(factor);
        }
        //initialize lists for evidence and its outcomes
        ArrayList<String> evidence = new ArrayList<>();
        ArrayList<String> evidenceOutcome = new ArrayList<>();
        for (String evidenceVar : evidenceVars) {
            String[] e = evidenceVar.split("=");
            evidence.add(e[0]);
            evidenceOutcome.add(e[1]);
        }

        //check if the answer to the query already exists in the cpts
        for(Factor factor : originalFactors){
            Map<Map<String,String>,Double> currCPT = factor.getCPT();
            Set<Map<String,String>> inners = currCPT.keySet();
            Map<String,String> querySet = new HashMap<>();
            querySet.put(queryName,queryVar.split("=")[1]); //the query itself - for exapmle: J=T
            for(int i=0; i<evidenceVars.size();i++){
                querySet.put(evidence.get(i),evidenceOutcome.get(i));
            }
            if(inners.contains(querySet)){
                double pro = currCPT.get(querySet);

                ans = pro + "," + Factor.add + "," + Factor.mul;
                return ans;
            }
        }

        ArrayList<Factor> relevant = preProcess(currNet, queryName, evidence);

        //getting a list of the names of the evidence variables
        ArrayList<String> eviNames = new ArrayList<>(evidenceVars);

        //remove the irrelevant rows and the evidence
        for (Factor factor : relevant) {
            factor.removeIrrelevant(evidenceVars);
            factor.removeEvi(eviNames);
        }

        //removing factors of size 1
        for (int i = 0; i < relevant.size(); i++) {
            if (relevant.get(i).getCPT().size() == 1) {
                relevant.remove(i);
                if (i > 0) i--;
            }
        }

        currNet.sortFactors(relevant); // sorting all the factors by size and ASCII

        Factor f1 = new Factor(currNet);
        Factor f2 = new Factor(currNet);
        int loop = 0;
        while (loop < hiddenVars.size()) {
            String hiddenVar = hiddenVars.get(loop);

            for (int i = 0; i < relevant.size(); i++) {
                Factor currentFactor = relevant.get(i);
                ArrayList<Factor> containsHidden = new ArrayList<>();
                for (Factor factor : relevant) {
                    if (factor.containsVar(currNet, hiddenVar)) {
                        containsHidden.add(factor);
                    }
                }
                if (containsHidden.size() == 1) {
                    Factor onlyFactor = containsHidden.getFirst();
                    Factor eliminatedFactor = onlyFactor.eliminateVar(currNet, hiddenVar);
                    relevant.remove(onlyFactor);
                    relevant.add(eliminatedFactor);
                    currNet.sortFactors(relevant);
                    break;
                }

                if (currentFactor.containsVar(currNet, hiddenVar)) {
                    if (f1.getCPT().isEmpty()) {
                        f1 = currentFactor;
                    }
                    else if (f2.getCPT().isEmpty()) {
                        f2 = currentFactor;
                    }
                    else {
                        Factor f = new Factor(f1.joinFactors(currNet, f2));
                        relevant.remove(f1);
                        containsHidden.remove(f1);
                        relevant.remove(f2);
                        containsHidden.remove(f2);
                        relevant.add(f);
                        containsHidden.add(f);
                        currNet.sortFactors(relevant);
                        f1 = new Factor(currNet);
                        f2 = new Factor(currNet);
                        i = 0;
                    }
                }
                if (!f1.getCPT().isEmpty() && !f2.getCPT().isEmpty()) {
                    Factor f = new Factor(f1.joinFactors(currNet, f2));
                    List<Variable> joinedVariables = f.extractVariables();
                    f.setFactorVars(joinedVariables);
                    relevant.remove(f1);
                    relevant.remove(f2);
                    relevant.add(f);
                    currNet.sortFactors(relevant);

                    f1 = new Factor(currNet);
                    f2 = new Factor(currNet);
                    i = -1;
                }
            }
            loop++;
        }

        if (relevant.size() > 1) {
            int i = 0;
            while (relevant.size() > 1) {
                Factor fac1 = relevant.get(i);
                Factor fac2 = relevant.get(i + 1);
                Factor joinedFac = fac1.joinFactors(currNet, fac2);
                relevant.remove(fac1);
                relevant.remove(fac2);
                relevant.add(joinedFac);
            }
        }
        Factor theNeeded = relevant.getFirst();
        theNeeded.normalize();
        String[] query = queryVar.split("=");
        Map<String, String> theQuery = new HashMap<>();
        theQuery.put(query[0], query[1]);
        Double pro = theNeeded.getCPT().get(theQuery);
        p = String.format("%.5f", pro);

        ans = p + "," + Factor.add + "," + Factor.mul;
        return ans;
    }
}
