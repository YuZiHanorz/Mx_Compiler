package mxCompiler.Backend;

import mxCompiler.IR.IRVisitor;
import mxCompiler.IR.node.*;
import mxCompiler.IR.operand.*;
import mxCompiler.Utility.RegCollection;

import java.util.HashMap;
import java.util.HashSet;

public class ConstPropagationOpt implements IRVisitor {
    HashMap<Integer, HashSet<VirtualRegister>> valNumRegsMap;
    HashMap<Integer, Integer> valNumConstMap;
    HashMap<VirtualRegister, Integer> regValNumMap;
    HashMap<Integer, Integer> constValNumMap;
    Integer valCnt;

    public ConstPropagationOpt(){
        valNumRegsMap = new HashMap<>();
        valNumConstMap = new HashMap<>();
        regValNumMap = new HashMap<>();
        constValNumMap = new HashMap<>();
    }

    @Override
    public void visit(IRProgram node){
        for (IRFunc f : node.IRFuncList){
            for (BasicBlock bb : f.sonBB)
                bb.accept(this);
        }
    }

    @Override
    public void visit(IRFunc node){}

    @Override
    public void visit(BasicBlock node){
        reset();
        for (IRInst i = node.firstInst; i != null; i = i.nxtInst)
            i.accept(this);
    }
    @Override
    public void visit(IRBranch instNode){
        Integer lt = tryGetConstVal(instNode.lt);
        if (valNumConstMap.containsKey(lt))
            instNode.lt = new IntImm(valNumConstMap.get(lt));

        Integer rt = tryGetConstVal(instNode.rt);
        if (valNumConstMap.containsKey(rt))
            instNode.rt = new IntImm(valNumConstMap.get(rt));

        if (instNode.lt instanceof  IntImm && instNode.rt instanceof IntImm){
            boolean flag;
            switch (instNode.op){
                case E:
                    flag = ((IntImm) instNode.lt).value == ((IntImm) instNode.rt).value;
                    break;
                case NE:
                    flag = ((IntImm) instNode.lt).value != ((IntImm) instNode.rt).value;
                    break;
                case G:
                    flag = ((IntImm) instNode.lt).value > ((IntImm) instNode.rt).value;
                    break;
                case L:
                    flag = ((IntImm) instNode.lt).value < ((IntImm) instNode.rt).value;
                    break;
                case GE:
                    flag = ((IntImm) instNode.lt).value >= ((IntImm) instNode.rt).value;
                    break;
                case LE:
                    flag = ((IntImm) instNode.lt).value <= ((IntImm) instNode.rt).value;
                    break;
                default:
                    throw new Error("hahahahhaha");
            }
            if (flag)
                instNode.replaceInst(new IRJump(instNode.parentBB, instNode.thenBB));
            else instNode.replaceInst(new IRJump(instNode.parentBB, instNode.elseBB));
        }
    }

    @Override
    public void visit(IRJump instNode){}

    @Override
    public void visit(IRReturn instNode){}

    @Override
    public void visit(IRUnary instNode){
        Integer valNum = valCnt++;
        Integer val = tryGetConstVal(instNode.dest);
        if (valNumConstMap.containsKey(val)){ //it is a const
            Integer result = calcUnaryResult(instNode.uop, valNumConstMap.get(val));
            valNumConstMap.put(valNum, result);
            instNode.replaceInst(new IRMove(instNode.parentBB, instNode.dest, new IntImm(result)));
        }
        if (instNode.dest instanceof VirtualRegister)
            propagate((VirtualRegister) instNode.dest, valNum);
    }

    @Override
    public void visit(IRBinary instNode){
        Integer valNum = valCnt++;
        if (instNode.bop == IRBinary.Bop.MUL || instNode.bop == IRBinary.Bop.DIV || instNode.bop == IRBinary.Bop.MOD){
            Integer lt = tryGetConstVal(RegCollection.vrax);
            Integer rt = tryGetConstVal(instNode.rt);
            if (valNumConstMap.containsKey(lt) && valNumConstMap.containsKey(rt)){
                Integer result = calcBinaryResult(instNode.bop, valNumConstMap.get(lt), valNumConstMap.get(rt));
                valNumConstMap.put(valNum, result);
                if (instNode.bop == IRBinary.Bop.DIV || instNode.bop == IRBinary.Bop.MOD)
                    instNode.prevInst.removeInst(); //cdq
                if (instNode.bop == IRBinary.Bop.MOD)
                    instNode.replaceInst(new IRMove(instNode.parentBB, RegCollection.vrdx, new IntImm(result)));
                else instNode.replaceInst(new IRMove(instNode.parentBB, RegCollection.vrax, new IntImm(result)));
            }
            if (instNode.bop == IRBinary.Bop.MOD){
                propagate(RegCollection.vrdx, valNum);
                propagate(RegCollection.vrax, valCnt++);
            }
            else {
                propagate(RegCollection.vrax, valNum);
                propagate(RegCollection.vrdx, valCnt++);
            }
        }
        else {
            Integer lt = tryGetConstVal(instNode.dest);
            Integer rt = tryGetConstVal(instNode.rt);
            if (valNumConstMap.containsKey(lt) && valNumConstMap.containsKey(rt)){
                Integer result = calcBinaryResult(instNode.bop, valNumConstMap.get(lt), valNumConstMap.get(rt));
                valNumConstMap.put(valNum, result);
                instNode.replaceInst(new IRMove(instNode.parentBB, instNode.dest, new IntImm(result)));
            }
            if (instNode.dest instanceof VirtualRegister)
                propagate((VirtualRegister) instNode.dest, valNum);
        }
    }

