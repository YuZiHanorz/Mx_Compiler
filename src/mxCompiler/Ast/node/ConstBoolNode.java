package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;
import mxCompiler.Utility.Location;
import org.antlr.v4.runtime.Token;

public class ConstBoolNode extends ConstExprNode {
    public String valueStr = null;

    public ConstBoolNode(){}
    public ConstBoolNode(Token token){
        location = new Location(token);
        valueStr = token.getText();
    }

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }
}
