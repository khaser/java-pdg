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
}

