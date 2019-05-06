package mxCompiler.Backend;

import mxCompiler.IR.node.*;
import mxCompiler.IR.operand.*;
import mxCompiler.Utility.RegCollection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

//All virtual registers store in memory
//Fetch into physical register when needed
//store back after operation
//with many bug, but I do not wanna debug
public class SimpleRegisterAllocator {
    public IRProgram irProgram;
    public LinkedList<PhysicalRegister> almightyRegList;

    public SimpleRegisterAllocator(IRProgram irProgram){
        this.irProgram = irProgram;
        this.almightyRegList = new LinkedList<>();
        this.almightyRegList.add(RegCollection.rbx);
        this.almightyRegList.add(RegCollection.r10);
        this.almightyRegList.add(RegCollection.r11);
        this.almightyRegList.add(RegCollection.r12);
        this.almightyRegList.add(RegCollection.r13);
        this.almightyRegList.add(RegCollection.r14);
        this.almightyRegList.add(RegCollection.r15);
    }

    public void build() {
        for (IRFunc f : irProgram.IRFuncList)
            processIRFunc(f);
    }

    private void processIRFunc(IRFunc f){
        for (BasicBlock bb : f.sonBB){
            for (IRInst i = bb.firstInst; i != null; i = i.nxtInst){
                if (i instanceof IRFuncCall)
                    continue; //already move args to pRegs
                HashSet<IRRegister> allRegs = new HashSet<>();
                allRegs.addAll(i.getUsedRegs());
                allRegs.addAll(i.getDefRegs());
                HashMap<IRRegister, IRRegister> renameMap = new HashMap<>();
                LinkedList<IRRegister> used = i.getUsedRegs();
                LinkedList<IRRegister> def = i.getDefRegs();

                for (IRRegister reg : allRegs){
                    if (reg instanceof VirtualRegister){
                        if (((VirtualRegister) reg).allocPhysicalReg != null) continue;
                        if (((VirtualRegister) reg).spillOut != null) continue;
                        ((VirtualRegister) reg).spillOut = new StackSlot(((VirtualRegister) reg).vrName);
                    }
                    else assert false;
                }

                //handle copy(move)
                if (i instanceof IRMove){
                    PhysicalRegister destPReg = getPReg(((IRMove) i).dest);
                    PhysicalRegister srcPReg = getPReg(((IRMove) i).src);
                    if (destPReg != null && srcPReg != null){
                        ((IRMove) i).dest = destPReg;
                        ((IRMove) i).src = srcPReg;
                        continue;
                    }
                    if (destPReg != null){
                        ((IRMove) i).dest = destPReg;
                        if (((IRMove) i).src instanceof IRConst){
                            continue;
                        }
                        if (((IRMove) i).src instanceof VirtualRegister)
                            ((IRMove) i).src = ((VirtualRegister) ((IRMove) i).src).spillOut;
                        else assert false;
                        continue;
                    }
                    if (srcPReg != null){
                        ((IRMove) i).src = srcPReg;
                        if (((IRMove) i).dest instanceof VirtualRegister)
                            ((IRMove) i).dest = ((VirtualRegister) ((IRMove) i).dest).spillOut;
                        else assert false;
                        continue;
                    }
                }

                int cnt = 0;
                for (IRRegister reg : allRegs){
                    if (renameMap.containsKey(reg)) continue;
                    PhysicalRegister pReg = ((VirtualRegister)reg).allocPhysicalReg;
                    if (pReg == null)
                        renameMap.put(reg, almightyRegList.get(cnt++));
                    else renameMap.put(reg, pReg);
                }
                i.renameDefReg(renameMap);
                i.renameUsedReg(renameMap);

                for (IRRegister reg : used){
                    if (reg instanceof VirtualRegister) {
                        if (((VirtualRegister) reg).allocPhysicalReg == null)
                            i.prependInst(new IRMove(bb, renameMap.get(reg), ((VirtualRegister) reg).spillOut));
                    }
                    else assert false;
                }

                for (IRRegister reg : def) {
                    if (reg instanceof VirtualRegister) {
                        if (((VirtualRegister) reg).allocPhysicalReg == null) {
                            i.appendInst(new IRMove(bb, ((VirtualRegister) reg).spillOut, renameMap.get(reg)));
                            i = i.nxtInst;
                        }
                    }
                    else assert false;
                }
            }
        }

    }

    private PhysicalRegister getPReg(Operand vr){
        if (vr instanceof VirtualRegister)
            return ((VirtualRegister) vr).allocPhysicalReg;
        else return null;
    }


}
