import java.util.*;

public class Factor implements Comparable<Factor> {

    private String name;
    private static Network network;
    private List<Variable> factorVars;
    private Map<Map<String, String>, Double> theCPT;
    public static int mul = 0;
    public static int add = 0;

    public Factor(String name, Network network) {
        this.name = name;
        this.theCPT = network.getCPT_tables().get(name);
        this.factorVars = this.extractVariables(network);
    }

    public Factor() {
        this.theCPT = new HashMap<>(0);
        this.factorVars = this.extractVariables(network);

    }

    public Factor(Network network) {
        this.theCPT = new HashMap<>(0);
        this.factorVars = this.extractVariables(network);
//        this.factorVars = new ArrayList<>();
//        factorVars = extractVariables(this.theCPT,network);
    }

    public Factor(Network network, Map<Map<String, String>, Double> theCPT) {
        this.name = network.findName(network.getCPT_tables(), theCPT);
        this.theCPT = theCPT;
        this.factorVars = new ArrayList<>();
        factorVars = this.extractVariables(network);
    }

    //copy constructor
    public Factor(Factor other) {
        this.name = other.name;
        this.theCPT = other.theCPT;
        this.factorVars =this.extractVariables(network);
//        factorVars = extractVariables(this.theCPT,other.network);
//        System.out.println("theCPT "+this.theCPT);
//        System.out.println("factorVars "+this.factorVars);
    }

    public static void initializeCounters() {
        mul = 0;
        add = 0;
    }

    public boolean containsVar(String var) {
        return factorVars.contains(network.getVariable(var));
    }

