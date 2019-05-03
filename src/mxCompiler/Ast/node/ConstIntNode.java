package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;
import mxCompiler.Utility.Location;

import org.antlr.v4.runtime.Token;

public class ConstIntNode extends ConstExprNode {
    public String valueStr = null;

    public ConstIntNode(){}
    public ConstIntNode(Token token){
        location = new Location(token);
        valueStr = token.getText();
    }

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }

}
