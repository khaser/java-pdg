package ru.itmo.khaser.java_pdg;

public class DotExporter {

    public String export(PDG pdg) {
        StringBuilder sb = new StringBuilder();

        sb.append("digraph PDG {\n");
        sb.append("  rankdir=TB;\n");
        sb.append("  node [shape=box];\n");
        sb.append("\n");

        for (PDGNode node : pdg.nodes) {
            if (!node.reachable) continue;
            sb.append("  ").append(nodeId(node)).append(" [label=\"");
            sb.append(escapeLabel(node.label));
            sb.append("\"];\n");
        }

        sb.append("\n");

        for (PDGEdge edge : pdg.edges) {
            // TODO: workaround
            if (edge.type == PDGEdge.EdgeType.DATA) {
                continue;
            }

            sb.append("  ").append(nodeId(edge.source));
            sb.append(" -> ");
            sb.append(nodeId(edge.target));
            sb.append(" [label=\"");

            if (edge.type == PDGEdge.EdgeType.CONTROL) {
                // sb.append("ctrl");
                // if (!edge.label.isEmpty()) {
                //     sb.append(": ").append(escapeLabel(edge.label));
                // }
            } else {
                sb.append("data: ").append(escapeLabel(edge.label));
            }

            sb.append("\"");

            if (edge.type == PDGEdge.EdgeType.CONTROL) {
                sb.append(", color=blue");
            } else {
                sb.append(", color=red, style=dashed");
            }

            sb.append("];\n");
        }

        sb.append("}\n");

        return sb.toString();
    }

    private String nodeId(PDGNode node) {
        return "node" + node.id;
    }

    private String escapeLabel(String label) {
        return label.replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "");
    }
}

