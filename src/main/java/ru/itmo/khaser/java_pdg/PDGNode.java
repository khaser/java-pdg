package ru.itmo.khaser.java_pdg;

import com.github.javaparser.ast.stmt.Statement;

public class PDGNode {
    final int id;
    final Statement statement;
    final String label;

    public PDGNode(int id, Statement statement, String label) {
        this.id = id;
        this.statement = statement;
        this.label = label;
    }

    @Override
    public String toString() {
        return "Node" + id + ": " + label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PDGNode pdgNode = (PDGNode) o;
        return id == pdgNode.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}

