package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;

public class BinaryExprNode extends ExprNode {
    public String bop = null;
    public ExprNode lt = null;
    public ExprNode rt = null;

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }
}
