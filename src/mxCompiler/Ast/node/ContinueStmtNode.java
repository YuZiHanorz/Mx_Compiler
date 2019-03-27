package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;

public class ContinueStmtNode extends StmtNode {
    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }
}
