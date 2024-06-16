
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


public class Ex1 {
    public static void main(String[] args) throws IOException {
        XMLParser parser = new XMLParser();

        try (BufferedReader reader = new BufferedReader(new FileReader("input.txt"))) {
            String networkName = reader.readLine();
            Network network = parser.parseXML(networkName);
            Factor.setNetwork(network);
            System.out.println(network);

            System.out.println();
            parser.printCpts(network);
            System.out.println();
            Factor f1 = new Factor("A", network);
            Factor f2 = new Factor("B", network);
            Factor f3 = new Factor("E", network);
            Factor f4 = new Factor("M", network);
            Factor f5 = new Factor("J", network);
            ArrayList<Factor> factors = new ArrayList<>();
            factors.add(f1);
            factors.add(f2);
            factors.add(f3);
            factors.add(f4);
            factors.add(f5);
            List<Variable> varss = f4.extractVariables(network);
            System.out.println("varss "+varss);
            Variable j = network.getVariable("J");
            System.out.println("j "+j);
            System.out.println("factor j" + f5);
//            System.out.println("before sorting" + factors);
//            int comp = f2.compareTo(f3);
//            System.out.println(comp);
//            Network.sortFactors(factors);
//            System.out.println("after sorting" + factors);
//            Map<Map<String, String>, Double> n = f4.joinFactors(f5.getCPT());
//            System.out.println(n);
//            System.out.println("mul " + Factor.mul);
            //check if is an ancestor
//            Variable v = new Variable("A",network);
//            boolean av1 = v.isAncestorOf(network.getVariable("M"));
//            System.out.println(av1);
//            boolean av2 = v.isAncestorOf(network.getVariable("E"));
//            System.out.println(av2);
//            System.out.println("the parent of the 0 variable are: "+network.getVariables().get(0).getParents());
//            System.out.println("the parent of the 1 variable are: "+network.getVariables().get(1).getParents());
//            Map<Map<String, String>, Double> factor5 = f5.getCPT();
//            System.out.println("factor 5: "+factor5);
//            ArrayList<String> ev1 = new ArrayList<>();
//            String j = "J";
//            ev1.add(j);
//            Factor.removeEvi(factor5,ev1);
//            Factor.removeIrrelevant(factor5,ev1);
//            System.out.println("after removing: "+factor5);
//            System.out.println();
//            Map<Map<String, String>, Double> factor1 = f1.getCPT();
//            System.out.println("factor 1: "+factor1);
//            ArrayList<String> ev2 = new ArrayList<>();
//            String a = "A=T";
//            ev2.add(a);
//            Factor.removeIrrelevant(factor1,ev2);
//            Factor.removeEvi(factor1,ev2);
//            System.out.println("after removing: "+factor1);

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

                    ArrayList<Factor> originalFactors = new ArrayList<>();
                    ArrayList<Map<Map<String, String>, Double>> allCPTs = new ArrayList<>(); //contains all the cpts of the network
                    for(Variable var : network.getVariables()){
                        var = network.getVariable(var.getName());
                        var.setCPT(var.getName(), network);
                        allCPTs.add(var.getCPT());
                        Factor factor = new Factor(var.getName(), network);
                        originalFactors.add(factor);
                        System.out.println("$$$$$$ORIGINAL FACTOR$$$$$$$ "+factor.getName()+" "+factor.getCPT());
                    }

                    // The salvation algorithm of the query:
                    String ans = VarElSolve(network, queryVar, evidenceVars, hiddenVars, originalFactors);
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
                            srcNode = parser.findVariable(nodes[0], network.getVariables());
                            targetNode = parser.findVariable(nodes[1], network.getVariables());
                        }
                    }
                    // Parse the variables
                    for (Variable var : network.getVariables()) { //reset all the variables to be not evidence
                        var.setEvi(false);
                    }
                    if (parts.length == 2) {
                        String[] givenConditions = parts[1].split(","); // the part after "|"
                        for (String condition : givenConditions) {
                            String[] variableParts = condition.split("=");
                            if (variableParts.length == 2) {
                                String var = variableParts[0];
                                Variable variable = parser.findVariable(var, network.getVariables());
                                variable.setEvi(true);
                                eviVar.add(variable); // Update the instance variables directly
                            }
                        }
                    }
                    // Print the main variables of the query
//                    System.out.println("BayesBall Query: " + " Source: " + srcNode +
//                                " \\ Target: " + targetNode + " \\ Variables: " + eviVar);
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
        for (Variable var : network.getVariables()) {
            var.setCPT(var.getName(), network);
            // If the variable is independent of the queryVar, it is not reachable
//            System.out.println("the query var in pre: " + network.getVariable(queryVar));
            if (independent(network.getVariable(queryVar), var, network).equals("yes")) {
                notReachable.add(var);
            }
        }
        //find non-relevant variables
        List<Variable> nonRelevantVars = new ArrayList<>();
        System.out.println("query var suppose to be relevant: " + queryVar);
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

