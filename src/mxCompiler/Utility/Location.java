package mxCompiler.Utility;

import mxCompiler.Ast.node.Node;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public class Location{
    public final int row;
    public final int column;



    public Location(int row, int col){
        this.row = row;
        this.column = col;
    }

    public Location(Token token){
        this.row = token.getLine();
        this.column = token.getCharPositionInLine();
    }

    public Location(ParserRuleContext ctx){
        this(ctx.start);
    }

    public Location(Node node){
        this.row = node.location.row;
        this.column = node.location.column;
    }

    @Override
    public String toString(){
        return "(" + row + "," + column + ")";
    }


}