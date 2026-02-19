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
}

