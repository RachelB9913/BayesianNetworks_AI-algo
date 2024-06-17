import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Ex1 {
    public static void main(String[] args) throws IOException {
        XMLParser parser = new XMLParser();

        try (BufferedReader reader = new BufferedReader(new FileReader("input2.txt"))) {
            String networkName = reader.readLine();
            Network mainNetwork = parser.parseXML(networkName);
//            System.out.println(mainNetwork);

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

                    // Print the main variables of the query
                    System.out.println("VarEl: variable: " + queryVar + " \\ Evidence: " + evidenceVars + " \\ Hidden: " + hiddenVars);
//                    System.out.println("the query var CPT before ans is: " + mainNetwork.getVariable(queryVar.split("=")[0]).getCPT());

                    //the salvation algorithm of the query:
                    String ans = VarElSolve(networkName, queryVar, evidenceVars, hiddenVars);//, originalFactors);
                    System.out.println(ans);
                    Factor.initializeCounters();
                    System.out.println();


                } else { //BAYES BALL query
                    Variable targetNode = new Variable();
                    Variable srcNode = new Variable();
                    List<Variable> eviVar = new ArrayList<>();

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
                                eviVar.add(variable); // Update the instance variables directly
                            }
                        }
                    }
//                     Print the main variables of the query
//                    System.out.println("BayesBall Query: " + " Source: " + srcNode +
//                                " \\ Target: " + targetNode + " \\ Variables: " + eviVar);

                    System.out.println(independent(srcNode,targetNode,mainNetwork));
                }
            }
        } catch (IOException e) {
            System.out.println("Something went wrong in reading the input file: " + e.getMessage());
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
//            System.out.println("current is: " + curr);
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
                            //System.out.println("1 - Added child: " + child + " Color: " + child.getColor());
                        }
                    }
                }
                if (!curr.getParents().isEmpty()) {
                    for (String parentName : curr.getParents()) {
                        Variable parent = network.getVariable(parentName);
                        if (parent.getColor() == 0) {
                            parent.setColor(1);
                            queue.add(parent);
                            //System.out.println("2 - Added parent: " + parent + " Color: " + parent.getColor());
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
                            //System.out.println("3 - Added child: " + child + " Color: " + child.getColor());
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
                            //System.out.println("4 - Added parent: " + parent + " Color: " + parent.getColor());
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
    public static ArrayList<Factor> preProcess(Network network, String queryVar, List<String> evidenceVars, List<String> hiddenVars) {
        ArrayList<Factor> theRelevantFactors = new ArrayList<>();

        //find not reachable variables
        List<Variable> notReachable = new ArrayList<>();
//        List<Variable> corrected = new ArrayList<>();
        for (Variable var : network.getVariables()) {
            var.setCPT(var.getName(), network);
            if (evidenceVars.contains(var.getName())) {
                var.setEvi(true);
            }
//            corrected.add(var);
        }
//        System.out.println("in pre process the network after evi: " + network);
        // If the variable is independent of the queryVar, it is not reachable
        for (Variable var :  network.getVariables()) {
            if (independent(network.getVariable(queryVar), var, network).equals("yes")) {
                notReachable.add(var);
            }
        }
        //find non-relevant variables
        List<Variable> nonRelevantVars = new ArrayList<>();
//        System.out.println("query var suppose to be relevant: " + queryVar);
        for (Variable var : network.getVariables()) {
            boolean isAvOfQueryOrEvidence = var.isAncestorOf(network.getVariable(queryVar), network);
//            System.out.println("Checking variable: " + var.getName() + " - Is ancestor of queryVar: " + isAvOfQueryOrEvidence);
            if (queryVar.equals(var.getName())) {
                isAvOfQueryOrEvidence = true;
            }

            if (!isAvOfQueryOrEvidence) {
                for (String evidenceVar : evidenceVars) {
                    Variable evi = network.getVariable(evidenceVar);
//                    System.out.println("evi:"+ evi);
                    if (var.isAncestorOf(evi, network) || evidenceVar.equals(var.getName())) {
                        isAvOfQueryOrEvidence = true;
                        //System.out.println("Variable " + var.getName() + " is ancestor of evidenceVar: " + evidenceVar.getName());
                        break;
                    }
                }
            }
            if (!isAvOfQueryOrEvidence) {
                nonRelevantVars.add(var);
                //System.out.println("Variable " + var.getName() + " added to non-relevant list.");
            }
        }
        System.out.println("The non-relevant variables are: " + nonRelevantVars);
        System.out.println("The not-reachable variables are: " + notReachable);

        //add relevant factors (CPTs) of reachable variables that are ancestors of the query or evidence variables
        for (Variable var : network.getVariables()) {
            var = network.getVariable(var.getName());
            var.setCPT(var.getName(), network);
//            System.out.println("preprocess:"+var);
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
                    Map<Map<String, String>, Double> cptOfVar = var.getCPT();
//                    System.out.println("cptOfVar " + cptOfVar);
                    Factor add = new Factor(var.getName(), network);
//                    System.out.println("add: " + add);
                    if (!theRelevantFactors.contains(add)) {
                        theRelevantFactors.add(add);
//                        System.out.println("Added CPT of variable: " + var.getName());
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
//        System.out.println("\nthe current network is: \n"+ currNet);

//        System.out.println("query var:" + queryVar);
        String queryName = queryVar.split("=")[0];
        Variable Var = new Variable(queryName, currNet);
//        System.out.println("the queryVar cpt is: " + Var.getCPT())


        ArrayList<Factor> originalFactors = new ArrayList<>();
        ArrayList<Map<Map<String, String>, Double>> allCPTs = new ArrayList<>(); //contains all the cpts of the network
        for (Variable var : currNet.getVariables()) {
            var = currNet.getVariable(var.getName());
            var.setCPT(var.getName(), currNet);
            allCPTs.add(var.getCPT());
            Factor factor = new Factor(var.getName(), currNet);
            originalFactors.add(factor);
//            System.out.println("$$$$$$ORIGINAL FACTOR$$$$$$$ "+factor.getName()+" "+factor.getCPT());
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

        ArrayList<Factor> allFactors = new ArrayList<>(0); //initializes to empty
//        System.out.println("suppose to be empty: " + allFactors);
        allFactors = currNet.copyFactors(originalFactors);
//        System.out.println("now full: " + allFactors);

        System.out.println("@@@@@@@@@@@@@@@@@@@@ originalFactors " + originalFactors);
        System.out.println("@@@@@@@@@@@@@@@@@@@@ allFactors " + allFactors);

//        System.out.println();
        System.out.println("allFactors before preProcess" + allFactors);
        ArrayList<Factor> relevant = preProcess(currNet, queryName, evidence, hiddenVars);
        System.out.println("relevant after preProcess" + relevant);
//        System.out.println();

        //getting a list of the names of the evidence variables
        ArrayList<String> eviNames = new ArrayList<>(evidenceVars);

//        System.out.println("--------------------------------------------------------------- ");
//        System.out.println("before removing all the non relevant: " + originalFactors);
//
//        System.out.println("relevant before: ");
//        for (Factor value : relevant) {
//            System.out.println(value);
//        }
        //remove the irrelevant rows and the evidence
        for (Factor factor : relevant) {
            factor.removeIrrelevant(evidenceVars);
            factor.removeEvi(eviNames);
        }
//        System.out.println("after: ");
//        for (Factor allFactor : relevant) {
//            System.out.println(allFactor);
//        }
//        System.out.println("after removing all the non relevant: " + relevant);
//        System.out.println("--------------------------------------------------------------- ");


        //removing factors of size 1
        for (int i = 0; i < relevant.size(); i++) {
            if (relevant.get(i).getCPT().size() == 1) {
                relevant.remove(i);
                if (i > 0) i--;
            }
        }

        // sorting all the factors by size and ASCII
        currNet.sortFactors(relevant);
//        System.out.println("all factors sorted: " + relevant);

        Factor f1 = new Factor(currNet);
//        System.out.println("factor 1: " + f1);
        Factor f2 = new Factor(currNet);
//        System.out.println("factor 2: " + f2);
        int loop = 0;
        while (loop < hiddenVars.size()) {
            String hiddenVar = hiddenVars.get(loop);
            System.out.println(" hidden: " + hiddenVar);

            for (int i = 0; i < relevant.size(); i++) {
                Factor currentFactor = relevant.get(i);
                ArrayList<Factor> containsHidden = new ArrayList<>();
                for (Factor factor : relevant) {
                    //System.out.println("current Var: "+factor.getName()+" check if contains "+hiddenVar+": " + currentFactor);
                    if (factor.containsVar(currNet, hiddenVar)) {
                        containsHidden.add(factor);
                    }
                }
//                System.out.println("containsHidden: " + containsHidden);
                if (containsHidden.size() == 1) {
//                    System.out.println("containsHidden is of size (1): " + containsHidden.size() + "  " + containsHidden);
                    Factor onlyFactor = containsHidden.get(0);
//                    System.out.println("onlyFactor: " + onlyFactor + " hidden:" + hiddenVar);
                    Factor eliminatedFactor = onlyFactor.eliminateVar(currNet, hiddenVar);
                    System.out.println("after elimination: " + eliminatedFactor);
                    relevant.remove(onlyFactor);
                    relevant.add(eliminatedFactor);
                    break;
                }

//                System.out.println("current factor: " + currentFactor);
                if (currentFactor.containsVar(currNet, hiddenVar)) {
//                    System.out.println("current factor contains: " + hiddenVar + " f1 is: " + f1 + "  f2 is: " + f2);
                    if (f1.getCPT().isEmpty()) {
                        f1 = currentFactor;
                        System.out.println("changed f1: " + f1);

                    } else if (f2.getCPT().isEmpty()) {
                        f2 = currentFactor;
                        System.out.println("changed f2: " + f2);
                    }
                    else {
                        Factor f = new Factor(f1.joinFactors(currNet, f2));
                        List<Variable> joinedVariables = f.extractVariables(currNet);
                        System.out.println("joinedVariables: " + joinedVariables);
                        System.out.println("joined: " + f);

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
                    List<Variable> joinedVariables = f.extractVariables(currNet);
                    System.out.println("joinedVariables: " + joinedVariables);
                    f.setFactorVars(joinedVariables);
                    System.out.println("joined: " + f);
                    relevant.remove(f1);
                    System.out.println("f1 was removed " + relevant);
                    relevant.remove(f2);
                    System.out.println("f2 was removed " + relevant);
                    relevant.add(f);
                    System.out.println("after join, remove and add: " + relevant);
                    currNet.sortFactors(relevant);
                    System.out.println("after sort: " + relevant);

                    f1 = new Factor(currNet);
                    f2 = new Factor(currNet);
                    i = -1;
//                    System.out.println("size: " + relevant.size() + " all factors first: " + relevant.get(0));
//                    System.out.println("after emptyCPT: f1 " + f1 + " f2 " + f2);
//                    System.out.println();
                }
            }

//            System.out.println("went over all factors for this hidden var " + hiddenVars.getFirst());
            loop++;
//            System.out.println("loop: " + loop);

//            if (loop < hiddenVars.size()) {
//                System.out.println("hidden vars now start with: " + hiddenVars.get(loop));
//            }
        }

//        System.out.println("the last factors are: " + relevant);
        if (relevant.size() > 1) {
            int i = 0;
            while (relevant.size() > 1) {
                Factor fac1 = relevant.get(i);
                Factor fac2 = relevant.get(i + 1);
                Factor joinedFac = fac1.joinFactors(currNet, fac2);
                relevant.remove(fac1);
                relevant.remove(fac2);
                relevant.add(joinedFac);
//                System.out.println("LAST: after join, remove and add: " + relevant);
            }
        }
        Factor theNeeded = relevant.get(0);
//        System.out.println("theNeeded: " + theNeeded);
        theNeeded.normalize();
//        System.out.println("afterNormalization: " + theNeeded);
        String[] query = queryVar.split("=");
        Map<String, String> theQuery = new HashMap<>();
        theQuery.put(query[0], query[1]);
        Double pro = theNeeded.getCPT().get(theQuery);
        p = String.format("%.5f", pro);
//        System.out.println("at the end the originals are: " + originalFactors);

        ans = p + "," + Factor.add + "," + Factor.mul;
        return ans;
    }
}
