import java.util.*;

public class Network {

    private HashMap<String, Variable>  variables;
    private Map<String, Map<Map<String, String>, Double>> CPTtables;

    public Network() {
        variables = new HashMap<>();
        CPTtables = new HashMap<>();
    }

    public Network copy(Network network) {
        network.variables = new HashMap<>(network.variables);
        network.CPTtables = new HashMap<>(network.CPTtables);
        return network;
    }

    public List<Variable> getVariables() {
        return new ArrayList<>(variables.values());
    }

    public HashMap<String, Variable> getMapOfVariables() {
        return variables;
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

}
