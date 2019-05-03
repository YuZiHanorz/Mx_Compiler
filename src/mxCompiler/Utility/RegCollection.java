package mxCompiler.Utility;

import mxCompiler.IR.operand.PhysicalRegister;
import mxCompiler.IR.operand.VirtualRegister;

import java.util.LinkedList;
import java.util.List;

public class RegCollection {
    //x86-64 registers
    public static PhysicalRegister rax; //FuncRet
    public static PhysicalRegister rsp; //StackPeek
    public static PhysicalRegister rdi, rsi, rdx, rcx, r8, r9; //FuncArgs
    public static PhysicalRegister rbx, rbp, r12, r13, r14, r15; //DataStore
    public static PhysicalRegister r10, r11; //DataStore

    //corresponding virtual registers
    public static VirtualRegister vrax; //FuncRet
    public static VirtualRegister vrsp; //StackPeek
    public static VirtualRegister vrdi, vrsi, vrdx, vrcx, vr8, vr9; //FuncArgs
    public static VirtualRegister vrbp, vrbx, vr12, vr13, vr14, vr15; //calleeDataSave
    public static VirtualRegister vr10, vr11; //DataStore

    public static LinkedList<PhysicalRegister> regList;
    public static LinkedList<PhysicalRegister> argRegList;
    public static LinkedList<PhysicalRegister> callerSaveRegList;
    public static LinkedList<PhysicalRegister> calleeSaveRegList;

    public static LinkedList<VirtualRegister> vregList;
    public static LinkedList<VirtualRegister> vArgRegList;
    public static LinkedList<VirtualRegister> vCallerSaveRegList;
    public static LinkedList<VirtualRegister> vCalleeSaveRegList;

    public static void build(){
        regList = new LinkedList<>();
        argRegList = new LinkedList<>();
        callerSaveRegList = new LinkedList<>();
        calleeSaveRegList = new LinkedList<>();

        vregList = new LinkedList<>();
        vArgRegList = new LinkedList<>();
        vCalleeSaveRegList = new LinkedList<>();
        vCallerSaveRegList = new LinkedList<>();

        String[] regName = new String[]{
                "rax", "rsp", "rdi", "rsi", "rdx", "rcx", "r8", "r9",
                "rbp", "rbx", "r12", "r13", "r14", "r15", "r10", "r11"
        };
        for (int i = 0; i < 16; ++i){
            PhysicalRegister reg = new PhysicalRegister(regName[i]);
            VirtualRegister vreg = new VirtualRegister("v" + regName[i], reg);
            regList.add(reg);
            vregList.add(vreg);
            if (regName[i].equals("rsp") || regName[i].equals("rbp"))
                continue;
            if (regName[i].equals("rdi") || regName[i].equals("rsi") || regName[i].equals("rdx")
                || regName[i].equals("rcx") || regName[i].equals("r8") || regName[i].equals("r9")){
                argRegList.add(reg);
                vArgRegList.add(vreg);
            }
            if (regName[i].equals("rbx") || regName[i].equals("r12")
                    || regName[i].equals("r13") || regName[i].equals("r14") || regName[i].equals("r15")){
                calleeSaveRegList.add(reg);
                vCalleeSaveRegList.add(vreg);
            }
            else {
                callerSaveRegList.add(reg);
                vCallerSaveRegList.add(vreg);
            }
        }

        rax = regList.get(0);   vrax = vregList.get(0);
        rsp = regList.get(1);   vrsp = vregList.get(1);
        rdi = regList.get(2);   vrdi = vregList.get(2);
        rsi = regList.get(3);   vrsi = vregList.get(3);
        rdx = regList.get(4);   vrdx = vregList.get(4);
        rcx = regList.get(5);   vrcx = vregList.get(5);
        r8  = regList.get(6);   vr8  = vregList.get(6);
        r9  = regList.get(7);   vr9  = vregList.get(7);
        rbp = regList.get(8);   vrbp = vregList.get(8);
        rbx = regList.get(9);   vrbx = vregList.get(9);
        r12 = regList.get(10);  vr12 = vregList.get(10);
        r13 = regList.get(11);  vr13 = vregList.get(11);
        r14 = regList.get(12);  vr14 = vregList.get(12);
        r15 = regList.get(13);  vr15 = vregList.get(13);
        r10 = regList.get(14);  vr10 = vregList.get(14);
        r11 = regList.get(15);  vr11 = vregList.get(15);



    }
}
