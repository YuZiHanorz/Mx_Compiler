package mxCompiler.Frontend;

import mxCompiler.IR.IRVisitor;
import mxCompiler.IR.node.*;
import mxCompiler.IR.operand.*;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

public class IRPrinter implements IRVisitor {
    public StringBuilder strBuilder;

    public HashMap<StaticData, String> sdNameMap;
    public HashMap<VirtualRegister, String> vrNameMap;
    public HashMap<BasicBlock, String> bbNameMap;
    public HashMap<StackSlot, String> ssNameMap;
    public int varCnt = 0;
    public int sdCnt = 0;
    public int bbCnt = 0;
    public int ssCnt = 0;

    public BasicBlock nxtBB = null;

    public boolean inLea = false;
    //public boolean showBBName = false;
    //public boolean showNasm = false;
    //public boolean showHeader = false;

    public IRPrinter(){
        this.strBuilder = new StringBuilder();
        this.sdNameMap = new HashMap<>();
        this.vrNameMap = new HashMap<>();
        this.bbNameMap = new HashMap<>();
        this.ssNameMap = new HashMap<>();
    }

    public void printTo(PrintStream out){
        out.print(strBuilder.toString());
    }

    @Override
    public void visit(IRProgram node){
        for (IRFunc f : node.IRFuncList)
            f.accept(this);
        for (StaticData sd : node.staticDataList){
            append(getSDName(sd) + "<bytes: " + sd.size + " >");
            if (sd.stringTypeInit != null)
                append("<init: " + sd.stringTypeInit + " >\n");
        }
    }

    @Override
    public void visit(IRFunc node){
        append("defFunc: " + node.funcName + " (");
        boolean begin = true;
        for (VirtualRegister vr : node.paraVirtualRegs){
            if (begin)
                begin  = false;
            else append(", ");
            vr.accept(this);
        }
        append(") {\n");
        ArrayList<BasicBlock> rpo = new ArrayList<>(node.reversePostOrder);
        for (int i = 0; i < rpo.size(); ++i){
            BasicBlock bb = rpo.get(i);
            if (i == rpo.size() -1)
                nxtBB = null;
            else nxtBB = rpo.get(i+1);
            bb.accept(this);
        }
        append("}\n");
    }

    @Override
    public void visit(BasicBlock node){
        append("\t" + getBBName(node) + ":\n");
        for (IRInst i = node.firstInst; i != null; i = i.nxtInst)
            i.accept(this);
    }
    @Override
    public void visit(IRBranch instNode){
        String cop = null;
        switch (instNode.op){
            case E:
                cop = "je";
                break;
            case NE:
                cop = "jne";
                break;
            case G:
                cop = "jg";
                break;
            case L:
                cop = "jl";
                break;
            case GE:
                cop = "jge";
                break;
            case LE:
                cop = "jle";
                break;
        }
        append("\t\t" + cop + " ");
        instNode.lt.accept(this);
        append(", ");
        instNode.rt.accept(this);
        append(", " + getBBName(instNode.thenBB) + ", " + getBBName(instNode.elseBB) + "\n");
    }

    @Override
    public void visit(IRJump instNode){
        if (instNode.destBB == nxtBB)
            return;
        append("\t\tjmp " + getBBName(instNode.destBB) + "\n");

    }

    @Override
    public void visit(IRReturn instNode){
        append("\t\tret \n");
    }

    @Override
    public void visit(IRUnary instNode){
        String uop = null;
        switch (instNode.uop){
            case NOT:
                uop = "not";
                break;
            case NEG:
                uop = "neg";
                break;
            case INC:
                uop = "inc";
                break;
            case DEC:
                uop = "dec";
                break;
        }
        append("\t\t" + uop + " ");
        instNode.dest.accept(this);
        append("\n");

    }

    @Override
    public void visit(IRBinary instNode){
        if (instNode.bop == IRBinary.Bop.MUL){
            append("\t\timul ");
            instNode.rt.accept(this);
            append("\n");
            return;
        }
        if (instNode.bop == IRBinary.Bop.DIV || instNode.bop == IRBinary.Bop.MOD){
            append("\t\tidiv ");
            instNode.rt.accept(this);
            append("\n");
            return;
        }
        if (instNode.bop == IRBinary.Bop.SAL){
            append("\t\t" + "sal" + " ");
            instNode.dest.accept(this);
            append(", cl\n");
            return;
        }
        if (instNode.bop == IRBinary.Bop.SAR){
            append("\t\t" + "sar" + " ");
            instNode.dest.accept(this);
            append(", cl\n");
            return;
        }

        String bop = null;
        switch (instNode.bop){
            case ADD:
                bop = "add";
                break;
            case SUB:
                bop = "sub";
                break;
            case AND:
                bop = "and";
                break;
            case OR:
                bop = "or";
                break;
            case XOR:
                bop = "xor";
                break;
        }
        append("\t\t" + bop + " ");
        instNode.dest.accept(this);
        append(", ");
        instNode.rt.accept(this);
        append("\n");
    }