    public static String VarElSolve(Network network, String queryVar, List<String> evidenceVars, List<String> hiddenVars, ArrayList<Factor> originalFactors) {
//        System.out.println("query var:" +queryVar);
        String p;
        String ans;
        List<String> vars = new ArrayList<>();
        vars = new ArrayList<>(network.getAncestors(queryVar.split("=")[0]));
        vars.add(queryVar.split("=")[0]);
        System.out.println("parent variables: "+ vars);

        //initialize lists for evidence and its outcomes
        ArrayList<String> evidence = new ArrayList<>();
        ArrayList<String> evidenceOutcome = new ArrayList<>();
        for (int i = 0; i < evidenceVars.size(); i++) {
            String[] e = evidenceVars.get(i).split("=");
            evidence.add(e[0]);
            evidenceOutcome.add(e[1]);
        }

        //initialize allFactors with factors ant their CPTs from the network
//        for (Variable var : network.getVariables()) {
//            allCPTs.add(network.getCPT_tables().get(var.getName()));
//            Factor curr = new Factor(var.getName(), network);
//            allFactors.add(curr);
//            System.out.println("$$$$$$$$$$$$$$$$$$$ "+var.getName()+" "+var.getCPT());
//
//        }
        ArrayList<Factor> allFactors = new ArrayList<>(0); //initializes to empty
        System.out.println("suppose to be empty: "+ allFactors);
        allFactors = network.copyFactors(originalFactors);
        System.out.println("now full: "+ allFactors);


        System.out.println("@@@@@@@@@@@@@@@@@@@@ originalFactors " + originalFactors );
        System.out.println("@@@@@@@@@@@@@@@@@@@@ allFactors "+allFactors);


        //setting the factor variables correctly for each factor
        for (Factor factor : allFactors) {
            factor.setFactorVars(factor.extractVariables(network));
            if(!factor.getFactorVars().contains(factor.getName())){
                factor.addVar(network.getVariable(factor.getName()));
            }
        }
//        for(Factor factor : allFactors){
//            System.out.println("for: "+factor.getName()+ "the vars are: "+factor.getFactorVars());
//        }

        System.out.println("allFactors before preProcess" + allFactors);
        ArrayList<Factor> relevant = preProcess(network, vars.get(0), evidence, hiddenVars);
        System.out.println("allFactors after preProcess" + relevant);

        //getting a list of the names of the evidence variables
        ArrayList<String> eviNames = new ArrayList<>();
        for (String evi : evidenceVars) {
            eviNames.add(evi);
        }

        System.out.println("--------------------------------------------------------------- ");
        System.out.println("before: ");
        for (Factor value : allFactors) {
            System.out.println(value);
        }
        //remove the irrelevant rows and the evidence
        for (Factor factor : allFactors) {
            Factor.removeIrrelevant(factor.getCPT(), evidenceVars);
            factor.removeEvi(eviNames);
        }
        System.out.println("after: ");
        for (Factor allFactor : allFactors) {
            System.out.println(allFactor);
        }
        System.out.println("--------------------------------------------------------------- ");


        //removing factors of size 1
        for (int i = 0; i < allFactors.size(); i++) {
            if (allFactors.get(i).getCPT().size() == 1) {
                allFactors.remove(i);
                if (i > 0) i--;
            }
        }
//        System.out.println("after removing of size 1: ");
//        for (Factor aFactor : allFactors) {
//            System.out.println(aFactor.getName()+ " size of cpt: "+aFactor.getCPT().size()+" "+aFactor.getCPT());
//        }

        // sorting all the factors by size and ASCII
        network.sortFactors(allFactors);
        System.out.println("all factors sorted: " + allFactors);

        Factor f1 = new Factor(network);
        System.out.println("factor 1: " + f1);
        Factor f2 = new Factor(network);
        System.out.println("factor 2: " + f2);
        int loop=0;
        while (loop<hiddenVars.size()) {
            String hiddenVar = hiddenVars.get(loop);
            System.out.println(" hidden: " + hiddenVar);

            for (int i = 0; i < allFactors.size(); i++) {
                Factor currentFactor = allFactors.get(i);
                ArrayList<Factor> containsHidden = new ArrayList<>();
                for(Factor factor : allFactors) {
                    if (factor.containsVar(hiddenVar)) {
                        containsHidden.add(factor);
                    }
                }
                System.out.println("containsHidden: " + containsHidden);
                if(containsHidden.size()==1){
                    System.out.println("containsHidden is of size (1): " + containsHidden.size() +"  "+ containsHidden);
                    Factor onlyFactor = containsHidden.get(0);
                    System.out.println("onlyFactor: " + onlyFactor + " hidden:"+hiddenVar);
                    Factor eliminatedFactor = onlyFactor.eliminateVar(hiddenVar);
                    System.out.println("after elimination: " + eliminatedFactor);
                    allFactors.remove(onlyFactor);
                    allFactors.add(eliminatedFactor);
                    break;
                }

                System.out.println("current factor: " + currentFactor);
                if (currentFactor.containsVar(hiddenVar)) {
                    System.out.println("current factor contains: " + hiddenVar + " f1 is: " + f1 + "  f2 is: " + f2);
                    if (f1.getCPT().isEmpty()) {
                        f1 = currentFactor;
                        System.out.println("changed f1: " + f1);

                    } else if (f2.getCPT().isEmpty()) {
                        f2 = currentFactor;
                        System.out.println("changed f2: " + f2);

                    } else {
                        Factor f = new Factor(f1.joinFactors(f2));
                        List<Variable> joinedVariables = f.extractVariables(network);
                        System.out.println("joinedVariables: " + joinedVariables);

                        System.out.println("joined: " + f);

                        allFactors.remove(f1); containsHidden.remove(f1);
                        allFactors.remove(f2); containsHidden.remove(f2);
                        allFactors.add(f); containsHidden.add(f);
                        network.sortFactors(allFactors);
                        f1 = new Factor(network);
                        f2 = new Factor(network);
                        i = 0;
                    }
                }
                if (!f1.getCPT().isEmpty() && !f2.getCPT().isEmpty()) {
                    Factor f = new Factor(f1.joinFactors(f2));
                    List<Variable> joinedVariables = f.extractVariables(network);
//                    System.out.println("joinedVariables: " + joinedVariables);
                    f.setFactorVars(joinedVariables);
                    System.out.println("joined: " + f);
                    allFactors.remove(f1);
                    System.out.println("f1 was removed " + allFactors);
                    allFactors.remove(f2);
                    System.out.println("f2 was removed " + allFactors);
                    allFactors.add(f);
                    System.out.println("after join, remove and add: " + allFactors);
                    network.sortFactors(allFactors);
                    System.out.println("after sort: " + allFactors);

                    f1 = new Factor(network);
                    f2 = new Factor(network);
                    i=-1;
                    System.out.println("size: "+allFactors.size()+" all factors first: "+allFactors.get(0));
                    System.out.println("after emptyCPT: f1 " + f1 + " f2 " + f2);
                    System.out.println();
                }
            }

            System.out.println("went over all factors for this hidden var " + hiddenVars.getFirst());

            //hiddenVars.remove(hiddenVars.getFirst());
            loop++;
            System.out.println("loop: " +loop);

            if(loop<hiddenVars.size()) {
                System.out.println("hidden vars now start with: " + hiddenVars.get(loop));
            }
        }

        System.out.println("the last factors are: " + allFactors);
        if(allFactors.size()>1) {
            int i=0;
            while (allFactors.size() > 1) {
                Factor fac1 = allFactors.get(i);
                Factor fac2 = allFactors.get(i+1);
                Factor joinedFac = fac1.joinFactors(fac2);
                allFactors.remove(fac1); allFactors.remove(fac2);
                allFactors.add(joinedFac);
                System.out.println("LAST: after join, remove and add: " + allFactors);
            }
        }
        Factor theNeeded = allFactors.get(0);
        System.out.println("theNeeded: " + theNeeded);
        theNeeded.normalize();
        System.out.println("afterNormalization: " + theNeeded);
        String[] query = queryVar.split("=");
        Map<String,String> theQuery = new HashMap<>();
        theQuery.put(query[0],query[1]);
        Double pro = theNeeded.getCPT().get(theQuery);
        p = String.format("%.5f", pro);

        ans = p + "," + Factor.add + "," + Factor.mul;
        return ans;
    }
}


