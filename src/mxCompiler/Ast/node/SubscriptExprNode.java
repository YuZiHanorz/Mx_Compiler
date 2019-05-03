package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;

public class SubscriptExprNode extends ExprNode {
    public ExprNode array = null;
    public ExprNode subscript = null;

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }
}
