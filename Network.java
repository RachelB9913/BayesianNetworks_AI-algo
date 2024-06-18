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


    public Variable getVariable(String name) {
        return variables.get(name);
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

    public void sortFactors(ArrayList<Factor> factors) {
        int n = factors.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                Factor f1 = new Factor(this);
                f1.setTheCPT(factors.get(j).getCPT());
                Factor f2 = new Factor(this);
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

    public String toString() {
        StringBuilder str = new StringBuilder("the network is: \n");
        for (Variable var : this.variables.values()) {
            var.setCPT(var.getName(),this);
            str.append(var).append("\n");
        }
        return str.toString();
    }
}