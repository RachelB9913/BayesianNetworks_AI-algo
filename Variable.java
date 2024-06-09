import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class Variable {
    private String name;
    private List<String> outcomes;
    private List<Double> cpt;
    private List<String> parents;
    private List<String> children;
    private boolean isEvi=false;
    private int color; //0=white(unvisited) , 1=black(visited from child), 2=gray(visited from parent)


    public Variable(String name) {
        this.name = name;
        outcomes = new ArrayList<>();
        cpt = new ArrayList<>();
        parents = new ArrayList<>();
        children = new ArrayList<>();
    }

    public Variable() {
        this.outcomes = new ArrayList<>();
        this.cpt = new ArrayList<>();
        this.parents = new ArrayList<>();
        this.children = new ArrayList<>();
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

    public List<Double> getCPT() {
        return cpt;
    }

    public void setCPT(List<Double> definitions) {
        this.cpt = definitions;
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

    public List<Double> getCpt() {
        return cpt;
    }

    public void setOutcomes(List<String> outcomes) {
        this.outcomes = outcomes;
    }

    public void setCpt(List<Double> cpt) {
        this.cpt = cpt;
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

    @Override
    public String toString() {
        return "{name='" + name + "', outcomes=" + outcomes+
                ", parents=" + parents + ", children=" + children+ ", color="+color+ ", evi?:" +isEvi+"}";
    }
}
