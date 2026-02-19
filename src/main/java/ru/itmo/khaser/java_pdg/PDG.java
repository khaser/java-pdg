package ru.itmo.khaser.java_pdg;

import java.util.List;

public class PDG {
    final List<PDGNode> nodes;
    final List<PDGEdge> edges;

    public PDG(List<PDGNode> nodes, List<PDGEdge> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }
}

