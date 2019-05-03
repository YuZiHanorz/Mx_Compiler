package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;

public class MemberExprNode extends ExprNode {
    public ExprNode obj = null;
    public IdExprNode member = null;
    public FuncCallExprNode method = null;

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }

}
