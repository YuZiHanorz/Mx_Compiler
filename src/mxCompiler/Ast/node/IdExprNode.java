package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;
import mxCompiler.Utility.Location;
import mxCompiler.Symbol.VarSymbol;

import org.antlr.v4.runtime.*;

public class IdExprNode extends ExprNode {
    public String name = null;

    public VarSymbol symbol = null;

    public IdExprNode(){}

    public IdExprNode(Token token){
        this.name = token.getText();
        this.location = new Location(token);
    }

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }

}
