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
    private final Map<String, List<PDGNode>> varToDefNodes;
    private final Map<Statement, Set<String>> stmtToVarsUsed;
    private final Map<Statement, Set<String>> stmtToVarsDefined;

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
        this.varToDefNodes = new HashMap<>();
        this.stmtToVarsUsed = new HashMap<>();
        this.stmtToVarsDefined = new HashMap<>();
    }

    public PDG build() {
        PDGNode entryNode = createNode(null, "ENTRY: " + method.getSignature().asString());
        PDGNode exitNode = createNode(null, "EXIT: " + method.getSignature().asString());

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
            addControlEdge(node, ctx.cont);
            analyzeVariableUsage(stmt, node);
            return true;
        } else if (stmt instanceof IfStmt) {
            var if_stmt = (IfStmt) stmt;
            Statement then_stmt = if_stmt.getThenStmt();
            var new_ctx = new CFGContext(ctx.cont, ctx.curLoopNode, ctx.contForCurLoopNode, ctx.methodExit);
            boolean ret = processStatement(then_stmt, new_ctx);

            if (if_stmt.getElseStmt().isPresent()) {
                Statement else_stmt = if_stmt.getElseStmt().get();
                ret |= processStatement(else_stmt, new_ctx);
            } else {
                addControlEdge(node, ctx.cont);
            }
            return ret;
        } else if (stmt instanceof WhileStmt) {
            var while_stmt = (WhileStmt) stmt;
            Statement body = while_stmt.getBody();
            var new_ctx = new CFGContext(node, node, ctx.cont, ctx.methodExit);
            addControlEdge(node, ctx.cont);
            processStatement(body, new_ctx);
            return true;
        // } else if (stmt instanceof ForStmt) {
        //     processForStmt((ForStmt) stmt, ctx);
        } else if (stmt instanceof ReturnStmt) {
            addControlEdge(node, ctx.methodExit);
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
            // TODO: Generic statement
            // PDGNode node = createNode(stmt, stmt.toString().trim());
            // addControlEdge(controlParent, node, "");
            // analyzeVariableUsage(stmt, node);
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

    private void analyzeVariableUsage(Statement stmt, PDGNode node) {
        Set<String> usedVars = extractUsedVariables(stmt);
        Set<String> definedVars = extractDefinedVariables(stmt);

        stmtToVarsUsed.put(stmt, usedVars);
        stmtToVarsDefined.put(stmt, definedVars);

        for (String var : definedVars) {
            varToDefNodes.computeIfAbsent(var, k -> new ArrayList<>()).add(node);
        }
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

        // Unary expressions (++, --)
        node.findAll(UnaryExpr.class).forEach(unary -> {
            if (unary.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT ||
                unary.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT ||
                unary.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT ||
                unary.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT) {
                if (unary.getExpression() instanceof NameExpr) {
                    vars.add(((NameExpr) unary.getExpression()).getNameAsString());
                }
            }
        });

        return vars;
    }

    private void addControlEdge(PDGNode source, PDGNode target) {
        if (source == null || target == null) return;
        edges.add(new PDGEdge(source, target, PDGEdge.EdgeType.CONTROL, ""));
    }

    private void addDataEdge(PDGNode source, PDGNode target, String varName) {
        edges.add(new PDGEdge(source, target, PDGEdge.EdgeType.DATA, varName));
    }

    private void addDataDependencies() {
        for (Map.Entry<Statement, Set<String>> entry : stmtToVarsUsed.entrySet()) {
            Statement stmt = entry.getKey();
            Set<String> usedVars = entry.getValue();
            PDGNode targetNode = stmtToNode(stmt);

            if (targetNode == null) continue;

            for (String var : usedVars) {
                List<PDGNode> defNodes = varToDefNodes.get(var);
                if (defNodes != null) {
                    for (PDGNode defNode : defNodes) {
                        addDataEdge(defNode, targetNode, var);
                    }
                }
            }
        }
    }
}

