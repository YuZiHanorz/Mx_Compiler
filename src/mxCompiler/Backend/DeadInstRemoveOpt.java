package mxCompiler.Backend;

import mxCompiler.Frontend.IRBuilder;
import mxCompiler.IR.node.*;
import mxCompiler.IR.operand.IRRegister;
import mxCompiler.IR.operand.VirtualRegister;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class DeadInstRemoveOpt {
    public IRProgram irProgram;
    public LivelinessAnalyzer livelinessAnalyzer = null;
    public HashMap<BasicBlock, HashSet<VirtualRegister>> liveOutMap;

    public DeadInstRemoveOpt(IRProgram irProgram){
        this.irProgram = irProgram;
    }

    public void build(){
        for (IRFunc f : irProgram.IRFuncList)
            processIRFunc(f);
    }

    private void processIRFunc(IRFunc f){
        livelinessAnalyzer = new LivelinessAnalyzer(f);
        livelinessAnalyzer.inOpt = true;
        livelinessAnalyzer.buildLiveOut();
        liveOutMap = livelinessAnalyzer.liveOutMap;
        for (BasicBlock bb : f.sonBB){
            HashSet<VirtualRegister> liveOut = new HashSet<>(liveOutMap.get(bb));
            for (IRInst i = bb.lastInst; i != null; i = i.prevInst){
                LinkedList<VirtualRegister> def = toVreg(i.getDefRegs());
                LinkedList<VirtualRegister> used;
                if (i instanceof IRFuncCall)
                    used = toVreg(((IRFuncCall) i).getCallUsedRegs());
                else used = toVreg(i.getUsedRegs());
                boolean dead = true;
                if (def.isEmpty())
                    dead = false;
                for (VirtualRegister vr : def){
                    if (liveOut.contains(vr) || vr.spillOut != null) {
                        dead = false;
                        break;
                    }
                }
                //def some var but not used later
                if (dead && canRemove(i)) {
                    i.removeInst();
                    continue;
                }
                liveOut.removeAll(def);
                liveOut.addAll(used);
            }
        }

    }

    private boolean canRemove(IRInst i){
        if (i instanceof IRPop || i instanceof IRPush || i instanceof IRCdq || i instanceof IRReturn
        || i instanceof IRJump || i instanceof IRBranch || i instanceof IRFuncCall)
            return false;
        else return true;
    }

    private LinkedList<VirtualRegister> toVreg(LinkedList<IRRegister> regs){
        LinkedList<VirtualRegister> vRegs = new LinkedList<>();
        for (IRRegister reg : regs)
            vRegs.add((VirtualRegister) reg);
        return vRegs;
    }
}
