package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;
import mxCompiler.Symbol.FuncSymbol;

import java.util.*;

public class FuncDeclNode extends DeclNode {
    public String name = null;
    public boolean isConstructor;
    public List<VarDeclNode> parameterList;
    public List<StmtNode> block;
    public TypeNode retType = null;
    public FuncSymbol symbol = null;

    public FuncDeclNode(){
        parameterList = new LinkedList<>();
        block = new LinkedList<>();
    }


    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }


}
