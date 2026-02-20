package ru.itmo.khaser.java_pdg;

import com.github.javaparser.Position;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.utils.Pair;

import java.util.*;

public class PDGBuilder {
    private final MethodDeclaration method;
    private final List<PDGNode> nodes;
    private final List<PDGEdge> edges;
    private int nodeIdCounter;
    private final Map<Pair<Statement, Position>, PDGNode> stmtToNode;
    private final Map<PDGNode, Set<String>> nodeToVarsUsed;
    private final Map<PDGNode, Set<String>> nodeToVarsDefined;

    class CFGContext {
        final PDGNode cont;
        final PDGNode curLoopNode;
        final PDGNode contForCurLoopNode;
        final PDGNode methodExit;
        CFGContext(PDGNode cont, PDGNode curLoopNode, PDGNode contForCurLoopNode, PDGNode methodExit) {
            this.cont = cont;
            this.curLoopNode = curLoopNode;
            this.contForCurLoopNode = contForCurLoopNode;
            this.methodExit = methodExit;
        }
    };

    public PDGBuilder(MethodDeclaration method) {
        this.method = method;
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.nodeIdCounter = 0;
        this.stmtToNode = new HashMap<>();
        this.nodeToVarsUsed = new HashMap<>();
        this.nodeToVarsDefined = new HashMap<>();
    }

    public PDG build() {
        PDGNode entryNode = createNode(null, "ENTRY: " + method.getSignature().asString());
        PDGNode exitNode = createNode(null, "EXIT: " + method.getSignature().asString());
        entryNode.reachable = true;
        exitNode.reachable = true;

        if (method.getBody().isPresent()) {
            BlockStmt body = method.getBody().get();
            addControlEdge(entryNode, createNodesRec(body));
            processBlockStmt(body, new CFGContext(exitNode, null, null, exitNode));
        }

        addDataDependencies();

        return new PDG(nodes, edges);
    }

    // Returns entry point to created structure
    private PDGNode createNodesRec(Statement stmt) {
        if (stmt instanceof BlockStmt) {
            var blk = (BlockStmt) stmt;
            List<Statement> statements = blk.getStatements();
            PDGNode ret = null;
            for (var i : statements) {
                var tmp = createNodesRec(i);
                if (ret == null) ret = tmp;
            }
            return ret;
        } else if (stmt instanceof IfStmt) {
            var if_stmt = (IfStmt) stmt;
            String label = "if (" + if_stmt.getCondition().toString() + ")";
            var if_node = createNode(if_stmt, label);
            Statement then_stmt = if_stmt.getThenStmt();
            var then_node = createNodesRec(then_stmt);
            addControlEdge(if_node, then_node);
            if (if_stmt.getElseStmt().isPresent()) {
                var else_stmt = if_stmt.getElseStmt().get();
                var else_node = createNodesRec(else_stmt);
                addControlEdge(if_node, else_node);
            }
            return if_node;
        } else if (stmt instanceof WhileStmt) {
            var while_stmt = (WhileStmt) stmt;
            String label = "while (" + while_stmt.getCondition().toString() + ")";
            var while_node = createNode(while_stmt, label);
            Statement body_stmt = while_stmt.getBody();
            var body_node = createNodesRec(body_stmt);
            addControlEdge(while_node, body_node);
            return while_node;
        } else if (stmt instanceof ForStmt) {
            var for_stmt = (ForStmt) stmt;
            String label = stmt.toString().split("\n")[0];
            var for_node = createNode(for_stmt, label);
            Statement body_stmt = for_stmt.getBody();
            var body_node = createNodesRec(body_stmt);
            addControlEdge(for_node, body_node);
            return for_node;
        } else {
            return createNode(stmt, stmt.getBegin().get().line + ": " + stmt.toString().trim());
        }
    }

    private PDGNode createNode(Statement stmt, String label) {
        PDGNode node = new PDGNode(nodeIdCounter++, stmt, label);
        nodes.add(node);
        if (stmt != null) {
            stmtToNode.put(new Pair<Statement, Position>(stmt, stmt.getBegin().get()), node);
        }
        return node;
    }

