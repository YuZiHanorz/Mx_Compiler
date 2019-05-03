package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;

public class ForStmtNode extends StmtNode {
    public ExprNode init = null;
    public ExprNode condition = null;
    public ExprNode step = null;
    public StmtNode body = null;

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }

}
