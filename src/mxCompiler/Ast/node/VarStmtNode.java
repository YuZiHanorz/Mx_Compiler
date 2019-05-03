package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;

public class VarStmtNode extends StmtNode {
    public VarDeclNode varDecl = null;

    @Override
    public void accept(AstVisitor visitor){
        visitor.visit(this);
    }

}
