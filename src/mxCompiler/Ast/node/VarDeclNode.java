package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;
import mxCompiler.Symbol.VarSymbol;

public class VarDeclNode extends DeclNode {
    public TypeNode type = null;
    public String name = null;
    public ExprNode init = null;

    public VarSymbol symbol = null;

    public VarDeclNode(){}

    public VarDeclNode(TypeNode type, String name, ExprNode init){
        this.type = type;
        this.name = name;
        this.init = init;
    }

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }


}
