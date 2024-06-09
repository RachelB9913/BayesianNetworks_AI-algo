import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Ex1 {
    public static void main(String[] args) throws IOException {
        XMLParser parser = new XMLParser();

        try (BufferedReader reader = new BufferedReader(new FileReader("input2.txt"))) {
            String networkName = reader.readLine();
            Network network = parser.parseXML(networkName);
            String line;
            while ((line = reader.readLine()) != null) {
                // VARIABLE ELIMINATION query
                if (line.startsWith("P(")) {
                    Variable queryVar;
                    List<Variable> evidenceVars = new ArrayList<>();
                    List<Variable> hiddenVars = new ArrayList<>();

                    String[] parts = line.split("\\|"); // Split the query string into its components

                    // Extract the query variable and its outcome
                    String[] queryPart = parts[0].split("[=()]");
                    if (queryPart.length >= 3) {
                        queryVar = new Variable(queryPart[1]);
                        queryVar.addOutcome(queryPart[2]);
                    } else {
                        continue; //skip this line if it does not have the correct query format
                    }

                    // Split evidence and hidden parts
                    String[] evidenceAndHidden;
                    if (parts.length > 1) {
                        evidenceAndHidden = parts[1].split("\\)");
                    } else {
                        evidenceAndHidden = new String[]{""};
                    }

                    // Extract the evidence variables and their outcomes
                    if (evidenceAndHidden.length > 0) {
                        String[] evidences = evidenceAndHidden[0].split(",");
                        for (String evidence : evidences) {
                            if (!evidence.trim().isEmpty()) {
                                String[] evidenceDetails = evidence.trim().split("=");
                                if (evidenceDetails.length == 2) {
                                    Variable evidenceVariable = new Variable(evidenceDetails[0]);
                                    evidenceVariable.addOutcome(evidenceDetails[1]);
                                    evidenceVars.add(evidenceVariable);
                                }
                            }
                        }
                    }

                    // Extract the hidden variables
                    if (evidenceAndHidden.length > 1) {
                        String hiddenPart = evidenceAndHidden[1].trim();
                        if (!hiddenPart.isEmpty()) {
                            String[] hidden = hiddenPart.split("-");
                            for (String hiddenvar : hidden) {
                                if (!hiddenvar.trim().isEmpty()) {
                                    Variable hiddenVariable = new Variable(hiddenvar.trim());
                                    hiddenVars.add(hiddenVariable);
                                }
                            }
                        }
                    }

//                    // Print the main variables of the query
//                    System.out.println("VarEl query variable: " + queryVar +
//                            " \\ Evidence: " + evidenceVars + " \\ Hidden: " + hiddenVars);
                    // The salvation algorithm of the query:

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
//                            " \\ Target: " + targetNode + " \\ Variables: " + eviVar);
//
                    boolean flag = independence(srcNode, targetNode, network);
//                    System.out.println(flag);

                    if (flag) {
                        System.out.println("no");
                    } else {
                        System.out.println("yes");
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Something went wrong in reading the input file: " + e.getMessage());
        }
    }


    public static boolean independence(Variable source, Variable destination, Network network) {
        resetColors(network);
        Network newNetwork = network.copy(network); // Create a copy of the network to avoid modifying the original network
        Variable targetVariable = BBSolve(newNetwork, source, destination.getName());
//        System.out.println("----" + targetVariable);
//        System.out.println("----" + destination);
        return targetVariable == destination; //if the target variable is reached, it means they are not independent

    }

    public static Variable BBSolve(Network network, Variable source, String target) {
//        System.out.println("src  "+ source);
//        System.out.println("target  "+target);

        Queue<Variable> queue = new LinkedList<>();
        Variable curr = source;
        source.setColor(1);
        queue.add(curr);
        while (!queue.isEmpty()) {
            curr = queue.remove();
//            System.out.println("current is: " + curr);
            if (curr.getName().equals(target)) //if we reached the target
                return curr;

            if (curr.getColor() == 1 && !curr.isEvi()) {
                if (!curr.getChildren().isEmpty())
                    for (String childName : curr.getChildren()) {
                        Variable child = network.getVariable(childName);
                        if (child.getColor() == 0) {
                            child.setColor(2);
                            queue.add(child);
                            //System.out.println("1 - Added child: " + child + " Color: " + child.getColor());
                        }
                    }
                if (!curr.getParents().isEmpty()) {
                    for (String parentName : curr.getParents()) {
                        Variable parent = network.getVariable(parentName);
                        if (parent.getColor() == 0) {
                            parent.setColor(2);
                            queue.add(parent);
                            //System.out.println("2 - Added parent: " + parent + " Color: " + parent.getColor());
                        }
                    }
                }
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


//    public static void bayesBall(Network network, Variable srcVar, Variable targetVar) {
//        boolean flag = BBSolve(network, srcVar, targetVar, true);
//        if (flag) {
//            System.out.println("no");
//        } else {
//            System.out.println("yes");
//        }
//    }

