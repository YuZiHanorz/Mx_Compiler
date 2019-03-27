package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;

import java.util.*;

public class ProgramNode extends Node {
    public List<FuncDeclNode> globalFuncList;
    public List<ClassDeclNode> globalClassList;
    public List<VarDeclNode> globalVarList;
    //fucking variables donot accept forward reference
    public List<DeclNode> declList;

    public ProgramNode(){
        this.globalFuncList = new LinkedList<>();
        this.globalClassList = new LinkedList<>();
        this.globalVarList = new LinkedList<>();
        this.declList = new LinkedList<>();
    }

    public void add(FuncDeclNode f){
        globalFuncList.add(f);
        declList.add(f);
    }

    public void add(ClassDeclNode c){
        globalClassList.add(c);
        declList.add(c);
    }

    // one line can declare many variables
    public void addAll(List<VarDeclNode> vL){
        globalVarList.addAll(vL);
        declList.addAll(vL);
    }

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }
}

