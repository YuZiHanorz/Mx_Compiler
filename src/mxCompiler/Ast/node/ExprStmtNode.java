package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;

public class ExprStmtNode extends StmtNode {
    public ExprNode expr = null;

    public ExprStmtNode(){}

    public ExprStmtNode(ExprNode expr){
        this.expr = expr;
    }

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }

}
