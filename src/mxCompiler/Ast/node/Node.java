package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;
import mxCompiler.Utility.Location;

abstract public class Node{
    public Location location = null;

    abstract public void accept(AstVisitor visitor);
}