    @Override
    public void visit(IRMove instNode){
        Integer srcValNum = tryGetConstVal(instNode.src);
        if (valNumConstMap.containsKey(srcValNum))
            instNode.src = new IntImm(valNumConstMap.get(srcValNum));
        if (instNode.dest instanceof VirtualRegister)
            propagate((VirtualRegister) instNode.dest, srcValNum);
    }

    @Override
    public void visit(IRFuncCall instNode){
        propagate(RegCollection.vrax, valCnt++);
    }

    @Override
    public void visit(IRPush instNode){}

    @Override
    public void visit(IRPop instNode){
        if (instNode.dest instanceof VirtualRegister)
            propagate((VirtualRegister) instNode.dest, valCnt++);
    }

    @Override
    public void visit(IRLea instNode){
        if (instNode.destReg instanceof VirtualRegister)
            propagate((VirtualRegister) instNode.destReg, valCnt++);
    }

    @Override
    public void visit(IRCdq instNode){
        propagate(RegCollection.vrdx, valCnt++);
    }

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

    private void reset(){
        valNumRegsMap.clear();
        valNumConstMap.clear();
        regValNumMap.clear();
        constValNumMap.clear();
        valCnt = 0;
    }


    private Integer tryGetConstVal(Operand o){
        if (o instanceof IntImm){
            if (constValNumMap.containsKey(((IntImm) o).value))
                return constValNumMap.get(((IntImm) o).value);
            if (!valNumConstMap.containsKey(valCnt))
                valNumConstMap.put(valCnt, ((IntImm) o).value);
            constValNumMap.put(((IntImm) o).value, valCnt);
            return valCnt++;
        }
        if (o instanceof VirtualRegister){
            if (regValNumMap.containsKey(o)){
                return regValNumMap.get(o);
            }
            if (!valNumRegsMap.containsKey(valCnt))
                valNumRegsMap.put(valCnt, new HashSet<>());
            valNumRegsMap.get(valCnt).add((VirtualRegister) o);
            regValNumMap.put((VirtualRegister) o, valCnt);
            return valCnt++;
        }
        else return valCnt++;
    }

    private void propagate(VirtualRegister vr, Integer valNum){
        //delete old if exist
        if (regValNumMap.containsKey(vr)){
            Integer last = regValNumMap.get(vr);
            if (valNumRegsMap.containsKey(last))
                valNumRegsMap.get(last).remove(vr);
            regValNumMap.remove(vr);
        }
        //add new
        if (!valNumRegsMap.containsKey(valNum))
            valNumRegsMap.put(valNum, new HashSet<>());
        valNumRegsMap.get(valNum).add(vr);
        regValNumMap.put(vr, valNum);
    }

    private Integer calcUnaryResult(IRUnary.Uop uop, Integer val){
        switch (uop){
            case NOT:
                return ~val;
            case NEG:
                return -val;
            case INC:
                return val + 1;
            case DEC:
                return val - 1;
            default:
                throw new Error("aaaaaaaaaaaaa");
        }
    }

    private Integer calcBinaryResult(IRBinary.Bop bop, Integer lt, Integer rt){
        switch (bop){
            case ADD:
                return lt + rt;
            case SUB:
                return lt - rt;
            case MUL:
                return lt * rt;
            case DIV:
                if (rt == 0)
                    return lt;
                else return lt / rt;
            case MOD:
                if (rt == 0)
                    return lt;
                else return lt % rt;
            case SAL:
                return lt << rt;
            case SAR:
                return lt >> rt;
            case AND:
                return lt & rt;
            case OR:
                return lt | rt;
            case XOR:
                return lt ^ rt;
            default:
                throw new Error("aaaaaaaaaaaaa");
        }
    }


}
