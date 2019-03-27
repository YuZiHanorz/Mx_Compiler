package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;

public class UnaryExprNode extends ExprNode {
    public String uop =  null;
    public ExprNode expr = null;

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }

}
