package ru.itmo.khaser.java_pdg;

public class PDGEdge {
    public enum EdgeType {
        CONTROL,
        DATA
    }

    final PDGNode source;
    final PDGNode target;
    final EdgeType type;
    final String label;

    public PDGEdge(PDGNode source, PDGNode target, EdgeType type, String label) {
        this.source = source;
        this.target = target;
        this.type = type;
        this.label = label;
    }

    @Override
    public String toString() {
        return source + " -> " + target + " [" + type + ": " + label + "]";
    }

    // @Override
    // public boolean equals(Object o) {
    //     if (this == o) return true;
    //     if (o == null || getClass() != o.getClass()) return false;
    //     PDGEdge pdgEdge = (PDGEdge) o;
    //     return source.equals(pdgEdge.source) &&
    //            target.equals(pdgEdge.target) &&
    //            type == pdgEdge.type &&
    //            label.equals(pdgEdge.label);
    // }
    //
    // @Override
    // public int hashCode() {
    //     int result = source.hashCode();
    //     result = 31 * result + target.hashCode();
    //     result = 31 * result + type.hashCode();
    //     result = 31 * result + label.hashCode();
    //     return result;
    // }
}