    // Return true if exists control flow that reaches ctx.cont
    private boolean processBlockStmt(BlockStmt blk, CFGContext ctx) {
        List<Statement> statements = blk.getStatements();
        for (int i = 0; i < statements.size(); ++i) {
            boolean ret = processStatement(statements.get(i),
                            new CFGContext(
                                (i != statements.size() - 1 ? stmtToNode(statements.get(i + 1)) : ctx.cont),
                                ctx.curLoopNode,
                                ctx.contForCurLoopNode,
                                ctx.methodExit
                          ));
            if (!ret) { return false; }
        }
        return true;
    }

    private PDGNode stmtToNode(Statement stmt) {
        return stmtToNode.get(new Pair<Statement, Position>(stmt, stmt.getBegin().get()));
    }

    // Return true if exists control flow that reaches ctx.cont
    private boolean processStatement(Statement stmt, CFGContext ctx) {
        var node = stmtToNode(stmt);
        if (node != null) { // we don't have nodes for BlockStmt
            node.reachable = true;
        }
        if (stmt instanceof ExpressionStmt) {
            analyzeVariableUsage(node, stmt);
            addControlEdge(node, ctx.cont);
            return true;
        } else if (stmt instanceof IfStmt) {
            var if_stmt = (IfStmt) stmt;
            analyzeVariableUsage(node, if_stmt.getCondition());
            Statement then_stmt = if_stmt.getThenStmt();
            var new_ctx = new CFGContext(ctx.cont, ctx.curLoopNode, ctx.contForCurLoopNode, ctx.methodExit);
            boolean ret = processStatement(then_stmt, new_ctx);

            if (if_stmt.getElseStmt().isPresent()) {
                Statement else_stmt = if_stmt.getElseStmt().get();
                ret |= processStatement(else_stmt, new_ctx);
            } else {
                addControlEdge(node, ctx.cont);
                ret = true;
            }
            return ret;
        } else if (stmt instanceof WhileStmt) {
            var while_stmt = (WhileStmt) stmt;
            analyzeVariableUsage(node, while_stmt.getCondition());
            Statement body = while_stmt.getBody();
            var new_ctx = new CFGContext(node, node, ctx.cont, ctx.methodExit);
            addControlEdge(node, ctx.cont);
            processStatement(body, new_ctx);
            return true;
        } else if (stmt instanceof ForStmt) {
            var for_stmt = (ForStmt) stmt;
            for_stmt.getCompare().ifPresent(it -> analyzeVariableUsage(node, it));
            for_stmt.getUpdate().forEach(it -> analyzeVariableUsage(node, it));
            for_stmt.getInitialization().forEach(it -> analyzeVariableUsage(node, it));
            Statement body = for_stmt.getBody();
            var new_ctx = new CFGContext(node, node, ctx.cont, ctx.methodExit);
            addControlEdge(node, ctx.cont);
            processStatement(body, new_ctx);
            return true;
        } else if (stmt instanceof ReturnStmt) {
            addControlEdge(node, ctx.methodExit);
            analyzeVariableUsage(node, stmt);
            return false;
        } else if (stmt instanceof BlockStmt) {
            return processBlockStmt((BlockStmt) stmt, ctx);
        } else if (stmt instanceof BreakStmt) {
            addControlEdge(node, ctx.contForCurLoopNode);
            return false;
        } else if (stmt instanceof ContinueStmt) {
            addControlEdge(node, ctx.curLoopNode);
            return false;
        } else {
            return false;
        }
    }

    // private void processForStmt(ForStmt stmt, PDGNode controlParent) {
    //     String label = "for (...)";
    //     PDGNode forNode = createNode(stmt, label);
    //     addControlEdge(controlParent, forNode, "");
    //
    //     Set<String> usedVars = new HashSet<>();
    //     Set<String> definedVars = new HashSet<>();
    //
    //     for (Expression init : stmt.getInitialization()) {
    //         usedVars.addAll(extractUsedVariables(init));
    //         definedVars.addAll(extractDefinedVariables(init));
    //     }
    //
    //     if (stmt.getCompare().isPresent()) {
    //         usedVars.addAll(extractUsedVariables(stmt.getCompare().get()));
    //     }
    //
    //     for (Expression update : stmt.getUpdate()) {
    //         usedVars.addAll(extractUsedVariables(update));
    //     }
    //
    //     stmtToVarsUsed.put(stmt, usedVars);
    //     stmtToVarsDefined.put(stmt, definedVars);
    //
    //     for (String var : definedVars) {
    //         varToDefNodes.computeIfAbsent(var, k -> new ArrayList<>()).add(forNode);
    //     }
    //
    //     Statement body = stmt.getBody();
    //     processStatement(body, forNode);
    // }

