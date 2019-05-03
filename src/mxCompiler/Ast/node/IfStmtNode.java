package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;

public class IfStmtNode extends StmtNode {
    public ExprNode condition = null;
    public StmtNode thenStmt = null;
    public StmtNode elseStmt = null;

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }
}
