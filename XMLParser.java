import org.w3c.dom.*;

import javax.xml.parsers.*;
import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class XMLParser {

    Network network = new Network();

    public Network parseXML(String filePath) {

        try {
            File inputFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            NodeList variableList = doc.getElementsByTagName("VARIABLE");
            for (int i = 0; i < variableList.getLength(); i++) {
                Node node = variableList.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    Variable variable = new Variable();
                    variable.setName(element.getElementsByTagName("NAME").item(0).getTextContent());

                    NodeList outcomeList = element.getElementsByTagName("OUTCOME");
                    for (int j = 0; j < outcomeList.getLength(); j++) {
                        variable.addOutcome(outcomeList.item(j).getTextContent());
                    }
                    network.addVariable(variable);
                }
            }

            NodeList definitionList = doc.getElementsByTagName("DEFINITION");
            for (int i = 0; i < definitionList.getLength(); i++) {
                Node node = definitionList.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String forVar = element.getElementsByTagName("FOR").item(0).getTextContent();
                    Variable variable = findVariable(forVar, network.getVariables());

                    NodeList givenList = element.getElementsByTagName("GIVEN");
                    for (int j = 0; j < givenList.getLength(); j++) {
                        variable.addParent(givenList.item(j).getTextContent());
                    }

                    String[] tableValues = element.getElementsByTagName("TABLE").item(0).getTextContent().split("\\s+");
                    List<Double> table = new ArrayList<>();
                    for (String value : tableValues) {
                        table.add(Double.parseDouble(value));
                    }
                    variable.setPro(table);
                    findChildren();
                }
            }
            populateCpts(network);
        } catch (Exception e) {
            System.out.println("something went wrong in parsing the XML file" + e.getMessage());
        }
        return network;
    }

    public Variable findVariable(String name, List<Variable> variables) {
        for (Variable variable : variables) {
            if (variable.getName().equals(name)) {
                return variable;
            }
        }
        return null;
    }

    //finds the children of the variables based on their parents.
    //if you are X's parent, add X to be your child
    public void findChildren() {
        List<Variable> variables = network.getVariables();
        for (int i = 0; i < variables.size(); i++) {
            Variable current = variables.get(i);
            List<String> parents = current.getParents();
            if (!parents.isEmpty()) {
                for (String parent : parents) {
                    Variable aba = findVariable(parent, variables);
                    if (!aba.getChildren().contains(current.getName())) {
                        aba.addChild(current.getName());
                    }
                }
            }
        }
    }

    private void populateCpts(Network network) {
        for (Variable variable : network.getVariables()) {
            Map<Map<String, String>, Double> cpt = new HashMap<>();
            buildCpt(variable, new HashMap<>(), 0, network, cpt, variable.getPro(), 0);
            network.addCpt(variable.getName(), cpt);
        }
    }

    private int buildCpt(Variable variable, Map<String, String> currentTable, int depth, Network network, Map<Map<String, String>, Double> cpt, List<Double> definitions, int index) {
        if (depth == variable.getParents().size()) {
            //go over all outcomes of the current variable
            for (String outcome : variable.getOutcomes()) {
                Map<String, String> theNewTable = new HashMap<>(currentTable);
                theNewTable.put(variable.getName(), outcome);
                cpt.put(theNewTable, definitions.get(index++));
            }
        } else {
            //process the current parent variable
            String givenVar = variable.getParents().get(depth);
            Variable givenVariable = findVariable(givenVar, network.getVariables());
            for (String outcome : givenVariable.getOutcomes()) {
                currentTable.put(givenVar, outcome);
                index = buildCpt(variable, currentTable, depth + 1, network, cpt, definitions, index);
                currentTable.remove(givenVar);
            }
        }
        return index;
    }
}