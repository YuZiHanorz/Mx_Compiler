package mxCompiler.Backend;

import mxCompiler.IR.IRVisitor;
import mxCompiler.IR.node.*;
import mxCompiler.IR.operand.*;
import mxCompiler.Symbol.VarSymbol;
import mxCompiler.Utility.Configuration;
import mxCompiler.Utility.RegCollection;

import java.util.HashSet;

public class IRRescanner implements IRVisitor {

    @Override
    public void visit(IRProgram node){
        for (IRFunc f : node.IRFuncList)
            f.accept(this);
    }

    @Override
    public void visit(IRFunc node){
        for (BasicBlock bb : node.sonBB)
            bb.accept(this);
    }

    @Override
    public void visit(BasicBlock node){
        for (IRInst i = node.firstInst; i != null; i = i.nxtInst)
            i.accept(this);
    }
    @Override
    public void visit(IRBranch instNode) {
        if (!(instNode.lt instanceof IRConst))
            return;
        if (instNode.rt instanceof IRConst){ //can do cmp immediately
            instNode.prependInst(new IRJump(instNode.parentBB, instNode.CalcDestBlock()));
            instNode.removeInst();
        }
        else { //reverse const to rt
            Operand tmp = instNode.rt;
            instNode.rt = instNode.lt;
            instNode.lt = tmp;
            instNode.op = instNode.reverseOp();
        }
    }

    @Override
    public void visit(IRJump instNode){}

    @Override
    public void visit(IRReturn instNode){}

    @Override
    public void visit(IRUnary instNode){}

    @Override
    public void visit(IRBinary instNode){
        boolean flag = instNode.bop == IRBinary.Bop.MUL ||
                       instNode.bop == IRBinary.Bop.DIV ||
                       instNode.bop == IRBinary.Bop.MOD;
        if (flag && instNode.rt instanceof IRConst){
            VirtualRegister vr = new VirtualRegister("");
            instNode.prependInst(new IRMove(instNode.parentBB, vr, instNode.rt));
            instNode.rt = vr;
        }
    }

    @Override
    public void visit(IRMove instNode){
        //mem to men -> mem to reg to mem
        if (instNode.dest instanceof IRMem && instNode.src instanceof IRMem){
            VirtualRegister vr = new VirtualRegister("");
            instNode.prependInst(new IRMove(instNode.parentBB, vr, instNode.src));
            instNode.src = vr;
        }
    }

    @Override
    public void visit(IRFuncCall instNode){
        IRFunc caller = instNode.parentBB.parentFunc;
        IRFunc callee = instNode.func;
        HashSet<VarSymbol> callerGlovalVars = caller.usedGlobalVars;
        HashSet<VarSymbol> calleeGlobalVars = callee.recursiveUsedGlobalVars;
        for (VarSymbol v : callerGlovalVars){
            if (calleeGlobalVars.contains(v)){ //save
                instNode.prependInst(new IRMove(instNode.parentBB, v.vR.spillOut, v.vR));
                //instNode.prevInst.accept(this);
            }
        }
        while (instNode.argList.size() > 6){ //move args to stackSlot
            instNode.prependInst(new IRPush(instNode.parentBB, instNode.argList.removeLast()));
        }
        for (int i = instNode.argList.size() - 1; i >= 0; --i){ //move rest args to vArgs
            instNode.prependInst(new IRMove(instNode.parentBB, RegCollection.vArgRegList.get(i), instNode.argList.get(i)));
            //instNode.prevInst.accept(this);
        }
        for (VarSymbol v : callerGlovalVars){
            if (calleeGlobalVars.contains(v)) //load
                instNode.appendInst(new IRMove(instNode.parentBB, v.vR, v.vR.spillOut));
        }

    }

    @Override
    public void visit(IRPush instNode){}

    @Override
    public void visit(IRPop instNode){}

    @Override
    public void visit(IRLea instNode){}

    @Override
    public void visit(IRCdq instNode){}

    @Override
    public void visit(IRLeave instNode){}

    @Override
    public void visit(VirtualRegister opNode){}

    @Override
    public void visit(PhysicalRegister opNode){}

    @Override
    public void visit(IRMem opNode){}

    @Override
    public void visit(StackSlot opNode){}

    @Override
    public void visit(IntImm opNode){}

    @Override
    public void visit(StaticData opNode){}

    @Override
    public void visit(FuncPointer opNode){}


}
