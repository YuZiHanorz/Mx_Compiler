package mxCompiler.IR;

import mxCompiler.IR.node.*;
import mxCompiler.IR.operand.*;

public interface IRVisitor {
    void visit(IRProgram node);
    void visit(IRFunc node);
    void visit(BasicBlock node);

    void visit(IRBranch instNode);
    void visit(IRJump instNode);
    void visit(IRReturn instNode);

    void visit(IRUnary instNode);
    void visit(IRBinary instNode);
    void visit(IRMove instNode);
    void visit(IRFuncCall instNode);
    void visit(IRPush instNode);
    void visit(IRPop instNode);
    void visit(IRLea instNode);
    void visit(IRCdq instNode);
    void visit(IRLeave instNode);

    void visit(VirtualRegister opNode);
    void visit(PhysicalRegister opNode);
    void visit(IRMem opNode);
    void visit(StackSlot opNode);
    void visit(IntImm opNode);
    void visit(StaticData opNode);
    void visit(FuncPointer opNode);
}