//CHECK PRINTS!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
// For demonstration, print the parsed data
//        System.out.println("Variables:");
//        for (Variable var : network.getVariables()) {
//            //System.out.println("Name: " + var.getName() + ", Outcomes: " + var.getOutcomes());
//            System.out.println(var);
//        }
//        System.out.println();
//
//        System.out.println("CPTs:");
//        for (Map.Entry<String, Map<Map<String, String>, Double>> entry : network.getCPT_tables().entrySet()) {
//            System.out.println("Variable: " + entry.getKey());
//            for (Map.Entry<Map<String, String>, Double> cptEntry : entry.getValue().entrySet()) {
//                System.out.println("Given: " + cptEntry.getKey() + " => " + cptEntry.getValue());
//            }
//        }
////      System.out.println("CPTs:");
////      XMLParser.printCpts(network);
//        System.out.println();
//
//        Map<String, String> givenConditions = new HashMap<>();
////      givenConditions.put("B", "F");
//        givenConditions.put("A", "F");
//        Double probability = parser.getProbability(network, "J", "F", givenConditions);
//        System.out.println("you get:  " + probability);

//SOME MORE CHECKS!!!!!!!!! - DELETE AFTER
//        System.out.println();
//        Variable v1 = XMLParser.findVariable("E",network.getVariables());
//        Variable v2 = XMLParser.findVariable("A",network.getVariables());
//        Variable v3 = XMLParser.findVariable("M",network.getVariables());
//        System.out.println(v1);
//        System.out.println(v2);
//        System.out.println(v3);



