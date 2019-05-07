package mxCompiler.Backend;

import mxCompiler.Frontend.IRPrinter;
import mxCompiler.IR.node.*;
import mxCompiler.IR.operand.*;
import mxCompiler.Utility.RegCollection;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class RegisterAllocator {
    public IRProgram irProgram;
    public LinkedList<PhysicalRegister> almightyRegList;

    private LivelinessAnalyzer livelinessAnalyzer = null;
    private IRPrinter irPrinter = new IRPrinter();
    private IRFunc curFunc = null;

    //interference graph
    private HashMap<VirtualRegister, HashSet<VirtualRegister>> graph;
    private HashMap<VirtualRegister, HashSet<VirtualRegister>> newGraph; //try

    private HashSet<VirtualRegister> mayCanSimplify;
    private HashSet<VirtualRegister> mayNeedSpill;
    private HashSet<VirtualRegister> spilled;
    private LinkedList<VirtualRegister> nodeSelection;

    private static int K = 14; //except for rsp and rbp
    private HashMap<VirtualRegister, PhysicalRegister> colourMap;

    public RegisterAllocator(IRProgram irProgram) {
        this.irProgram = irProgram;
        almightyRegList = new LinkedList<>();
        for (PhysicalRegister pr : RegCollection.regList) {
            if (pr.name.equals("rsp") || pr.name.equals("rbp"))
                continue;
            almightyRegList.add(pr);
        }
    }

    //show interference graph for debug
    private void dump() {
        irPrinter.strBuilder = new StringBuilder();
        irPrinter.sdNameMap = new HashMap<>();
        irPrinter.vrNameMap = new HashMap<>();
        irPrinter.bbNameMap = new HashMap<>();
        irPrinter.ssNameMap = new HashMap<>();
        irPrinter.inLea = false;
        irPrinter.visit(curFunc);
        irPrinter.printTo(System.err);

        System.err.println("LiveOut:");
        for(BasicBlock bb : curFunc.reversePostOrder) {
            System.err.print(irPrinter.bbNameMap.get(bb) + ": ");
            for(VirtualRegister reg : livelinessAnalyzer.liveOutMap.get(bb))
                System.err.print(irPrinter.vrNameMap.get(reg) + " ");
            System.err.print("\n");
        }
        System.err.println("Interference Graph:");
        for(VirtualRegister reg : newGraph.keySet()){
            System.err.print(irPrinter.vrNameMap.get(reg) + ": ");
            for(VirtualRegister adjReg : getNGAdjacent(reg)) {
                System.err.print(irPrinter.vrNameMap.get(adjReg) + " ");
            }
            System.err.print("\n");
        }
        System.err.print("\n\n\n");
    }

    public void build(){
        for (IRFunc f : irProgram.IRFuncList){
            this.curFunc = f;
            processIRFunc();
        }
    }


    /*While True:
            Build Interference Graph
            Simplifylist = Nodes Whose Degree < K
            Spilllist = Nodes Whose Degree >= K
            While Simplifylist Or Spilllist Not Empty:
            If Simplifylist Not Empty: Simplify()
            Else: Potentialspill()
            Assigncolor()
            If Success: Break
            Else:  Realspill()
    */
    private void processIRFunc(){
       while (true){
            livelinessAnalyzer = new LivelinessAnalyzer(curFunc);
            livelinessAnalyzer.buildInferenceGraph(); //rebuild the graph after real spill
            graph = livelinessAnalyzer.interferenceGraph;
            copy();
            //dump();
            reset();
           do {
                if (!mayCanSimplify.isEmpty())
                    doSimplify();
                else if (!mayNeedSpill.isEmpty())
                    potentialSpill();
            } while (!mayCanSimplify.isEmpty() || !mayNeedSpill.isEmpty());
            assignColour();

            //System.err.print("current colour: \n");
            //for (HashMap.Entry<VirtualRegister, PhysicalRegister> entry : colourMap.entrySet())
               //System.err.print("vr<" + entry.getKey().vrName + "> : " + "pr<" + entry.getValue().name + ">\n");
            //System.err.print("current spill: \n");
            //for (VirtualRegister vr : spilled)
                //System.err.print("vr<" + vr.vrName + ">\t");

            if (spilled.isEmpty()) { //success
                doRename();
                break;
            }
            else realSpill();
       }
       curFunc.allocate();
    }

    private void reset(){
        mayCanSimplify = new HashSet<>();
        mayNeedSpill = new HashSet<>();
        spilled = new HashSet<>();
        nodeSelection = new LinkedList<>();
        colourMap = new HashMap<>();
        for (VirtualRegister vr : newGraph.keySet()){
            if (getNGDegree(vr) < K)
                mayCanSimplify.add(vr);
            else mayNeedSpill.add(vr);
        }
    }

    //select those nodes not being limited and try remove
    private void doSimplify(){
        VirtualRegister node = mayCanSimplify.iterator().next();
        LinkedList<VirtualRegister> adjacent = new LinkedList<>(getNGAdjacent(node));
        removeNGNode(node);
        for (VirtualRegister vr : adjacent){
            if (getNGDegree(vr) < K && mayNeedSpill.contains(vr)){ //select it at nxt loop
                mayCanSimplify.add(vr);
                mayNeedSpill.remove(vr);
            }
        }
        mayCanSimplify.remove(node);
        nodeSelection.addFirst(node);
    }

    private void potentialSpill(){
        VirtualRegister spillNode = null;
        int max, cur; //select the node with max degree
        max = -2;
        for (VirtualRegister vr : mayNeedSpill){
            if (vr.allocPhysicalReg != null) //never spill preColoured node unless all is done
                cur = -1;
            else cur = getNGDegree(vr);
            if (cur > max){
                max = cur;
                spillNode = vr;
            }
        }
        removeNGNode(spillNode);
        mayNeedSpill.remove(spillNode);
        nodeSelection.addFirst(spillNode);
    }

    //try colour the selected nodes
    private void assignColour(){
        for (VirtualRegister node : nodeSelection){
            if (node.allocPhysicalReg != null) //handle preAllocated regs at first
                colourMap.put(node, node.allocPhysicalReg);
        }

        for (VirtualRegister node : nodeSelection){
            if (node.allocPhysicalReg != null) continue;
            //find colours that can choose
            HashSet<PhysicalRegister> choices = new HashSet<>(almightyRegList);
            for (VirtualRegister adjacent : getGAdjacent(node)){
                if (colourMap.containsKey(adjacent))
                    choices.remove(colourMap.get(adjacent));
            }
            //fail to colour, need spillOut
            if (choices.isEmpty()) {
                spilled.add(node);
                continue;
            }

            //not use calleeSave at best to reduce memOp
            PhysicalRegister colour = null;
            for (PhysicalRegister pr : RegCollection.callerSaveRegList){
                if (choices.contains(pr)){
                    colour = pr;
                    break;
                }
            }
            if (colour == null)
                colour = choices.iterator().next();
            colourMap.put(node, colour);
        }
    }
    //def a
    //def b
    //def s -> def s1, store s1 to s
    //use b
    //...
    //use s -> load s to s2, use s2
    //use a
    //to avoid origin s be related to not necessary nodes
    private void realSpill(){
        HashMap<VirtualRegister, IRMem> spillMap = new HashMap<>();
        for (VirtualRegister vr : spilled){
            if (vr.spillOut == null)
                spillMap.put(vr, new StackSlot(vr.vrName));
            else spillMap.put(vr, vr.spillOut);
        }
        for (BasicBlock bb : curFunc.sonBB){
            for (IRInst i = bb.firstInst; i != null; i = i.nxtInst){ //find vr that is spilled
                HashMap<IRRegister, IRRegister> renameMap = new HashMap<>();
                LinkedList<VirtualRegister> used = new LinkedList<>(toVreg(i.getUsedRegs()));
                LinkedList<VirtualRegister> def = new LinkedList<>(toVreg(i.getDefRegs()));
                used.retainAll(spilled);
                def.retainAll(spilled);

                for (VirtualRegister vr : used){
                    if (renameMap.containsKey(vr))
                        continue;
                    renameMap.put(vr, new VirtualRegister(""));
                }
                for (VirtualRegister vr : def){
                    if (renameMap.containsKey(vr))
                        continue;
                    renameMap.put(vr, new VirtualRegister(""));
                }
                i.renameUsedReg(renameMap);
                i.renameDefReg(renameMap);
                for (VirtualRegister vr : used)
                    i.prependInst(new IRMove(i.parentBB, renameMap.get(vr), spillMap.get(vr)));
                for (VirtualRegister vr : def){
                    i.appendInst(new IRMove(i.parentBB, spillMap.get(vr), renameMap.get(vr)));
                    i = i.nxtInst;
                }
            }
        }
    }

    //bingo
    private void doRename(){
        HashMap<IRRegister, IRRegister> renameMap = new HashMap<>();
        for (HashMap.Entry<VirtualRegister, PhysicalRegister> entry : colourMap.entrySet())
            renameMap.put(entry.getKey(), entry.getValue());
        for (BasicBlock bb : curFunc.sonBB){
            for (IRInst i = bb.firstInst;  i != null; i = i.nxtInst){
                i.renameUsedReg(renameMap);
                i.renameDefReg(renameMap);
            }
        }
    }

    //maybe I need an assert?
    private LinkedList<VirtualRegister> toVreg(LinkedList<IRRegister> regs){
        LinkedList<VirtualRegister> vRegs = new LinkedList<>();
        for (IRRegister reg : regs)
            vRegs.add((VirtualRegister) reg);
        return vRegs;
    }

    //substitute for g = new HashMap(g1), build a new HashSet for each node
    private void copy(){
        newGraph = new HashMap<>();
        for (VirtualRegister vr : graph.keySet()){
            newGraph.put(vr, new HashSet<>(getGAdjacent(vr)));
        }
    }

    //graph operation
    private Collection<VirtualRegister> getGAdjacent(VirtualRegister node){
        //if (!graph.containsKey(node))
            //return new HashSet<>();
        //else return graph.get(node);
        return graph.getOrDefault(node, new HashSet<>());
    }

    //new graph operation
    private Collection<VirtualRegister> getNGAdjacent(VirtualRegister node){
        //if (!newGraph.containsKey(node))
            //return new HashSet<>();
        //else return newGraph.get(node);
        return newGraph.getOrDefault(node, new HashSet<>());
    }

    private int getNGDegree(VirtualRegister node){
        if (!newGraph.containsKey(node))
            return 0;
        return newGraph.get(node).size();
    }

    private void removeNGNode(VirtualRegister node){
        for (VirtualRegister vr: getNGAdjacent(node))
            newGraph.get(vr).remove(node);
        newGraph.remove(node);
    }




}