    public int compareTo(Factor o) {
        if (this.theCPT.size() > o.theCPT.size()) {
            return 1;
        } else if (this.theCPT.size() < o.theCPT.size()) {
            return -1;
        } else {
            int current = this.asciiSum();
            int other = o.asciiSum();
            if (current > other) {
                return 1;
            } else if (current < other) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    //removes from the cpt all the maps where in the inner map Map<String, String>
    //the key is as the evidence variable but the value is not the outcome given
    public static void removeIrrelevant(Map<Map<String, String>, Double> currCPT, List<String> eviVar) {
        ArrayList<String> evidence = new ArrayList<>();
        for(String var : eviVar) {
            String[] e = var.split("=");
            evidence.addAll(Arrays.asList(e));
        }

        ArrayList<Map<String, String>> varsToRemove = new ArrayList<>();
        for (Map<String, String> key : currCPT.keySet()) {
            for (String s : key.keySet()) {
                for (int j = 0; j < evidence.size() - 1; j += 2) {
                    if (evidence.get(j).equals(s) && !evidence.get(j + 1).equals(key.get(s))) {
                        varsToRemove.add(key);
                    }
                }
            }
        }
        for (Map<String, String> innerMap : varsToRemove) {
            currCPT.remove(innerMap);
        }
//        System.out.println("cpt after remove irrelevant: " + currCPT);
    }

    //removes all entries from the CPT that involve any of the evidence variables
    public void removeEvi(ArrayList<String> evi) {
        for (String e : evi) {
            e = e.split("=")[0];
            ArrayList<Map<String, String>> toRemove = new ArrayList<>();
            for (Map<String, String> key : this.theCPT.keySet()) {
                if (key.containsKey(e)) {
                    toRemove.add(key);
                }
            }
            for (int i = 0; i < toRemove.size(); i++) {
                Double x = this.theCPT.get(toRemove.get(i));
                this.theCPT.remove(toRemove.get(i));
                toRemove.get(i).remove(e);
                this.theCPT.put(toRemove.get(i), x);
            }
            //this.factorVars.remove(network.getVariable(e));
        }
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Network getNetwork() {
        return network;
    }

    public static void setNetwork(Network network) {
        Factor.network = network;
    }

    public List<Variable> getFactorVars() {
        return factorVars;
    }

    public void setFactorVars(List<Variable> setFactorVars) {
      this.factorVars = new ArrayList<>();
      for(Variable var : setFactorVars) {
          this.factorVars.add(var);
      }
    }

    public Map<Map<String, String>, Double> getCPT() {
        return theCPT;
    }

    public void setTheCPT(Map<Map<String, String>, Double> theCPT) {
        this.theCPT = theCPT;
    }

    public void emptyCPT(){
        this.theCPT = new HashMap<>(0);
    }

    //works!! -
    //given a factor it finds the common variable with the current one
    public Set<Variable> findCommonVariables(Map<Map<String, String>, Double> cpt) {
        List<Variable> variables1 = extractVariables(network);
        List<Variable> variables2 = extractVariables(network);

        // Find the intersection of the two sets (common variables)
        Set<Variable> commonVariables = new HashSet<>(variables1);
        commonVariables.retainAll(variables2);

        return commonVariables;
    }

    //given a factor extract from it all the variables in it
    public List<Variable> extractVariables(Network network) {
        Set<String> variableNames = new HashSet<>();
        if(this.theCPT == null) {
            System.out.println("the factor is null");
        }

        for (Map<String, String> assignment : this.theCPT.keySet()) {
            variableNames.addAll(assignment.keySet());
        }
        List<Variable> variables = new ArrayList<>();
        for (String name : variableNames) {
            Variable v = network.getVariable(name);
            variables.add(v);
        }
        return variables;
    }

    //
    //eliminating a specific variable from each factor in the factor map while summing up the probabilities of
    //lines that share the same values for all other variables.
    public Factor eliminateVar(String variable) {
        Factor newFactor = new Factor();
        newFactor.setName("the last");

        for (Map.Entry<Map<String, String>, Double> entry : this.theCPT.entrySet()) {
            Map<String, String> currentAssignment = entry.getKey();
            Double probability = entry.getValue();

            Map<String, String> newAssignment = new HashMap<>(currentAssignment);
            newAssignment.remove(variable); //so in the new factor the given variable will not show

            if (newFactor.theCPT.containsKey(newAssignment)) { //if it exists, add the probability to its existing value
                newFactor.theCPT.put(newAssignment, newFactor.theCPT.get(newAssignment) + probability);
                Factor.add++;
            } else { //if it doesn't exist, put it with the current probability
                newFactor.theCPT.put(newAssignment, probability);
            }
        }
        Set<String> varNames = new HashSet<>();
        for (Map<String, String> map : newFactor.theCPT.keySet()) {
            varNames = map.keySet();
        }
        List<Variable> vars = new ArrayList<>();
        for (String name : varNames) {
            vars.add(network.getVariable(name));
        }
        newFactor.setFactorVars(vars);
        return newFactor;
    }


    public Factor joinFactors(Factor factor) {
        Map<Map<String, String>, Double> resultCPT = new HashMap<>();
        Factor resultFactor = new Factor();
        resultFactor.setName(this.getName()+factor.getName());

        Set<Variable> commonVariables = findCommonVariables(factor.theCPT);
        Set<String> allVariables = findAllVariables(factor.theCPT);

        // לולאת פור על כל הרשומות ב-CPT של הפקטור הנוכחי
        for (Map.Entry<Map<String, String>, Double> entry1 : this.getCPT().entrySet()) {
            // לולאת פור על כל הרשומות ב-CPT של הפקטור factor
            for (Map.Entry<Map<String, String>, Double> entry2 : factor.theCPT.entrySet()) {
                Map<String, String> assignment1 = entry1.getKey();
                Map<String, String> assignment2 = entry2.getKey();

                // בדיקה האם ההשמות תואמות לפי המשתנים המשותפים
                if (isConsistent(assignment1, assignment2, commonVariables)) {
                    // שילוב השמות ליצירת השמה משותפת
                    Map<String, String> combinedAssignment = new HashMap<>(assignment1);
                    combinedAssignment.putAll(assignment2);
                    combinedAssignment.keySet().retainAll(allVariables); // שמירה רק על המשתנים המשותפים

                    // חישוב ההסתברות המשולבת ועיגול ל-5 ספרות אחרי הנקודה
                    double combinedProbability = entry1.getValue() * entry2.getValue();
                    Factor.mul++;
//                    String roundedNumber = String.format("%.5f", combinedProbability);
//                    Double roundedProbability = Double.valueOf(roundedNumber);

                    // הוספת ההשמה המשולבת וההסתברות למפת התוצאה
                    resultCPT.put(combinedAssignment, combinedProbability);
                }
            }
        }
        Set<String> varNames = new HashSet<>();
        for (Map<String, String> map : resultCPT.keySet()) {
            varNames = map.keySet();
        }
        List<Variable> vars = new ArrayList<>();
        for (String name : varNames) {
            vars.add(network.getVariable(name));
        }
//        System.out.println("the NAMES of the variables of the new factor are: "+varNames);
//        System.out.println("the variables of the new factor are: "+vars);
        for(Variable var : vars) {
            resultFactor.addVar(var);
        }

        resultFactor.setTheCPT(resultCPT);
        return resultFactor;
    }

    public void normalize(){
        double sum=0;
        Set<Map<String, String>> keys = this.theCPT.keySet();
        for(Map<String, String> key : keys){
            double value = theCPT.get(key);
            sum+=value;
            Factor.add++;
        }
        for(Map<String, String> key : keys){
            double normValue = theCPT.get(key)/sum;
            this.theCPT.put(key, normValue);
        }
    }

    public void addVar (Variable var) {
        this.factorVars.add(var);
    }

    //finds all variables present in both factors
    private Set<String> findAllVariables(Map<Map<String, String>, Double> factor) {
        Set<String> allVariables = new HashSet<>();
        for (Map<String, String> assignment : this.getCPT().keySet()) {
            allVariables.addAll(assignment.keySet());
        }
        for (Map<String, String> assignment : factor.keySet()) {
            allVariables.addAll(assignment.keySet());
        }
        return allVariables;
    }

    private static boolean isConsistent(Map<String, String> assignment1, Map<String, String> assignment2, Set<Variable> commonVariables) {
        for (Variable Var : commonVariables) {
            String var = Var.getName();
            if (assignment1.containsKey(var) && assignment2.containsKey(var) && !assignment1.get(var).equals(assignment2.get(var))) {
                return false;
            }
        }
        return true;
    }

    public int asciiSum() {
        Set<Map.Entry<Map<String, String>, Double>> entries = this.theCPT.entrySet();
        Set<String> uniqueKeys = new HashSet<>();
        for (Map.Entry<Map<String, String>, Double> entry : entries) {
            Map<String, String> nestedMap = entry.getKey();
            uniqueKeys.addAll(nestedMap.keySet());
        }
//        System.out.println(uniqueKeys);
        int asciiSum = 0;
        for (String key : uniqueKeys) {
            asciiSum += getAsciiSum(key);
        }
        return asciiSum;
    }

    public int getAsciiSum(String str) {
        int sum = 0;
        for (char c : str.toCharArray()) {
            sum += (int) c;
        }
        return sum;
    }

    @Override
    public String toString() {
        return "Factor{" + name +" theCPT " + theCPT+ '}';
        // " factorVars: "+factorVars+" theCPT " + theCPT
    }
}
