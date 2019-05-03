package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;

public class CustomTypeNode extends TypeNode {
    public String typeName = null;

    public CustomTypeNode(){}

    public CustomTypeNode(String name){
        this.typeName = name;
    }

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }

}
