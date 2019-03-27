package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;
import mxCompiler.Utility.Location;

import org.antlr.v4.runtime.Token;

public class ThisExprNode extends ExprNode {
    public ThisExprNode(){}

    public ThisExprNode(Token token){
        if(token != null)
            this.location = new Location(token);
    }


    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }
}
