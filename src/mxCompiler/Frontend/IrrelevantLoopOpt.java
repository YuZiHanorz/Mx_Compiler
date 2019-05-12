package mxCompiler.Frontend;

import mxCompiler.Ast.AstVisitor;
import mxCompiler.Ast.node.*;
import mxCompiler.Symbol.VarSymbol;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class IrrelevantLoopOpt implements AstVisitor {
    public HashMap<ExprNode, HashSet<VarSymbol>> exprRelevantMap;

    public IrrelevantLoopOpt(){
        exprRelevantMap = new HashMap<>();
    }

    @Override
    public void visit(ProgramNode node){
        for (FuncDeclNode f : node.globalFuncList)
            f.accept(this);
        for (ClassDeclNode c : node.globalClassList)
            c.accept(this);
    }

    @Override
    public void visit(DeclNode node){}

    @Override
    public void visit(FuncDeclNode node){
        for (StmtNode s : node.block) 
            s.accept(this);
    }

    @Override
    public void visit(ClassDeclNode node){
        if (node.constructor != null)
            node.constructor.accept(this);
        for (FuncDeclNode f : node.methodList)
            f.accept(this);
    }

    @Override
    public void visit(VarDeclNode node){}

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
        for (StmtNode s: node.stmtList) {
            if (s instanceof ForStmtNode || s instanceof WhileStmtNode)
                s.parent = node;
            s.accept(this);
        }
    }

    @Override
    public void visit(ExprStmtNode node){
        node.expr.accept(this);
    }

    @Override
    public void visit(IfStmtNode node){
        node.condition.accept(this);
        node.thenStmt.accept(this);
        if (node.elseStmt != null)
            node.elseStmt.accept(this);
    }
    @Override
    public void visit(WhileStmtNode node){
        node.condition.accept(this);
        if (node.body != null)
            node.body.accept(this);

        IfStmtNode ifStmt = null;
        if (node.body instanceof BlockStmtNode) {
            if (((BlockStmtNode) node.body).stmtList.size() == 1) {
                if (((BlockStmtNode) node.body).stmtList.get(0) instanceof IfStmtNode)
                ifStmt = (IfStmtNode) ((BlockStmtNode) node.body).stmtList.get(0);
            }
        }
        if (ifStmt == null )
            return;
        if (ifStmt.elseStmt != null)
            return;
        HashSet<VarSymbol> condRelevant = exprRelevantMap.getOrDefault(node.condition, new HashSet<>());
        HashSet<VarSymbol> set = new HashSet<>(exprRelevantMap.getOrDefault(ifStmt.condition, new HashSet<>()));
        set.retainAll(condRelevant);
        if (set.size() != 0)
            return;
        ((BlockStmtNode) node.body).stmtList.remove(ifStmt);
        ((BlockStmtNode) node.body).stmtList.add(ifStmt.thenStmt);
        ifStmt.thenStmt = node;
        if (node.parent instanceof BlockStmtNode){
            ((BlockStmtNode) node.parent).stmtList.remove(node);
            ((BlockStmtNode) node.parent).stmtList.add(ifStmt);
        }
    }

    @Override
    public void visit(ForStmtNode node){
        if (node.init != null)
            node.init.accept(this);
        if (node.condition != null)
            node.condition.accept(this);
        if (node.body != null)
            node.body.accept(this);

        if (node.body == null)
            return;
        IfStmtNode ifStmt = null;
        if (node.body instanceof BlockStmtNode) {
            if (((BlockStmtNode) node.body).stmtList.size() == 1) {
                if (((BlockStmtNode) node.body).stmtList.get(0) instanceof IfStmtNode)
                    ifStmt = (IfStmtNode) ((BlockStmtNode) node.body).stmtList.get(0);
            }
        }

        if (ifStmt == null)
            return;
        if (ifStmt.elseStmt != null)
            return;
        HashSet<VarSymbol> initRelevant = exprRelevantMap.getOrDefault(node.init, new HashSet<>());
        HashSet<VarSymbol> condRelevant = exprRelevantMap.getOrDefault(node.condition, new HashSet<>());
        HashSet<VarSymbol> set1 = new HashSet<>(condRelevant);
        HashSet<VarSymbol> set2 = new HashSet<>(exprRelevantMap.getOrDefault(ifStmt.condition, new HashSet<>()));
        set1.addAll(initRelevant);
        set2.retainAll(set1);
        if (set2.size() != 0)
            return;
        ((BlockStmtNode) node.body).stmtList.remove(ifStmt);
        ((BlockStmtNode) node.body).stmtList.add(ifStmt.thenStmt);
        ifStmt.thenStmt = node;
        if (node.parent instanceof BlockStmtNode){
            ((BlockStmtNode) node.parent).stmtList.remove(node);
            ((BlockStmtNode) node.parent).stmtList.add(ifStmt);
        }
    }

    @Override
    public void visit(ContinueStmtNode node){}

    @Override
    public void visit(BreakStmtNode node){}

    @Override
    public void visit(ReturnStmtNode node){
        if (node.retExpr != null)
            node.retExpr.accept(this);
    }

    @Override
    public void visit(VarStmtNode node){}

    @Override
    public void visit(EmptyStmtNode node){}

    @Override
    public void visit(ExprNode node){}

    @Override
    public void visit(MemberExprNode node){
        if (node.method != null)
            node.method.accept(this);
    }

    @Override
    public void visit(SubscriptExprNode node){
        node.subscript.accept(this);
        exprRelevantMap.put(node, new HashSet<>(exprRelevantMap.getOrDefault(node.subscript, new HashSet<>())));
    }

    @Override
    public void visit(FuncCallExprNode node){
        HashSet<VarSymbol> set = new HashSet<>();
        for (ExprNode e : node.argList){
            e.accept(this);
            set.addAll(exprRelevantMap.getOrDefault(e, new HashSet<>()));
        }
        exprRelevantMap.put(node, set);
    }

    @Override
    public void visit(NewExprNode node){}

    @Override
    public void visit(UnaryExprNode node){
        node.expr.accept(this);
        exprRelevantMap.put(node, new HashSet<>(exprRelevantMap.getOrDefault(node.expr, new HashSet<>())));
    }

    @Override
    public void visit(BinaryExprNode node){
        node.lt.accept(this);
        node.rt.accept(this);
        HashSet<VarSymbol> set = new HashSet<>(exprRelevantMap.getOrDefault(node.lt, new HashSet<>()));
        set.addAll(exprRelevantMap.getOrDefault(node.rt, new HashSet<>()));
        exprRelevantMap.put(node, set);
    }

    @Override
    public void visit(AssignExprNode node){
        if (node.lt != null)
            node.lt.accept(this);
        if (node.rt != null)
            node.rt.accept(this);
        HashSet<VarSymbol> set = new HashSet<>(exprRelevantMap.getOrDefault(node.lt, new HashSet<>()));
        set.addAll(exprRelevantMap.getOrDefault(node.rt, new HashSet<>()));
        exprRelevantMap.put(node, set);
    }

    @Override
    public void visit(IdExprNode node){
        HashSet<VarSymbol> set = new HashSet<>();
        set.add(node.symbol);
        exprRelevantMap.put(node, set);
    }

    @Override
    public void visit(ConstExprNode node){}

    @Override
    public void visit(ConstIntNode node){}

    @Override
    public void visit(ConstBoolNode node){}

    @Override
    public void visit(ConstNullNode node){}

    @Override
    public void visit(ConstStringNode node){}
}
