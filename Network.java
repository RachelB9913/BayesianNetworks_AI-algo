import java.util.*;

public class Network {

    private HashMap<String, Variable> variables;
    private Map<String, Map<Map<String, String>, Double>> CPTtables;

    public Network() {
        variables = new HashMap<>();
        CPTtables = new HashMap<>();
    }

    public Network copy(Network newNetwork) {
        this.variables = new HashMap<>(newNetwork.variables);
        this.CPTtables = new HashMap<>(newNetwork.CPTtables);
        return newNetwork;
    }

    public List<Variable> getVariables() {
        return new ArrayList<>(variables.values());
    }

    public String findName(Map<String, Map<Map<String, String>, Double>> CPTtables, Map<Map<String, String>, Double> targetMap) {
        //run over the entry set of CPTtables
        for (Map.Entry<String, Map<Map<String, String>, Double>> entry : CPTtables.entrySet()) {
            if (entry.getValue().equals(targetMap)) { //compare the current map value with the target map
                return entry.getKey();
            }
        }
        //no match is found
        return null;
    }

    public HashMap<String, Variable> getMapOfVariables() {
        return variables;
    }


    public Variable getVariable(String name) {
        Variable var = variables.get(name);
        return var;
    }

    public void addVariable(Variable variable) {
        variables.put(variable.getName(), variable);
    }

    public Map<String, Map<Map<String, String>, Double>> getCPT_tables() {
        return CPTtables;
    }

    public void addCpt(String variable, Map<Map<String, String>, Double> cpt) {
        CPTtables.put(variable, cpt);
    }

    //given a set of variable it finds the CPT relevant for them
    public Map<Map<String, String>, Double> getCPTof(Set<Variable> variablesToCombine) {
        Map<Map<String, String>, Double> combinedCPT = new HashMap<>();

        // Iterate over each CPT in the CPTtables map
        for (Map<Map<String, String>, Double> cpt : CPTtables.values()) {
            // Check if the current CPT contains all the variables we're interested in
            boolean containsAllVariables = true;
            for (Variable var : variablesToCombine) {
                boolean variableFound = false;
                for (Map<String, String> theCPT : cpt.keySet()) {
                    if (theCPT.containsKey(var.getName())) {
                        variableFound = true;
                        break;
                    }
                }
                if (!variableFound) {
                    containsAllVariables = false;
                    break;
                }
            }

            // If all variables are present in the CPT, add its entries to the combined CPT
            if (containsAllVariables) {
                combinedCPT.putAll(cpt); // This merges the CPT entries into combinedCPT
            }
        }
        return combinedCPT;
    }

    public void sortFactors(ArrayList<Factor> factors) {
        int n = factors.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                Factor f1 = new Factor();
                f1.setTheCPT(factors.get(j).getCPT());
                Factor f2 = new Factor();
                f2.setTheCPT(factors.get(j + 1).getCPT());

                if (f1.compareTo(f2) > 0) {
                    // Swap factors[j] and factors[j + 1]
                    Factor temp = factors.get(j);
                    factors.set(j, factors.get(j + 1));
                    factors.set(j + 1, temp);
                }
            }
        }
    }

    //recursive function to find all the ancestors of a variable
    public List<String> getAncestors(String name){
        ArrayList<String> ans = new ArrayList<>(this.getVariable(name).getParents());
        ArrayList<String> theParents = new ArrayList<>(this.getVariable(name).getParents());
        for(String parent: ans){
            ArrayList<String> temp = new ArrayList<>(getAncestors(parent));
            for(String s: temp){
                if(!theParents.contains(s)){
                    theParents.add(s);
                }
            }
        }
        return theParents;
    }

    public ArrayList<Factor> copyFactors(ArrayList<Factor> factors){
        ArrayList<Factor> factorsCopy = new ArrayList<>();
        for(Factor factor: factors){
            Factor copyFactor = new Factor();
            copyFactor.setTheCPT(factor.getCPT());
            copyFactor.setName(factor.getName());
            factorsCopy.add(copyFactor);
        }
        return factorsCopy;
    }

    public String toString() {
        String str = "the network is: \n";
        for (Variable var : this.variables.values()) {
            var.setCPT(var.getName(),this);
            str += var.toString() + "\n";
        }
        return str;
    }
}