package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;

public class ArrayTypeNode extends TypeNode {
    public TypeNode elementType = null;
    public int dim = -1;

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }
}
