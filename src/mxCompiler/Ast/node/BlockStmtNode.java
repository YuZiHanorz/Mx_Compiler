package mxCompiler.Ast.node;

import java.util.*;
import mxCompiler.Ast.AstVisitor;

public class BlockStmtNode extends StmtNode{
    public List<StmtNode> stmtList;

    public BlockStmtNode(){
        stmtList = new LinkedList<>();
    }

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }

}
