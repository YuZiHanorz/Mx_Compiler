package mxCompiler.Ast.node;

import mxCompiler.Symbol.CustomTypeSymbol;
import mxCompiler.Ast.AstVisitor;

import java.util.*;

public class ClassDeclNode extends DeclNode {
    public String name = null;
    public FuncDeclNode constructor = null;
    public List<VarDeclNode> memberList;
    public List<FuncDeclNode> methodList;
    public CustomTypeSymbol symbol = null;

    public ClassDeclNode(){
        memberList = new LinkedList<>();
        methodList = new LinkedList<>();
    }

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }


}
