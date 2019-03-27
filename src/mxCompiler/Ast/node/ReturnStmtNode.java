package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;

public class ReturnStmtNode extends StmtNode {
    public ExprNode retExpr = null;

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }
}
