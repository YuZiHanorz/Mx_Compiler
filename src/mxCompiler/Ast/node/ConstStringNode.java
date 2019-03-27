package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;
import mxCompiler.Utility.Location;

import org.antlr.v4.runtime.Token;

public class ConstStringNode extends ConstExprNode {
    public String valueStr = null;

    public ConstStringNode(){}
    public ConstStringNode(Token token){
        location = new Location(token);
        valueStr = resolveESC(token.getText());
    }
    private String resolveESC(String string) {
        StringBuilder str = new StringBuilder();
        int length = string.length();
        for(int i = 0; i < length; i++) {
            char ch = string.charAt(i);
            if(ch == '\\') {
                char nxt = string.charAt(i + 1);
                switch(nxt) {
                    case 'n':
                        str.append('\n');
                        break;
                    case 't':
                        str.append('\t');
                        break;
                    case '\\':
                        str.append('\\');
                        break;
                    case '"':
                        str.append('"');
                        break;
                    default:
                        str.append(nxt);
                }
                i++;
            } else {
                str.append(ch);
            }
        }
        return str.toString();
    }
    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }

}
