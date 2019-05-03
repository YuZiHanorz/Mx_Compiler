package mxCompiler.Backend;

import mxCompiler.IR.IRVisitor;
import mxCompiler.IR.node.*;
import mxCompiler.IR.operand.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;

public class NASMPrinter implements IRVisitor {
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

    public NASMPrinter(){
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
        try{
            BufferedReader br = new BufferedReader(new FileReader("lib/lib.asm"));
            String lineRead;
            while((lineRead = br.readLine()) != null)
                append(lineRead + "\n");
        }
        catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }
        append(";======================\n");
        append("\tsection\t.text\n");

        for (IRFunc f : node.IRFuncList)
            f.accept(this);

        append("\tsection\t.data\n");
        for (StaticData sd : node.staticDataList){
            append(getSDName(sd));
            append(":\n");

            if (sd.stringTypeInit == null){
                append("\tdb ");
                for (int i = 0; i < sd.size; ++i){
                    if (i != 0)
                        append(", ");
                    append("00H");
                }
                append("\n");
            }
            else {
                append("\tdq " + sd.stringTypeInit.length() + "\n");
                append("\tdb ");
                for (int i = 0; i < sd.stringTypeInit.length(); ++i) {
                    Formatter ft = new Formatter();
                    ft.format("%02XH, ", (int) sd.stringTypeInit.charAt(i));
                    append(ft.toString());
                }
                append("00H\n");
            }
        }

        for (StaticData sd : node.staticDataList){
            append(getSDName(sd) + "<bytes: " + sd.size + " >");
            if (sd.stringTypeInit != null)
                append("<init: " + sd.stringTypeInit + " >\n");
        }
    }

    @Override
    public void visit(IRFunc node){
        append(getFuncName((node)));
        append(":\n");
        ArrayList<BasicBlock> rpo = new ArrayList<>(node.reversePostOrder);
        for (int i = 0; i < rpo.size(); ++i){
            BasicBlock bb = rpo.get(i);
            if (i == rpo.size() -1)
                nxtBB = null;
            else nxtBB = rpo.get(i+1);
            bb.accept(this);
        }
    }

    @Override
    public void visit(BasicBlock node){
        append("\t" + getBBName(node));
        append(":\n");
        for (IRInst i = node.firstInst; i != null; i = i.nxtInst)
            i.accept(this);
    }
    @Override
    public void visit(IRBranch instNode) {
        String cop = null;
        switch (instNode.op) {
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
        append("\tcmp ");
        instNode.lt.accept(this);
        append(", ");
        instNode.rt.accept(this);
        append("\n");
        append("\t" + cop + " " + getBBName(instNode.thenBB) + "\n");
        if (instNode.elseBB != nxtBB) {
            append("\tjmp " + getBBName(instNode.elseBB)+ "\n");
        }
    }

    @Override
    public void visit(IRJump instNode){
        if (instNode.destBB == nxtBB)
            return;
        append("\tjmp " + getBBName(instNode.destBB) + "\n");

    }

    @Override
    public void visit(IRReturn instNode){
        append("\tret \n");
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
        append("\t" + uop + " ");
        instNode.dest.accept(this);
        append("\n");

    }

    @Override
    public void visit(IRBinary instNode){
        if (instNode.bop == IRBinary.Bop.MUL){
            append("\timul ");
            instNode.rt.accept(this);
            append("\n");
            return;
        }
        if (instNode.bop == IRBinary.Bop.DIV || instNode.bop == IRBinary.Bop.MOD){
            append("\tidiv ");
            instNode.rt.accept(this);
            append("\n");
            return;
        }
        if (instNode.bop == IRBinary.Bop.SAL){
            append("\tsal ");
            instNode.dest.accept(this);
            append(", cl\n");
            return;
        }
        if (instNode.bop == IRBinary.Bop.SAR){
            append("\tsar ");
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
        append("\t" + bop + " ");
        instNode.dest.accept(this);
        append(", ");
        instNode.rt.accept(this);
        append("\n");
    }

    @Override
    public void visit(IRMove instNode){
        if (instNode.src == instNode.dest)
            return;
        append("\tmov ");
        instNode.dest.accept(this);
        append(", ");
        instNode.src.accept(this);
        append("\n");
    }

    @Override
    public void visit(IRFuncCall instNode){
        append("\tcall " + getFuncName(instNode.func) + "\n");
    }

    @Override
    public void visit(IRPush instNode){
        append("\tpush ");
        instNode.src.accept(this);
        append("\n");
    }

    @Override
    public void visit(IRPop instNode){
        append("\tpop");
        instNode.dest.accept(this);
        append("\n");
    }

    @Override
    public void visit(IRLea instNode){
        inLea = true;
        append("\tlea ");
        instNode.destReg.accept(this);
        append(", ");
        instNode.srcMem.accept(this);
        append("\n");
        inLea = false;
    }

    @Override
    public void visit(IRCdq instNode){
        append("\tcdq \n");
    }

    @Override
    public void visit(IRLeave instNode){
        append("\tleave \n");
    }

    @Override
    public void visit(VirtualRegister opNode){
        assert false;
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
            assert false;
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
        append(getFuncName(opNode.func));
    }

    private void append(String str){
        strBuilder.append(str);
    }

    private String getBBName(BasicBlock bb){
        if (bbNameMap.containsKey(bb))
            return bbNameMap.get(bb);
        bbNameMap.put(bb, "bb_"+ (bbCnt++));
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
        sdNameMap.put(sd, "sd_" + (sdCnt++));
        return sdNameMap.get(sd);
    }

    private String getSSName(StackSlot ss){
        if (ssNameMap.containsKey(ss))
            return ssNameMap.get(ss);
        ssNameMap.put(ss, "ss[" + (ssCnt++)+ "]");
        return ssNameMap.get(ss);
    }

    private String getFuncName(IRFunc f){
        if (f.funcName.equals("malloc"))
            return f.funcName; //externalMalloc
        if (f.isCustom)
            return "_" + f.funcName;
        return "lib_" + f.funcName; //lib
    }

}
