package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;

import java.util.LinkedList;
import java.util.List;

public class VarListNode extends Node{
    public List<VarDeclNode> varList;

    public VarListNode(){
        varList = new LinkedList<>();
    }

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }

}
