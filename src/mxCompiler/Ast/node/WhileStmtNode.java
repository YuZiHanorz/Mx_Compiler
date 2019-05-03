package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;

public class WhileStmtNode extends StmtNode {
    public ExprNode condition = null;
    public StmtNode body = null;

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }

}
