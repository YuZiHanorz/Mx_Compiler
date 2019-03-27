package mxCompiler.Ast;

import mxCompiler.Ast.node.*;

public interface AstVisitor{
    //program
    void visit(ProgramNode node);

    //declaration
    void visit(DeclNode node);
    void visit(FuncDeclNode node);
    void visit(ClassDeclNode node);
    void visit(VarDeclNode node);
    void visit(VarListNode node);

    //type
    void visit(TypeNode node);
    void visit(ArrayTypeNode node);
    void visit(CustomTypeNode node);
    void visit(BaseTypeNode node);

    //statement
    void visit(StmtNode node);
    void visit(BlockStmtNode node);
    void visit(ExprStmtNode node);
    void visit(IfStmtNode node);
    void visit(WhileStmtNode node);
    void visit(ForStmtNode node);
    void visit(ContinueStmtNode node);
    void visit(BreakStmtNode node);
    void visit(ReturnStmtNode node);
    void visit(VarStmtNode node);
    void visit(EmptyStmtNode node);

    //Expr
    void visit(ExprNode node);
    void visit(MemberExprNode node);
    void visit(SubscriptExprNode node);
    void visit(FuncCallExprNode node);
    void visit(NewExprNode node);
    void visit(UnaryExprNode node);
    void visit(BinaryExprNode node);
    void visit(AssignExprNode node);
    void visit(IdExprNode node);
    void visit(ConstExprNode node);
    void visit(ConstIntNode node);
    void visit(ConstBoolNode node);
    void visit(ConstNullNode node);
    void visit(ConstStringNode node);
}