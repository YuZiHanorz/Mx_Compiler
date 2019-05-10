package mxCompiler.Backend;

import mxCompiler.IR.node.BasicBlock;
import mxCompiler.IR.node.IRFunc;
import mxCompiler.IR.node.IRFuncCall;
import mxCompiler.IR.node.IRInst;
import mxCompiler.IR.operand.IRRegister;
import mxCompiler.IR.operand.VirtualRegister;
import org.antlr.v4.runtime.InterpreterRuleContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class LivelinessAnalyzer {
    public IRFunc func = null;
    public boolean inOpt = false;
    public HashMap<VirtualRegister, HashSet<VirtualRegister>> interferenceGraph;
    public HashMap<BasicBlock, HashSet<VirtualRegister>> liveOutMap;
    public HashMap<BasicBlock, HashSet<VirtualRegister>> ueVarMap;
    public HashMap<BasicBlock, HashSet<VirtualRegister>> varKillMap;

    public LivelinessAnalyzer(IRFunc f){
        this.func = f;
        interferenceGraph = new HashMap<>();
        liveOutMap = new HashMap<>();
        ueVarMap = new HashMap<>();
        varKillMap = new HashMap<>();
        for (BasicBlock bb : f.sonBB){
            liveOutMap.put(bb, new HashSet<>());
            ueVarMap.put(bb, new HashSet<>());
            varKillMap.put(bb, new HashSet<>());
        }
    }

    public void buildInferenceGraph(){
        buildLiveOut();
        interferenceGraph.clear();

        //init interference graph with no edge
        for (BasicBlock bb : func.sonBB){
            for (IRInst i = bb.firstInst; i != null; i = i.nxtInst){
                LinkedList<VirtualRegister> all = new LinkedList<>(toVreg(i.getDefRegs()));
                all.addAll(toVreg(i.getUsedRegs()));
                for (VirtualRegister vr : all){
                    if (interferenceGraph.containsKey(vr))
                        continue;
                    interferenceGraph.put(vr, new HashSet<>());
                }
            }
        }

        //build interference graph by scanning from end of the block using liveOut
        for (BasicBlock bb : func.sonBB){
            HashSet<VirtualRegister> liveNow = new HashSet<>(liveOutMap.get(bb));
            for (IRInst i = bb.lastInst; i != null; i = i.prevInst){
                for (VirtualRegister vrdef : toVreg(i.getDefRegs())){
                    for (VirtualRegister vrlive : liveNow){
                        if (vrdef == vrlive)
                            continue;
                        interferenceGraph.get(vrdef).add(vrlive);
                        interferenceGraph.get(vrlive).add(vrdef);
                    }
                }
                liveNow.removeAll(toVreg(i.getDefRegs()));
                liveNow.addAll(toVreg(i.getUsedRegs()));
            }
        }

    }

    private LinkedList<VirtualRegister> toVreg(LinkedList<IRRegister> regs){
        LinkedList<VirtualRegister> vRegs = new LinkedList<>();
        for (IRRegister reg : regs)
            vRegs.add((VirtualRegister) reg);
        return vRegs;
    }

    //build ueVar and varKill for a block
    private void initBB(BasicBlock bb){
        HashSet<VirtualRegister> ueVar = new HashSet<>();
        HashSet<VirtualRegister> varKill = new HashSet<>();
        for (IRInst i = bb.firstInst; i != null; i = i.nxtInst){
            LinkedList<IRRegister> used;
            if (i instanceof IRFuncCall && inOpt)
                used = ((IRFuncCall) i).getCallUsedRegs();
            else used = i.getUsedRegs();
            for (VirtualRegister vr : toVreg(used)){
                if (varKill.contains(vr))
                    continue;
                ueVar.add(vr);
            }
            varKill.addAll(toVreg(i.getDefRegs()));
        }
        ueVarMap.put(bb, ueVar);
        varKillMap.put(bb, varKill);
    }

    //build liveOut
    public void buildLiveOut(){
        for (BasicBlock bb : func.sonBB)
            initBB(bb);
        boolean changed = true;
        while (changed){
            changed = false;
            for (BasicBlock bb : func.reversePreOrder){
                int last = liveOutMap.get(bb).size();
                for (BasicBlock s : bb.succ){
                    HashSet<VirtualRegister> tmp = new HashSet<>();
                    tmp.addAll(liveOutMap.get(s));
                    tmp.removeAll(varKillMap.get(s));
                    tmp.addAll(ueVarMap.get(s));
                    liveOutMap.get(bb).addAll(tmp);
                }
                changed = changed || liveOutMap.get(bb).size() != last;
            }
        }
    }

}
