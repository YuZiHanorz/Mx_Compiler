package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;

import java.util.*;

public class NewExprNode extends ExprNode {
    public TypeNode type = null; //base or custom(no array)
    public List<ExprNode> defineSizeList;
    public int notDefine = -1;

    public NewExprNode(){
        defineSizeList = new LinkedList<>();
    }

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }

}
