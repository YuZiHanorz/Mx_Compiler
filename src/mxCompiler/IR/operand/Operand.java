package mxCompiler.IR.operand;

import mxCompiler.IR.IRVisitor;

abstract public class Operand {
    abstract public void accept(IRVisitor visitor);
}
