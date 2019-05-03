package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;

public class BaseTypeNode extends TypeNode{
    public String typeName = null;

    public BaseTypeNode(){}
    public BaseTypeNode(String name){
        this.typeName = name;
    }

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }
}
