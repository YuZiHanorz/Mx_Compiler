package mxCompiler.Backend;

import mxCompiler.IR.node.*;
import mxCompiler.IR.operand.IntImm;
import mxCompiler.IR.operand.PhysicalRegister;
import mxCompiler.IR.operand.StackSlot;
import mxCompiler.IR.operand.VirtualRegister;
import mxCompiler.Utility.Configuration;
import mxCompiler.Utility.RegCollection;

import javax.print.DocFlavor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class NASMTransformer {
    public IRProgram irProgram;

    //a func has a stack
    public class IRStack{
        public List<StackSlot> argList;
        public List<StackSlot> tmpList;
        public IRStack(){
            argList = new LinkedList<>();
            tmpList = new LinkedList<>();
        }
        public int getSize(){
            int size = (argList.size() + tmpList.size()) *Configuration.regSize;
            return (size +15) /16 * 16;
        }
    }


    public HashMap<IRFunc, IRStack> funcStackMap; //for debug

    public NASMTransformer(IRProgram irProgram){
        this.irProgram = irProgram;
        this.funcStackMap = new HashMap<>();
    }

    public void build(){
        for (IRFunc f : irProgram.IRFuncList)
            processIRFunc(f);
    }

    private void processIRFunc(IRFunc f){
        IRStack stack = new IRStack();
        funcStackMap.put(f, stack);
        LinkedList<VirtualRegister> vArgList = f.paraVirtualRegs;


        for (int i = vArgList.size()-1; i >= 0; --i){
            if (i < 6) break;
            //need to push into stack
            stack.argList.add((StackSlot) vArgList.get(i).spillOut);
        }
        for (BasicBlock bb : f.sonBB){
            for (IRInst i = bb.firstInst; i != null; i = i.nxtInst){
                LinkedList<StackSlot> sss = i.getStackSlots();
                //find tmp vars needing to push into stack
                for (StackSlot ss : sss){
                    if (!stack.argList.contains(ss))
                        stack.tmpList.add(ss);
                }
            }
        }

        //put args to [rbp + 16 + 8 * i]
        for (int i = 0; i < stack.argList.size(); ++i){
            StackSlot ss = stack.argList.get(i);
            assert ss.baseReg == null && ss.literal == null;
            ss.baseReg = RegCollection.rbp;
            ss.literal = new IntImm(16 + 8 * i);
        }

        //put tmp vars to [rbp - 8 -8 * i]
        for (int i = 0; i < stack.tmpList.size(); ++i){
            StackSlot ss = stack.tmpList.get(i);
            assert ss.baseReg == null && ss.literal == null;
            ss.baseReg = RegCollection.rbp;
            ss.literal = new IntImm(-8 - 8 * i);
        }

        //put rsp to stackPeek
        BasicBlock firstBB = f.firstBB;
        IRInst firstInst = firstBB.firstInst;
        firstInst.prependInst(new IRPush(firstBB, RegCollection.rbp));
        firstInst.prependInst(new IRMove(firstBB, RegCollection.rbp, RegCollection.rsp));
        firstInst.prependInst(new IRBinary(firstBB, IRBinary.Bop.SUB, RegCollection.rsp, new IntImm(stack.getSize())));
        HashSet<PhysicalRegister> needSave = new HashSet<>(f.usedPhysicalRegs);
        needSave.retainAll(RegCollection.calleeSaveRegList); //find pReg that need save
        firstInst = firstInst.prevInst;
        for (PhysicalRegister pReg : needSave)
            firstInst.appendInst(new IRPush(firstBB, pReg)); //push them into stack

        IRReturn r = (IRReturn) f.lastBB.lastInst;
        for (PhysicalRegister pReg : needSave)
            r.prependInst(new IRPop(r.parentBB, pReg));
        r.prependInst(new IRLeave(r.parentBB));
    }


}
