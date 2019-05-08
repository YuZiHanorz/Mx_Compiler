package mxCompiler.Ast.node;

import mxCompiler.Type.TypeType;

abstract public class ExprNode extends Node{
    public TypeType calcType = null;
    public boolean isLvalue;
    public boolean toConst = false;
}
