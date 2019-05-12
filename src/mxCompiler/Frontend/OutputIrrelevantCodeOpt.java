package mxCompiler.Frontend;

import mxCompiler.Ast.AstVisitor;
import mxCompiler.Ast.node.*;
import mxCompiler.Symbol.VarSymbol;
import mxCompiler.Type.TypeArray;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class OutputIrrelevantCodeOpt implements AstVisitor {
    private HashSet<VarSymbol> relevantVars;
    private HashMap<Node, HashSet<VarSymbol>> defVarMap;
    private HashMap<Node, HashSet<VarSymbol>> usedVarMap;
    private boolean isInit, isCalcDependence;
    private boolean inAssign = false;
    private VarSymbol cannotRemove;

    public OutputIrrelevantCodeOpt(){
        this.relevantVars = new HashSet<>();
        this.defVarMap = new HashMap<>();
        this.usedVarMap = new HashMap<>();
        this.cannotRemove = new VarSymbol(null, null, null, false, false);
    }

    @Override
    public void visit(ProgramNode node){
        relevantVars.add(cannotRemove);

        isInit = true;
        for (FuncDeclNode f : node.globalFuncList)
            f.accept(this);
        isInit = false;

        isCalcDependence = true;
        int flag = -1;
        while (flag != relevantVars.size()) {
            flag = relevantVars.size();
            for (FuncDeclNode f : node.globalFuncList)
                f.accept(this);
        }
        isCalcDependence = false;

        for (FuncDeclNode f : node.globalFuncList)
            f.accept(this);
    }

    @Override
    public void visit(DeclNode node){}

    @Override
    public void visit(FuncDeclNode node){
        if (isInit){
            for (StmtNode s : node.block)
                s.accept(this);
        }
        else if (isCalcDependence){
            for (StmtNode s : node.block)
                s.accept(this);
        }
        else tryRemove(node.block);
    }

    @Override
    public void visit(ClassDeclNode node){}

    @Override
    public void visit(VarDeclNode node){
        if (isInit){
            initUsedDef(node);
            if (node.init != null) {
                node.init.accept(this);
                addDependence(node, node.init);
            }
            defVarMap.get(node).add(node.symbol);
        }
        else if (isCalcDependence){
            if (node.init != null) {
                propagate(node, node.init);
                node.init.accept(this);
            }
        }
    }

    @Override
    public void visit(VarListNode node){}

    @Override
    public void visit(TypeNode node){}

    @Override
    public void visit(ArrayTypeNode node){}

    @Override
    public void visit(CustomTypeNode node){}

    @Override
    public void visit(BaseTypeNode node){}

    @Override
    public void visit(StmtNode node){}

    @Override
    public void visit(BlockStmtNode node){
        if (isInit){
            initUsedDef(node);
            for (StmtNode s: node.stmtList) {
                s.accept(this);
                addDependence(node, s);
            }
        }
        else if (isCalcDependence) {
            for (StmtNode s : node.stmtList)
                s.accept(this);
        }
        else tryRemove(node.stmtList);
    }

    @Override
    public void visit(ExprStmtNode node){
        if (isInit){
            initUsedDef(node);
            node.expr.accept(this);
            addDependence(node, node.expr);
        }
        else if (isCalcDependence){
            propagate(node, node.expr);
            node.expr.accept(this);
        }
    }

    @Override
    public void visit(IfStmtNode node){
        if (isInit){
            initUsedDef(node);
            node.condition.accept(this);
            addDependence(node, node.condition);
            node.thenStmt.accept(this);
            addDependence(node, node.thenStmt);
            if (node.elseStmt != null){
                node.elseStmt.accept(this);
                addDependence(node, node.elseStmt);
            }
        }
        else if (isCalcDependence){
            propagate(node, node.condition);
            node.condition.accept(this);
            node.thenStmt.accept(this);
            if (node.elseStmt != null)
                node.elseStmt.accept(this);
        }
        else {
            node.thenStmt.accept(this);
            if (node.elseStmt != null)
                node.elseStmt.accept(this);
        }
    }
    @Override
    public void visit(WhileStmtNode node){
        if (isInit){
            initUsedDef(node);
            node.condition.accept(this);
            addDependence(node, node.condition);
            node.body.accept(this);
            addDependence(node, node.body);
        }
        else if (isCalcDependence){
            propagate(node, node.condition);
            node.body.accept(this);
        }
        else node.body.accept(this);
    }

    @Override
    public void visit(ForStmtNode node){
        if (isInit){
            initUsedDef(node);
            if (node.init != null){
                node.init.accept(this);
                addDependence(node, node.init);
            }
            if (node.condition != null){
                node.condition.accept(this);
                addDependence(node, node.condition);
            }
            if (node.step != null){
                node.step.accept(this);
                addDependence(node, node.step);
            }
            node.body.accept(this);
            addDependence(node, node.body);
        }
        else if (isCalcDependence){
            propagate(node, node.init, node.condition, node.step);
            node.body.accept(this);
        }
        else {
            if (!canRemove(node.body))
                node.body.accept(this);
        }
    }

    @Override
    public void visit(ContinueStmtNode node){
        if (isInit){
            initUsedDef(node);
            defVarMap.get(node).add(cannotRemove);
        }
    }

    @Override
    public void visit(BreakStmtNode node){
        if (isInit){
            initUsedDef(node);
            defVarMap.get(node).add(cannotRemove);
        }
    }

    @Override
    public void visit(ReturnStmtNode node){
        if (isInit) {
            initUsedDef(node);
            if (node.retExpr != null) {
                node.retExpr.accept(this);
                addDependence(node, node.retExpr);
                relevantVars.addAll(usedVarMap.get(node.retExpr));
            }
            defVarMap.get(node).add(cannotRemove);
        }
        else if (isCalcDependence){
            if (node.retExpr != null)
                node.retExpr.accept(this);
        }
    }

    @Override
    public void visit(VarStmtNode node){
        if (isInit){
            initUsedDef(node);
            node.varDecl.accept(this);
            addDependence(node, node.varDecl);
        }
        else if (isCalcDependence){
            propagate(node, node.varDecl);
            node.varDecl.accept(this);
        }
    }

    @Override
    public void visit(EmptyStmtNode node){
        if (isInit)
            initUsedDef(node);
    }

    @Override
    public void visit(ExprNode node){}

    @Override
    public void visit(MemberExprNode node){
        if (isInit){
            initUsedDef(node);
            node.obj.accept(this);
            addDependence(node, node.obj);
            if (node.method != null){
                node.method.accept(this);
                addDependence(node, node.method);
            }
            else {
                node.member.accept(this);
                addDependence(node, node.member);
            }
        }
        else if (isCalcDependence){
            propagate(node, node.obj, node.method, node.member);
            node.obj.accept(this);
            if (node.method != null)
                node.method.accept(this);
            else node.member.accept(this);
        }
    }

    @Override
    public void visit(SubscriptExprNode node){
        if (isInit) {
            initUsedDef(node);
            node.array.accept(this);
            addDependence(node, node.array);
            boolean lastInAssign = inAssign;
            inAssign = false;
            node.subscript.accept(this);
            addDependence(node, node.subscript);
            inAssign = lastInAssign;
        }
    }

    @Override
    public void visit(FuncCallExprNode node){
        if (isInit){
            initUsedDef(node);
            for (ExprNode e : node.argList){
                e.accept(this);
                addDependence(node, e);
            }
            if (node.symbol != null){
                defVarMap.get(node).add(cannotRemove);
                relevantVars.addAll(usedVarMap.get(node));
            }
        }
        else if (isCalcDependence){
            propagate(node, node.argList);
            for (ExprNode e : node.argList)
                e.accept(this);
        }
    }

    @Override
    public void visit(NewExprNode node){
        if (isInit){
            initUsedDef(node);
            for (ExprNode e : node.defineSizeList) {
                e.accept(this);
                addDependence(node, e);
            }
        }
        else if (isCalcDependence){
            propagate(node, node.defineSizeList);
            for (ExprNode e : node.defineSizeList)
                e.accept(this);
        }
    }

    @Override
    public void visit(UnaryExprNode node){
        if (isInit){
            initUsedDef(node);
            node.expr.accept(this);
            addDependence(node, node.expr);
            if (node.uop.contains("++") || node.uop.contains("--"))
                defVarMap.get(node).addAll(usedVarMap.get(node.expr));
        }
        else if (isCalcDependence){
            propagate(node, node.expr);
            node.expr.accept(this);
        }
    }

    @Override
    public void visit(BinaryExprNode node){
        if (isInit){
            initUsedDef(node);
            node.lt.accept(this);
            addDependence(node, node.lt);
            node.rt.accept(this);
            addDependence(node, node.rt);
        }
        else if (isCalcDependence){
            propagate(node, node.lt, node.rt);
            node.lt.accept(this);
            node.rt.accept(this);
        }

    }

    @Override
    public void visit(AssignExprNode node){
        if (isInit){
            initUsedDef(node);
            boolean lastInAssign = inAssign;
            inAssign = true;
            node.lt.accept(this);
            addDependence(node, node.lt);
            defVarMap.get(node).addAll(defVarMap.get(node.lt));
            inAssign = lastInAssign;

            node.rt.accept(this);
            addDependence(node, node.rt);
            if (node.rt.calcType instanceof TypeArray)
                defVarMap.get(node).addAll(usedVarMap.get(node.rt));
        }
        else if (isCalcDependence){
            propagate(node, node.lt, node.rt);
            node.lt.accept(this);
            node.rt.accept(this);
        }
    }

    @Override
    public void visit(IdExprNode node){
        if (isInit){
            initUsedDef(node);
            if (node.symbol.isGlobal)
                defVarMap.get(node).add(cannotRemove);
            else if (inAssign){
                defVarMap.get(node).add(node.symbol);
                usedVarMap.get(node).add(node.symbol);
            }
            else usedVarMap.get(node).add(node.symbol);
        }
    }

    @Override
    public void visit(ConstExprNode node){}

    @Override
    public void visit(ConstIntNode node){
        if (isInit)
            initUsedDef(node);
    }

    @Override
    public void visit(ConstBoolNode node){
        if (isInit)
            initUsedDef(node);
    }

    @Override
    public void visit(ConstNullNode node){
        if (isInit)
            initUsedDef(node);
    }

    @Override
    public void visit(ConstStringNode node){
        if (isInit)
            initUsedDef(node);
    }

    private void initUsedDef(Node node){
        defVarMap.put(node, new HashSet<>());
        usedVarMap.put(node, new HashSet<>());
    }

    private void addDependence(Node fa, Node son){
        defVarMap.get(fa).addAll(defVarMap.get(son));
        usedVarMap.get(fa).addAll(usedVarMap.get(son));
    }

    private boolean canRemove(Node node){
        HashSet<VarSymbol> def = new HashSet<>(defVarMap.get(node));
        def.retainAll(relevantVars);
        return def.isEmpty();
    }

    private void tryRemove(List<StmtNode> stmtList){
        HashSet<StmtNode> removed = new HashSet<>();
        for (StmtNode s : stmtList){
            if (canRemove(s))
                removed.add(s);
            else s.accept(this);
        }
        stmtList.removeAll(removed);
    }

    private void propagate(Node fa, Node...sons){
        propagate(fa, Arrays.asList(sons));
    }
    private void propagate(Node fa, List<? extends Node> sons){
        if (canRemove(fa))
            return;
        for (Node son : sons){
            if (son != null)
                relevantVars.addAll(usedVarMap.get(son));
        }
    }


}


