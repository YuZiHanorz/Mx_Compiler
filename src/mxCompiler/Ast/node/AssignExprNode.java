package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;

public class AssignExprNode extends ExprNode {
    public ExprNode lt = null;
    public ExprNode rt = null;

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }

}