    private void analyzeVariableUsage(PDGNode node, Node stmt) {
        Set<String> usedVars = extractUsedVariables(stmt);
        Set<String> definedVars = extractDefinedVariables(stmt);

        nodeToVarsUsed.put(node, usedVars);
        nodeToVarsDefined.put(node, definedVars);
    }

    private Set<String> extractUsedVariables(Node node) {
        Set<String> vars = new HashSet<>();
        node.findAll(NameExpr.class).forEach(name -> vars.add(name.getNameAsString()));
        return vars;
    }

    private Set<String> extractDefinedVariables(Node node) {
        Set<String> vars = new HashSet<>();

        node.findAll(VariableDeclarator.class).forEach(decl ->
            vars.add(decl.getNameAsString())
        );

        // Assignments
        node.findAll(AssignExpr.class).forEach(assign -> {
            if (assign.getTarget() instanceof NameExpr) {
                vars.add(((NameExpr) assign.getTarget()).getNameAsString());
            }
        });

        return vars;
    }

    private void addControlEdge(PDGNode source, PDGNode target) {
        if (source == null || target == null) return;
        edges.add(new PDGEdge(source, target, PDGEdge.EdgeType.CONTROL, ""));
    }

    private void addDataEdge(PDGNode source, PDGNode target, String varName) {
        if (source == target) return;
        edges.add(new PDGEdge(source, target, PDGEdge.EdgeType.DATA, varName));
    }

    private void addDataDependencies() {
        Map<PDGNode, List<PDGNode>> adjacencyList = new HashMap<>();
        for (PDGNode node : nodes) {
            adjacencyList.put(node, new ArrayList<>());
        }
        for (PDGEdge edge : edges) {
            if (edge.type == PDGEdge.EdgeType.CONTROL) {
                adjacencyList.get(edge.source).add(edge.target);
            }
        }

        // For each variable, find all definitions and uses
        Map<String, List<PDGNode>> varToDefNodes = new HashMap<>();
        Map<String, List<PDGNode>> varToUseNodes = new HashMap<>();
        for (Map.Entry<PDGNode, Set<String>> entry : nodeToVarsDefined.entrySet()) {
            PDGNode node = entry.getKey();
            for (String var : entry.getValue()) {
                varToDefNodes.computeIfAbsent(var, k -> new ArrayList<>()).add(node);
            }
        }
        for (Map.Entry<PDGNode, Set<String>> entry : nodeToVarsUsed.entrySet()) {
            PDGNode node = entry.getKey();
            for (String var : entry.getValue()) {
                varToUseNodes.computeIfAbsent(var, k -> new ArrayList<>()).add(node);
            }
        }

        // For each definition, find all reachable uses of the same variable
        for (String var : varToDefNodes.keySet()) {
            List<PDGNode> defNodes = varToDefNodes.get(var);
            List<PDGNode> useNodes = varToUseNodes.getOrDefault(var, new ArrayList<>());

            for (PDGNode defNode : defNodes) {
                Set<PDGNode> reachable = findReachableNodes(defNode, adjacencyList);
                for (PDGNode useNode : useNodes) {
                    if (reachable.contains(useNode)) {
                        addDataEdge(defNode, useNode, var);
                    }
                }
            }
        }
    }

    private Set<PDGNode> findReachableNodes(PDGNode start, Map<PDGNode, List<PDGNode>> adjacencyList) {
        Set<PDGNode> reachable = new HashSet<>();
        Queue<PDGNode> queue = new LinkedList<>();
        queue.add(start);
        reachable.add(start);

        while (!queue.isEmpty()) {
            PDGNode current = queue.poll();
            for (PDGNode neighbor : adjacencyList.get(current)) {
                if (!reachable.contains(neighbor)) {
                    reachable.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return reachable;
    }
}

