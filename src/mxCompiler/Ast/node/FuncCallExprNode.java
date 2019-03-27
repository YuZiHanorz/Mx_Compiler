package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;
import mxCompiler.Symbol.FuncSymbol;

import java.util.*;

public class FuncCallExprNode extends ExprNode {
    public String funcName = null;
    public List<ExprNode> argList;

    public FuncSymbol symbol;

    public FuncCallExprNode(){
        argList = new LinkedList<>();
    }

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }
}
