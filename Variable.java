
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class Variable {
    private String name;
    private List<String> outcomes;
    private List<Double> probabilities;
    private List<String> parents;
    private List<String> children;
    private boolean isEvi=false;
    private int color; //0=white(unvisited) , 1=black(visited from child), 2=gray(visited from parent)
    private Map<Map<String, String>, Double> CPTtable;
    private Network network;


    public Variable(String name, Network network) {
        this.name = name;
        this.outcomes = new ArrayList<>();
        this.probabilities = new ArrayList<>();
        this.parents = new ArrayList<>();
        this.children = new ArrayList<>();
        this.network = network;
        this.CPTtable = network.getCPT_tables().get(name);
//        System.out.println("the cpt table for variable: "+ this.name + " is "+this.CPTtable);
    }

    public Variable() {
        this.outcomes = new ArrayList<>();
        this.probabilities = new ArrayList<>();
        this.parents = new ArrayList<>();
        this.children = new ArrayList<>();
        this.CPTtable = new HashMap<>();
        this.network = null;
    }

    public Variable(Variable variable) {
        this.name = variable.name;
        this.outcomes = variable.outcomes;
        this.probabilities = variable.probabilities;
        this.parents = variable.parents;
        this.children = variable.children;
        this.CPTtable = variable.CPTtable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEvi() {
        return isEvi;
    }

    public void setEvi(boolean evi) {
        isEvi = evi;
    }

    public List<String> getOutcomes() {
        return outcomes;
    }

    public void addOutcome(String outcome) {
        outcomes.add(outcome);
    }

    public List<Double> getPro() {
        return probabilities;
    }

    public void setPro(List<Double> definitions) {
        this.probabilities = definitions;
    }

    public Map<Map<String, String>, Double> getCPT() {
        return CPTtable;
    }

    public void setCPT(String name, Network network) {
      this.CPTtable = network.getCPT_tables().get(name);
    }

    public List<String> getParents() {
        return parents;
    }

    public void addParent(String givenVar) {
        parents.add(givenVar);
    }

    public List<String> getChildren() {
        return children;
    }

    public List<Double> getProbabilities() {
        return probabilities;
    }

    public void setOutcomes(List<String> outcomes) {
        this.outcomes = outcomes;
    }

    public void setProbabilities(List<Double> probabilities) {
        this.probabilities = probabilities;
    }

    public void setParents(List<String> parents) {
        this.parents = parents;
    }

    public void setChildren(List<String> children) {
        this.children = children;
    }

    public void addChild(String child) {
        children.add(child);
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean isAncestorOf(Variable var,Network network){
        if (var.getParents().contains(this.name)) {
            return true;
        }
        for (String parentName : var.getParents()) {
            Variable parent = network.getVariable(parentName);
            if (parent != null && this.isAncestorOf(parent,network)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "{name='" + name + "', outcomes=" + outcomes+ ", cpt=" + CPTtable +"}";
//                ", parents=" + parents + ", children=" + children+ ", color="+color+ ", evi?:" +isEvi+"}";
    }
}