    @Override
    public void visit(IRMove instNode){
        if (instNode.src == instNode.dest)
            return;
        append("\t\tmov ");
        instNode.dest.accept(this);
        append(", ");
        instNode.src.accept(this);
        append("\n");
    }

    @Override
    public void visit(IRFuncCall instNode){
        append("\t\tcall ");
        if (instNode.dest != null){
            instNode.dest.accept(this);
            append(" = ");
        }
        append(instNode.func.funcName + "(");
        if (instNode.argList != null){
            boolean begin = true;
            for (Operand o : instNode.argList){
                if (begin)
                    begin  = false;
                else append(", ");
                o.accept(this);
            }
        }
        append(")\n");
    }

    @Override
    public void visit(IRPush instNode){
        append("\t\tpush ");
        instNode.src.accept(this);
        append("\n");
    }

    @Override
    public void visit(IRPop instNode){
        append("\t\tpop");
        instNode.dest.accept(this);
        append("\n");
    }

    @Override
    public void visit(IRLea instNode){
        inLea = true;
        append("\t\tlea ");
        instNode.destReg.accept(this);
        append(", ");
        instNode.srcMem.accept(this);
        append("\n");
        inLea = false;
    }

    @Override
    public void visit(IRCdq instNode){
        append("\t\tcdq \n");
    }

    @Override
    public void visit(IRLeave instNode){
        append("\t\tleave \n");
    }

    @Override
    public void visit(VirtualRegister opNode){
        if (opNode.allocPhysicalReg != null){
            opNode.allocPhysicalReg.accept(this);
            vrNameMap.put(opNode, opNode.allocPhysicalReg.name);
        }
        else append(getVRName(opNode));
    }

    @Override
    public void visit(PhysicalRegister opNode){
        append(opNode.name);
    }

    @Override
    public void visit(IRMem opNode){
        boolean flag =false;
        if (!inLea)
            append("qword");
        append("[");

        if (opNode.baseReg != null){
            opNode.baseReg.accept(this);
            flag = true;
        }

        if (opNode.indexReg != null){
            if (flag)
                append(" + ");
            opNode.indexReg.accept(this);
            if (opNode.scale != 1)
                append(" * " + opNode.scale);
            flag = true;
        }

        if (opNode.literal != null){
            if (opNode.literal instanceof StaticData){
                if (flag)
                    append(" + ");
                opNode.literal.accept(this);
            }
            else if (opNode.literal instanceof IntImm){
                int val = ((IntImm) opNode.literal).value;
                if (flag){
                    if (val > 0)
                        append(" + " + val);
                    if (val < 0)
                        append(" - " + (-val));
                }
                else append(String.valueOf(val));
            }
        }

        append("]");
    }

    @Override
    public void visit(StackSlot opNode){
        if (opNode.baseReg == null && opNode.indexReg == null && opNode.literal == null)
            append(getSSName(opNode));
        else visit((IRMem)opNode);
    }

    @Override
    public void visit(IntImm opNode){
        append(String.valueOf(opNode.value));
    }

    @Override
    public void visit(StaticData opNode){
        append(getSDName(opNode));
    }

    @Override
    public void visit(FuncPointer opNode){

    }

    private void append(String str){
        strBuilder.append(str);
    }

    private String getBBName(BasicBlock bb){
        if (bbNameMap.containsKey(bb))
            return bbNameMap.get(bb);
        bbNameMap.put(bb, "bb_"+ (bbCnt++) + "<" + bb.BBName + ">");
        return bbNameMap.get(bb);
    }

    private String getVRName(VirtualRegister vr){
        if (vrNameMap.containsKey(vr))
            return vrNameMap.get(vr);
        vrNameMap.put(vr, "vr_"+ (varCnt++) + "<" + vr.vrName + ">");
        return vrNameMap.get(vr);
    }

    private String getSDName(StaticData sd){
        if (sdNameMap.containsKey(sd))
            return sdNameMap.get(sd);
        sdNameMap.put(sd, "sd_" + (sdCnt++)+ "<" + sd.name + ">");
        return sdNameMap.get(sd);
    }

    private String getSSName(StackSlot ss){
        if (ssNameMap.containsKey(ss))
            return ssNameMap.get(ss);
        ssNameMap.put(ss, "ss_" + (ssCnt++)+ "<" + ss.name + ">");
        return ssNameMap.get(ss);
    }

}
