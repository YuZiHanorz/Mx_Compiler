package mxCompiler.Ast.node;

import mxCompiler.Ast.AstVisitor;
import mxCompiler.Utility.Location;

abstract public class Node{
    public Node parent = null;
    public Location location = null;

    abstract public void accept(AstVisitor visitor);
}
