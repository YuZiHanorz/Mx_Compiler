package mxCompiler.Frontend;

import mxCompiler.Ast.AstVisitor;
import mxCompiler.Ast.node.*;
import mxCompiler.IR.node.*;
import mxCompiler.IR.operand.*;
import mxCompiler.Symbol.CustomTypeSymbol;
import mxCompiler.Symbol.FuncSymbol;
import mxCompiler.Symbol.GlobalSymbolTable;
import mxCompiler.Symbol.VarSymbol;
import mxCompiler.Type.TypeArray;
import mxCompiler.Type.TypeBase;
import mxCompiler.Type.TypeCustom;
import mxCompiler.Type.TypeType;
import mxCompiler.Utility.Configuration;
import mxCompiler.Utility.RegCollection;

import java.lang.reflect.Type;
import java.util.*;

public class IRBuilder implements AstVisitor{
    public IRProgram irProgram;

    public IRFunc curFunc = null;
    public BasicBlock curBB = null;
    public CustomTypeSymbol curClass = null;
    public VirtualRegister curThisPointer = null;

    public GlobalSymbolTable globalSymbolTable;

    public BasicBlock continueDestBB;
    public BasicBlock breakDestBB;

    public HashMap<String, FuncDeclNode> ASTFuncDeclMap;
    public HashMap<String, IRFunc> IRFuncMap;

    public HashMap<ExprNode, BasicBlock> trueDestBBMap;
    public HashMap<ExprNode, BasicBlock> falseDestBBMap;

    public HashMap<ExprNode, IRStorePos> exprDestMap;
    public HashMap<ExprNode, Operand> exprSrcMap;

    //for inlineOpt
    public boolean isInClass = false, isInArgs = false, isInline = false;
    public LinkedList<HashMap<VarSymbol, VirtualRegister>> inlineVarRegMaps;
    public LinkedList<BasicBlock> inlineFuncLeaveBBs;

    public HashMap<FuncSymbol ,Integer> funcOpCntMap;

    //some LibFunc
    private static IRFunc libHasVal, libSetVal, libGetVal;
    private static IRFunc libStringCmp, libStringConcat;
    private static IRFunc libInit;
    private static IRFunc exMalloc;

    public IRBuilder(GlobalSymbolTable globalSymbolTable){
        this.irProgram = new IRProgram();

        this.globalSymbolTable = globalSymbolTable;

        this.ASTFuncDeclMap = new HashMap<>();
        this.IRFuncMap = new HashMap<>();

        this.trueDestBBMap = new HashMap<>();
        this.falseDestBBMap = new HashMap<>();

        this.exprDestMap = new HashMap<>();
        this.exprSrcMap = new HashMap<>();

        this.inlineVarRegMaps = new LinkedList<>();
        this.inlineFuncLeaveBBs = new LinkedList<>();
        this.funcOpCntMap = new HashMap<>();

        //init libraryFunc
        IRFunc funcPrint = new IRFunc("print", false, true);
        IRFunc funcPrintln = new IRFunc("println", false, true);
        IRFunc funcGetString = new IRFunc("getString", false, true);
        IRFunc funcGetInt = new IRFunc("getInt", false, false);
        IRFunc funcToString = new IRFunc("toString", false, false);

        IRFuncMap.put("print", funcPrint);
        IRFuncMap.put("println", funcPrintln);
        IRFuncMap.put("getString", funcGetString);
        IRFuncMap.put("getInt", funcGetInt);
        IRFuncMap.put("toString", funcToString);

        IRFunc funcLength = new IRFunc("stringLength", false, false);
        IRFunc funcSubstring = new IRFunc("stringSubstring", false, false);
        IRFunc funcParseInt = new IRFunc("stringParseInt", false, false);
        IRFunc funcOrd = new IRFunc("stringOrd", false, false);

        IRFuncMap.put("string.length", funcLength);
        IRFuncMap.put("string.substring", funcSubstring);
        IRFuncMap.put("string.parseInt", funcParseInt);
        IRFuncMap.put("string.ord", funcOrd);

        libHasVal = new IRFunc("hasVal", false, false);
        libGetVal = new IRFunc("getVal", false, false);
        libSetVal = new IRFunc("setVal", false, false);

        libStringCmp = new IRFunc("stringCmp", false, false);
        libStringConcat = new IRFunc("stringConcat", false, false);

        libInit = new IRFunc("init", false, false);

        exMalloc = new IRFunc("malloc", false, false);
    }

    @Override
    public void visit(ProgramNode node){
        for (VarDeclNode v : node.globalVarList){
            StaticData sd = new StaticData(v.name, Configuration.regSize);
            irProgram.staticDataList.add(sd);

            VirtualRegister vr = new VirtualRegister(v.name);
            vr.spillOut = new IRMem();
            vr.spillOut.literal = sd;
            v.symbol.vR = vr;
        }

        LinkedList<FuncDeclNode> allAstFunc = new LinkedList<>(node.globalFuncList);
        for (ClassDeclNode c : node.globalClassList){
            allAstFunc.addAll(c.methodList);
            if (c.constructor != null)
                allAstFunc.add(c.constructor);
        }

        for (FuncDeclNode f : allAstFunc){
            ASTFuncDeclMap.put(f.symbol.name, f);
            if (!IRFuncMap.containsKey(f.symbol.name))
                IRFuncMap.put(f.symbol.name, new IRFunc(f.symbol.name, true, isVoid(f.symbol.returnType)));
        }

        for (FuncDeclNode f : node.globalFuncList)
            f.accept(this);
        for (ClassDeclNode c : node.globalClassList)
            c.accept(this);

        for (IRFunc f : IRFuncMap.values()){
            if (f.isCustom)
                f.build();
        }

        irProgram.IRFuncList.add(libInit);
        libInit.usedGlobalVars = new HashSet<>(globalSymbolTable.globalinitVarSet);
        curFunc = libInit;
        BasicBlock firstBB = new BasicBlock(libInit, "enter_libInit");
        curBB = curFunc.firstBB = firstBB;
        for (VarDeclNode v : node.globalVarList){
            if (v.init != null)
                exprDestBuild(v.init, v.symbol.vR);
        }
        curBB.pushTailInst(new IRFuncCall(curBB, RegCollection.vrax, IRFuncMap.get("main")));
        curBB.pushTailInst(new IRReturn(curBB));
        curFunc.lastBB = curBB;
        curFunc.build();
    }

    @Override
    public void visit(DeclNode node){}

    @Override
    public void visit(FuncDeclNode node){
        curFunc = IRFuncMap.get(node.symbol.name);
        curBB = curFunc.firstBB = new BasicBlock(curFunc, "enter_" + node.symbol.name);

        //arrange args virtual registers
        isInArgs = true;
        if (isInClass){
            VirtualRegister vr = new VirtualRegister("this");
            curThisPointer = vr;
            curFunc.paraVirtualRegs.add(vr);
        }
        for (VarDeclNode argv : node.parameterList)
            argv.accept(this);
        isInArgs = false;

        //move args to their virtual registers
        for (int i = 0; i < curFunc.paraVirtualRegs.size(); ++i){
            if (i < 6)
                curBB.pushTailInst(new IRMove(curBB, curFunc.paraVirtualRegs.get(i),
                        RegCollection.vArgRegList.get(i)));
            else
                curBB.pushTailInst(new IRMove(curBB, curFunc.paraVirtualRegs.get(i),
                        curFunc.paraVirtualRegs.get(i).spillOut));
        }

        //move globalVars to vr
        for (VarSymbol v : node.symbol.globalVarSet)
            curBB.pushTailInst(new IRMove(curBB, v.vR, v.vR.spillOut));

        //recursively handle block
        for (StmtNode s : node.block)
            s.accept(this);

        //handle return
        LinkedList<IRReturn> ret = new LinkedList<>();
        if (!(curBB.lastInst instanceof IRReturn)){
            if (isVoid(node.symbol.returnType))
                curBB.pushTailInst(new IRReturn(curBB));
            else {
                //main
                curBB.pushTailInst(new IRMove(curBB, RegCollection.vrax, new IntImm(0)));
                curBB.pushTailInst(new IRReturn(curBB));
            }
        }
        for (BasicBlock b : curFunc.sonBB){
            for (IRInst i = b.firstInst; i != null; i = i.nxtInst){
                if (i instanceof IRReturn)
                    ret.add((IRReturn)i);
            }
        }

        //handle leave block(gather return and save globalVars)
        curFunc.lastBB = new BasicBlock(curFunc, "leave_"+ node.symbol.name);
        for (IRReturn r : ret){
            r.prependInst(new IRJump(r.parentBB, curFunc.lastBB));
            r.removeInst();
        }
        curFunc.lastBB.pushTailInst(new IRReturn(curFunc.lastBB));
        IRInst retInst = curFunc.lastBB.lastInst;
        for (VarSymbol v : node.symbol.globalVarSet)
            retInst.prependInst(new IRMove(curFunc.lastBB, v.vR.spillOut, v.vR));

        irProgram.IRFuncList.add(curFunc);
    }


    @Override
    public void visit(ClassDeclNode node){
        isInClass = true;
        curClass = node.symbol;
        if (node.constructor != null)
            node.constructor.accept(this);
        for (FuncDeclNode f : node.methodList)
            f.accept(this);
        isInClass = false;
        curClass = null;
    }

    @Override
    public void visit(VarDeclNode node){
        if (curFunc == null)
            throw new Error("why is it a globalVar");
        VirtualRegister vr = new VirtualRegister(node.name);
        if (!isInline){
            if (isInArgs){
                if (curFunc.paraVirtualRegs.size() >= 6)
                    vr.spillOut = new StackSlot(vr.vrName);
                curFunc.paraVirtualRegs.add(vr);
            }
            node.symbol.vR = vr;
        }
        else
            inlineVarRegMaps.getLast().put(node.symbol, vr);
        if (node.init != null)
            exprDestBuild(node.init, vr);
    }

    @Override
    public void visit(VarListNode node){}

    @Override
    public void visit(TypeNode node){}

    @Override
    public void visit(ArrayTypeNode node){}

    @Override
    public void visit(CustomTypeNode node){}

    @Override
    public void visit(BaseTypeNode node){}

    @Override
    public void visit(StmtNode node){}

    @Override
    public void visit(BlockStmtNode node){
        for (StmtNode x : node.stmtList)
            x.accept(this);
    }

    @Override
    public void visit(ExprStmtNode node){
        node.expr.accept(this);
    }

    @Override
    public void visit(IfStmtNode node) {
        BasicBlock ifThenBB, ifElseBB, ifLeaveBB;
        ifThenBB = new BasicBlock(curFunc, "ifThenBB");
        ifLeaveBB = new BasicBlock(curFunc, "ifLeave");
        if (node.elseStmt == null)
            ifElseBB = ifLeaveBB;
        else ifElseBB = new BasicBlock(curFunc, "ifElseBB");
        trueDestBBMap.put(node.condition, ifThenBB);
        falseDestBBMap.put(node.condition, ifElseBB);

        node.condition.accept(this);

        curBB = ifThenBB;
        node.thenStmt.accept(this);
        curBB.pushTailInst(new IRJump(curBB, ifLeaveBB));

        if (node.elseStmt != null) {
            curBB = ifElseBB;
            node.elseStmt.accept(this);
            curBB.pushTailInst(new IRJump(curBB, ifLeaveBB));
        }

        curBB = ifLeaveBB;
    }
    @Override
    public void visit(WhileStmtNode node){
        //whileStmt: cond -> body or leave, body -> cond
        BasicBlock whileCondBB = new BasicBlock(curFunc, "whileCondition");
        BasicBlock whileBodyBB = new BasicBlock(curFunc, "whileBody");
        BasicBlock whileLeaveBB = new BasicBlock(curFunc, "whileLeave");
        curBB.pushTailInst(new IRJump(curBB, whileCondBB));

        //continue -> cond, break -> leave
        BasicBlock lastContinueDestBB = continueDestBB;
        BasicBlock lastBreakDestBB = breakDestBB;
        continueDestBB = whileCondBB;
        breakDestBB = whileLeaveBB;

        //handle cond
        curBB = whileCondBB;
        trueDestBBMap.put(node.condition, whileBodyBB);
        falseDestBBMap.put(node.condition, whileLeaveBB);
        node.condition.accept(this);

        //handle body
        curBB = whileBodyBB;
        node.body.accept(this);
        curBB.pushTailInst(new IRJump(curBB, whileCondBB));

        continueDestBB = lastContinueDestBB;
        breakDestBB = lastBreakDestBB;

        curBB = whileLeaveBB;
    }

    @Override
    public void visit(ForStmtNode node){
        if (node.init != null)
            node.init.accept(this);

        BasicBlock forCondBB, forStepBB, forBodyBB, forLeaveBB;
        //forStmt: init -> cond, cond -> body or leave, body ->step, step -> cond
        forBodyBB = new BasicBlock(curFunc, "forBody");
        if (node.condition == null)
            forCondBB = forBodyBB;
        else forCondBB = new BasicBlock(curFunc, "forCondition");
        if (node.step == null)
            forStepBB = forCondBB;
        else forStepBB = new BasicBlock(curFunc, "forStep");
        forLeaveBB = new BasicBlock(curFunc, "forLeave");
        curBB.pushTailInst(new IRJump(curBB, forCondBB));

        //continue -> step, break -> leave

        BasicBlock lastContinueDestBB = continueDestBB;
        BasicBlock lastBreakDestBB = breakDestBB;
        continueDestBB = forStepBB;
        breakDestBB = forLeaveBB;

        //handle cond
        if (node.condition != null){
            curBB = forCondBB;
            trueDestBBMap.put(node.condition, forBodyBB);
            falseDestBBMap.put(node.condition, forLeaveBB);
            node.condition.accept(this);
        }

        //handle body
        curBB = forBodyBB;
        node.body.accept(this);
        curBB.pushTailInst(new IRJump(curBB, forStepBB));

        //handle step
        if (node.step != null){
            curBB = forStepBB;
            node.step.accept(this);
            curBB.pushTailInst(new IRJump(curBB, forCondBB));
        }

        continueDestBB = lastContinueDestBB;
        breakDestBB = lastBreakDestBB;

        curBB = forLeaveBB;
    }

    @Override
    public void visit(ContinueStmtNode node){
       curBB.pushTailInst(new IRJump(curBB, continueDestBB));
    }

    @Override
    public void visit(BreakStmtNode node){
        curBB.pushTailInst(new IRJump(curBB, breakDestBB));
    }

    @Override
    public void visit(ReturnStmtNode node){
        if (node.retExpr != null) {
            if (isBool(node.retExpr.calcType))
                exprDestBuild(node.retExpr, RegCollection.vrax);
            else {
                node.retExpr.accept(this);
                curBB.pushTailInst(new IRMove(curBB, RegCollection.vrax, exprSrcMap.get(node.retExpr)));
            }
        }
        if (isInline)
            curBB.pushTailInst(new IRJump(curBB, inlineFuncLeaveBBs.getLast()));
        else curBB.pushTailInst(new IRReturn(curBB));
    }

    @Override
    public void visit(VarStmtNode node){
        node.varDecl.accept(this);
    }

    @Override
    public void visit(EmptyStmtNode node){}

    @Override
    public void visit(ExprNode node){}

    @Override
    public void visit(MemberExprNode node){
        node.obj.accept(this);
        VirtualRegister baseReg = new VirtualRegister("");
        curBB.pushTailInst(new IRMove(curBB, baseReg, exprSrcMap.get(node.obj)));
        IRMem mem = new IRMem();
        Operand store = null;

        //arrayType
        if (node.obj.calcType instanceof TypeArray){ //array.size()
            mem.baseReg = baseReg;
            exprSrcMap.put(node, mem);
            return;
        }

        //member
        if (node.member != null){
            TypeCustom type = (TypeCustom) node.obj.calcType;
            int offset = type.symbol.classSymbolTable.getVarOffset(node.member.name);
            mem.baseReg = baseReg;
            mem.literal = new IntImm(offset);
            store = mem;
        }
        //method
        else {
            IRFunc func = IRFuncMap.get(node.method.symbol.name);
            LinkedList<Operand> args = new LinkedList<>();
            args.add(baseReg);
            for (ExprNode e : node.method.argList){
                e.accept(this);
                args.add(exprSrcMap.get(e));
            }
            if (needInlineOpt(node.method.symbol))
                buildInlineOpt(node.method.symbol, args);
            else curBB.pushTailInst(new IRFuncCall(curBB, RegCollection.vrax, func, args));

            if (!isVoid(node.method.symbol.returnType)){
                VirtualRegister vr = new VirtualRegister("");
                curBB.pushTailInst(new IRMove(curBB, vr, RegCollection.vrax));
                store = vr;
            }
        }

        //for condition
        if (trueDestBBMap.containsKey(node))
            curBB.pushTailInst(new IRBranch(curBB, IRBranch.Cop.NE, store, new IntImm(0),
                    trueDestBBMap.get(node), falseDestBBMap.get(node)));
        else exprSrcMap.put(node, store);

    }

    @Override
    public void visit(SubscriptExprNode node){
        node.array.accept(this);
        Operand array = exprSrcMap.get(node.array);
        node.subscript.accept(this);
        Operand subscript = exprSrcMap.get(node.subscript);

        VirtualRegister baseReg;
        if (array instanceof IRRegister)
            baseReg = (VirtualRegister)array;
        else {
            baseReg  = new VirtualRegister("");
            curBB.pushTailInst(new IRMove(curBB, baseReg, array));
        }

        IRMem store;
        if (subscript instanceof IntImm){
            store = new IRMem();
            store.baseReg = baseReg;
            store.literal = new IntImm((((IntImm) subscript).value + 1) * Configuration.regSize);
        }
        else if (subscript instanceof IRRegister)
            store = new IRMem(baseReg, (IRRegister)subscript, Configuration.regSize, new IntImm(Configuration.regSize));
        else if (subscript instanceof IRMem){
            VirtualRegister vr = new VirtualRegister("");
            curBB.pushTailInst(new IRMove(curBB, vr ,subscript));
            store = new IRMem(baseReg, vr, Configuration.regSize, new IntImm(Configuration.regSize));
        }
        else store = null;

        //for condition
        if (trueDestBBMap.containsKey(node))
            curBB.pushTailInst(new IRBranch(curBB, IRBranch.Cop.NE, store, new IntImm(0),
                    trueDestBBMap.get(node), falseDestBBMap.get(node)));
        else exprSrcMap.put(node, store);
    }

    @Override
    public void visit(FuncCallExprNode node){
        LinkedList<Operand> args = new LinkedList<>();
        if (!node.symbol.isGlobalFunc)
            args.add(curThisPointer); //add this
        for (int i = 0; i < node.argList.size(); ++i){
            node.argList.get(i).accept(this);
            args.add(exprSrcMap.get(node.argList.get(i)));
        }
        if (needInlineOpt(node.symbol))
            buildInlineOpt(node.symbol, args);
        else curBB.pushTailInst(new IRFuncCall(curBB, RegCollection.vrax, IRFuncMap.get(node.symbol.name), args));

        //for condition
        if (trueDestBBMap.containsKey(node))
            curBB.pushTailInst(new IRBranch(curBB, IRBranch.Cop.NE, RegCollection.vrax, new IntImm(0),
                    trueDestBBMap.get(node), falseDestBBMap.get(node)));
        else {
            if (!isVoid(node.symbol.returnType)){
                VirtualRegister vr = new VirtualRegister("");
                curBB.pushTailInst(new IRMove(curBB, vr, RegCollection.vrax));
                exprSrcMap.put(node, vr);
            }
        }
    }

    @Override
    public void visit(NewExprNode node) {
        //get baseType constructor
        IRFunc constructor = null;
        if (node.notDefine == 0 && node.calcType instanceof TypeCustom) {
            TypeCustom type = (TypeCustom) node.calcType;
            if (!type.name.equals("string")) {
                FuncSymbol f = type.symbol.classSymbolTable.getFunc(type.name);
                if (f != null)
                    constructor = IRFuncMap.get(f.name);
            }
        }

        LinkedList<Operand> defineSizeList = new LinkedList<>();
        for (ExprNode x : node.defineSizeList){
            x.accept(this);
            defineSizeList.add(exprSrcMap.get(x));
        }

        if (node.type instanceof BaseTypeNode){
            Operand p = processNewArray(defineSizeList, 0, null); //int bool null
            exprSrcMap.put(node, p);
        }
        else if (node.notDefine > 0){
            Operand p = processNewArray(defineSizeList, 0, null);
            exprSrcMap.put(node, p);
        }
        else {
            //CustomType or arrayType with notDefine = 0
            int size;
            //string
            if (node.calcType instanceof TypeCustom && ((TypeCustom)node.calcType).name.equals("string"))
                size = 2 * Configuration.regSize;
            else size = node.calcType.getSize();
            Operand p = processNewArray(defineSizeList, size, constructor);
            exprSrcMap.put(node, p);
        }
    }

    @Override
    public void visit(UnaryExprNode node){
        if (node.uop.equals("!")){
            trueDestBBMap.put(node.expr, falseDestBBMap.get(node));
            falseDestBBMap.put(node.expr, trueDestBBMap.get(node));
            node.expr.accept(this);
            return;
        }
        node.expr.accept(this);
        Operand o = exprSrcMap.get(node.expr);
        if (node.uop.equals("x++") || node.uop.equals("x--")){
            if (!(o instanceof IRStorePos))
                throw new Error("waaaaaaagh");
            VirtualRegister last = new VirtualRegister("");
            curBB.pushTailInst(new IRMove(curBB, last, o));
            if (node.uop.equals("x++"))
                curBB.pushTailInst(new IRUnary(curBB, IRUnary.Uop.INC, (IRStorePos)o));
            else curBB.pushTailInst(new IRUnary(curBB, IRUnary.Uop.DEC, (IRStorePos)o));
            exprSrcMap.put(node, last);
        }
        else if (node.uop.equals("++x") || node.uop.equals("--x")){
            if (!(o instanceof IRStorePos))
                throw new Error("waaaaaaaghAgain");
            if (node.uop.equals("++x"))
                curBB.pushTailInst(new IRUnary(curBB, IRUnary.Uop.INC, (IRStorePos)o));
            else curBB.pushTailInst(new IRUnary(curBB, IRUnary.Uop.DEC, (IRStorePos)o));
            exprSrcMap.put(node, o);
        }
        else if (node.uop.equals("+"))
            exprSrcMap.put(node, o);
        else if (node.uop.equals("-")){
            VirtualRegister nxt = new VirtualRegister("");
            curBB.pushTailInst(new IRMove(curBB, nxt, o));
            curBB.pushTailInst(new IRUnary(curBB, IRUnary.Uop.NEG, nxt));
            exprSrcMap.put(node, nxt);
        }
        else if(node.uop.equals("~")){
            VirtualRegister nxt = new VirtualRegister("");
            curBB.pushTailInst(new IRMove(curBB, nxt, o));
            curBB.pushTailInst(new IRUnary(curBB, IRUnary.Uop.NOT, nxt));
            exprSrcMap.put(node, nxt);
        }
    }

    @Override
    public void visit(BinaryExprNode node){
        switch (node.bop){
            case "&&":
            case "||":
                processLogicalBop(node);
                break;
            case "*":
            case "/":
            case "%":
            case "+":
            case "-":
            case ">>":
            case "<<":
            case "&":
            case "|":
            case "^":
                processArithBop(node);
                break;
            case "<":
            case ">":
            case ">=":
            case "<=":
            case "==":
            case "!=":
                processCmpBop(node);
                break;
        }
    }

    @Override
    public void visit(AssignExprNode node){
        node.lt.accept(this);
        IRStorePos lt = (IRStorePos) exprSrcMap.get(node.lt);
        exprDestBuild(node.rt, lt);
    }

    @Override
    public void visit(IdExprNode node) {
        Operand op;
        if (node.name.equals("this"))
            op = curThisPointer;
        else if (!node.symbol.isClassMember) {
            if (isInline)
                op = inlineVarRegMaps.getLast().get(node.symbol);
            else op = node.symbol.vR;
        }
        else {
            op = new IRMem();
            ((IRMem) op).baseReg = curThisPointer;
            ((IRMem) op).literal = new IntImm(curClass.classSymbolTable.getVarOffset(node.name));
        }

        //for condition
        if (trueDestBBMap.containsKey(node)) {
            IRBranch b = new IRBranch(curBB, IRBranch.Cop.NE, op, new IntImm(0), trueDestBBMap.get(node), falseDestBBMap.get(node));
            curBB.pushTailInst(b);
        }
        else exprSrcMap.put(node, op);
    }
    @Override
    public void visit(ConstExprNode node){}

    @Override
    public void visit(ConstIntNode node){
        exprSrcMap.put(node, new IntImm(Integer.valueOf(node.valueStr)));
    }

    @Override
    public void visit(ConstBoolNode node){
        BasicBlock dest;
        IntImm val;
        if (node.valueStr.equals("true")) {
            dest = trueDestBBMap.get(node);
            val = new IntImm(1);
        }
        else {
            dest = falseDestBBMap.get(node);
            val = new IntImm(0);
        }
        if (dest != null)
            curBB.pushTailInst(new IRJump(curBB, dest));
        exprSrcMap.put(node, val);
    }

    @Override
    public void visit(ConstNullNode node){
        exprSrcMap.put(node, new IntImm(0));
    }

    @Override
    public void visit(ConstStringNode node){
        StaticData sd = new StaticData("staticString", node.valueStr);
        irProgram.staticDataList.add(sd);
        exprSrcMap.put(node, sd);
    }


    //private boolean isInt(TypeType t) {
        //return t instanceof TypeBase && ((TypeBase) t).name.equals("int");
    //}

    private boolean isBool(TypeType t) {
        return t instanceof TypeBase && ((TypeBase) t).name.equals("bool");
    }
    private boolean isVoid(TypeType t) {
        return t instanceof TypeBase && ((TypeBase) t).name.equals("void");
    }

    private boolean isString(TypeType t) {
        return t instanceof TypeCustom && ((TypeCustom) t).name.equals("string");
    }

    private void exprDestBuild(ExprNode expr, IRStorePos vr){
        if (isBool(expr.calcType)){
            BasicBlock trueBB = new BasicBlock(curFunc, "trueBB");
            BasicBlock falseBB = new BasicBlock(curFunc, "falseBB");
            trueDestBBMap.put(expr, trueBB);
            falseDestBBMap.put(expr, falseBB);
            expr.accept(this);
            BasicBlock joinBB = new BasicBlock(curFunc, "joinTF");
            trueBB.pushTailInst(new IRMove(trueBB, vr, new IntImm(1)));
            trueBB.pushTailInst(new IRJump(trueBB, joinBB));
            falseBB.pushTailInst(new IRMove(falseBB, vr, new IntImm(0)));
            falseBB.pushTailInst(new IRJump(falseBB, joinBB));
            curBB = joinBB;
        }
        else {
            exprDestMap.put(expr, vr);
            expr.accept(this);
            Operand src = exprSrcMap.get(expr);
            if (src != vr)
                curBB.pushTailInst(new IRMove(curBB, vr, src));
        }
    }

    private int countOp(ExprNode expr){
        int count = 0;
        if (expr == null)
            return count;
        if (expr instanceof AssignExprNode){
            count += countOp(((AssignExprNode) expr).lt);
            count += countOp(((AssignExprNode) expr).rt);
        }
        else if (expr instanceof BinaryExprNode){
            count += countOp(((BinaryExprNode) expr).lt);
            count += countOp(((BinaryExprNode) expr).rt);
        }
        else if (expr instanceof FuncCallExprNode){
            for (ExprNode e : ((FuncCallExprNode) expr).argList)
                count += countOp(e);
        }
        else if (expr instanceof MemberExprNode){
            if (((MemberExprNode) expr).method != null)
                count += countOp(((MemberExprNode) expr).method);
            else count += countOp(((MemberExprNode) expr).member);
        }
        else if (expr instanceof NewExprNode) {
            for (ExprNode e : ((NewExprNode) expr).defineSizeList)
                count += countOp(e);
        }
        else if (expr instanceof SubscriptExprNode){
            count += countOp(((SubscriptExprNode) expr).array);
            count += countOp(((SubscriptExprNode) expr).subscript);
        }
        else if (expr instanceof UnaryExprNode)
            count += countOp(((UnaryExprNode) expr).expr) + 1;
        else count += 1;
        return count;
    }

    private int countOp(StmtNode stmt){
        int count = 0;
        if (stmt == null)
            return count;
        if (stmt instanceof BlockStmtNode)
            count += countOp(((BlockStmtNode) stmt).stmtList);
        else if (stmt instanceof EmptyStmtNode)
            count = 0;
        else if (stmt instanceof ExprStmtNode)
            count += countOp(((ExprStmtNode) stmt).expr);
        else if (stmt instanceof ForStmtNode){
            count += countOp(((ForStmtNode) stmt).body);
            count += countOp(((ForStmtNode) stmt).condition);
            count += countOp(((ForStmtNode) stmt).init);
            count += countOp(((ForStmtNode) stmt).step);
        }
        else if (stmt instanceof IfStmtNode){
            count += countOp(((IfStmtNode) stmt).thenStmt);
            count += countOp(((IfStmtNode) stmt).elseStmt);
        }
        else if (stmt instanceof ReturnStmtNode)
            count += countOp(((ReturnStmtNode) stmt).retExpr);
        else if (stmt instanceof VarStmtNode)
            count += countOp(((VarStmtNode) stmt).varDecl.init);
        else if (stmt instanceof WhileStmtNode){
            count += countOp(((WhileStmtNode) stmt).condition);
            count += countOp(((WhileStmtNode) stmt).body);
        }
        else count += 1;
        return count;

    }

    private int countOp(List<StmtNode> stmtList){
        int count = 0;
        for (StmtNode s : stmtList)
            count += countOp(s);
        return count;
    }

    private boolean needInlineOpt(FuncSymbol f){
        if (!Configuration.useInlineOpt || !ASTFuncDeclMap.containsKey(f.name))
            return false;
        if (!f.globalVarSet.isEmpty() || !f.isGlobalFunc)
            return false;
        FuncDeclNode func = ASTFuncDeclMap.get(f.name);
        if (!funcOpCntMap.containsKey(f))
            funcOpCntMap.put(f, countOp(func.block));
        if (funcOpCntMap.get(f) >= Configuration.inlineOpCnt)
            return false;
        return inlineVarRegMaps.size() <= Configuration.inlineMaxDepth;
    }

    private void buildInlineOpt(FuncSymbol f, LinkedList<Operand> args){
        HashMap<VarSymbol, VirtualRegister> varMap = new HashMap<>();
        inlineVarRegMaps.addLast(varMap);
        LinkedList<VirtualRegister> argVregs = new LinkedList<>();
        for (Operand o : args){
            VirtualRegister vr = new VirtualRegister("");
            curBB.pushTailInst(new IRMove(curBB, vr, o));
            argVregs.addLast(vr);
        }

        FuncDeclNode func = ASTFuncDeclMap.get(f.name);
        for (int i = 0; i < func.parameterList.size(); ++i)
            varMap.put(func.parameterList.get(i).symbol, argVregs.get(i));

        BasicBlock funcLeaveBB = new BasicBlock(curFunc, "inlineLeaveOf_"+f.name);
        inlineFuncLeaveBBs.addLast(funcLeaveBB);

        BasicBlock funcBodyBB = new BasicBlock(curFunc, "inlineBodyOf_"+f.name);
        curBB.pushTailInst(new IRJump(curBB, funcBodyBB));
        curBB = funcBodyBB;
        boolean lastIsInline = isInline;
        isInline = true;
        for (StmtNode s : func.block)
            s.accept(this);
        if (!(curBB.lastInst instanceof IRJump))
            curBB.pushTailInst(new IRJump(curBB, funcLeaveBB));

        curBB = funcLeaveBB;
        inlineVarRegMaps.removeLast();
        inlineFuncLeaveBBs.removeLast();
        isInline = lastIsInline;
    }

    private Operand processNewArray(LinkedList<Operand> defineSizeList, int size, IRFunc consturctor){
        //for recursiveCall
        if (defineSizeList.size() == 0){
            if (size == 0) //rest > 0
                return new IntImm(0);
            else {
                VirtualRegister pointer = new VirtualRegister("");
                //malloc
                curBB.pushTailInst(new IRFuncCall(curBB, RegCollection.vrax, exMalloc, new IntImm(size)));
                curBB.pushTailInst(new IRMove(curBB, pointer, RegCollection.vrax));
                //construct
                if (consturctor != null)
                    curBB.pushTailInst(new IRFuncCall(curBB, RegCollection.vrax, consturctor, pointer));
                else {
                    IRMem mem = new IRMem();
                    mem.baseReg = pointer;
                    if (size == Configuration.regSize){
                        curBB.pushTailInst(new IRMove(curBB, mem, new IntImm(0)));
                    }
                    else if (size == 2 * Configuration.regSize){ //string
                        curBB.pushTailInst(new IRBinary(curBB, IRBinary.Bop.ADD,
                                pointer, new IntImm(Configuration.regSize)));
                        curBB.pushTailInst(new IRMove(curBB, mem, new IntImm(0)));
                        curBB.pushTailInst(new IRBinary(curBB, IRBinary.Bop.SUB,
                                pointer, new IntImm(Configuration.regSize)));
                    }
                }
                return pointer;
            }
        }

        //array
        else {
            VirtualRegister pointer = new VirtualRegister("");
            VirtualRegister indexReg = new VirtualRegister("");
            VirtualRegister sizeReg = new VirtualRegister("");
            IRMem src = new IRMem(), addr = new IRMem();
            //get mallocSize
            curBB.pushTailInst(new IRMove(curBB, indexReg, defineSizeList.get(0)));
            src.indexReg = indexReg;
            src.scale = Configuration.regSize;
            src.literal = new IntImm(Configuration.regSize);
            curBB.pushTailInst(new IRLea(curBB, sizeReg, src));
            //malloc
            curBB.pushTailInst(new IRFuncCall(curBB, RegCollection.vrax, exMalloc, sizeReg));
            curBB.pushTailInst(new IRMove(curBB, pointer, RegCollection.vrax));
            addr.baseReg = pointer;
            curBB.pushTailInst(new IRMove(curBB, addr, indexReg));

            BasicBlock condBB = new BasicBlock(curFunc, "newArrayCondBB");
            BasicBlock bodyBB = new BasicBlock(curFunc, "newArrayBodyBB");
            BasicBlock leaveBB = new BasicBlock(curFunc, "newARrayLeaveBB");
            curBB.pushTailInst(new IRJump(curBB, condBB));

            curBB = condBB;
            curBB.pushTailInst(new IRBranch(curBB, IRBranch.Cop.G, indexReg, new IntImm(0), bodyBB, leaveBB));

            curBB = bodyBB;
            IRMem a = new IRMem();
            a.baseReg = pointer;
            a.indexReg = indexReg;
            a.scale = Configuration.regSize;
            if (defineSizeList.size() == 1){
                Operand p = processNewArray(new LinkedList<>(), size, consturctor);
                curBB.pushTailInst(new IRMove(curBB, a, p));
            }
            else {
                LinkedList<Operand> rest = new LinkedList<>();
                for (int i = 1; i < defineSizeList.size(); ++i)
                    rest.add(defineSizeList.get(i));
                Operand p = processNewArray(rest, size, consturctor);
                curBB.pushTailInst(new IRMove(curBB, a, p));
            }
            curBB.pushTailInst(new IRUnary(curBB, IRUnary.Uop.DEC, indexReg));
            curBB.pushTailInst(new IRJump(curBB, condBB));

            curBB = leaveBB;
            return pointer;
        }

    }

    private void processLogicalBop(BinaryExprNode node){
        BasicBlock checkNxtCondBB = new BasicBlock(curFunc, "checkNxtCondBB");
        if (node.bop.equals("&&")){
            trueDestBBMap.put(node.lt, checkNxtCondBB);
            falseDestBBMap.put(node.lt, falseDestBBMap.get(node));
        }
        else if (node.bop.equals("||")){
            trueDestBBMap.put(node.lt, trueDestBBMap.get(node));
            falseDestBBMap.put(node.lt, checkNxtCondBB);
        }
        node.lt.accept(this);

        curBB = checkNxtCondBB;
        trueDestBBMap.put(node.rt, trueDestBBMap.get(node));
        falseDestBBMap.put(node.rt, falseDestBBMap.get(node));
        node.rt.accept(this);
    }

    private void processArithBop(BinaryExprNode node){
        node.lt.accept(this);
        node.rt.accept(this);
        Operand lt = exprSrcMap.get(node.lt);
        Operand rt = exprSrcMap.get(node.rt);
        IRStorePos store = new VirtualRegister("");

        if (node.lt.calcType instanceof TypeCustom && ((TypeCustom) node.lt.calcType).name.equals("string") && node.bop.equals("+")){
            if (lt instanceof IRMem && !(lt instanceof StackSlot)){
                VirtualRegister vrlt = new VirtualRegister("");
                curBB.pushTailInst(new IRMove(curBB, vrlt, lt));
                lt = vrlt;
            }
            if (rt instanceof IRMem && !(rt instanceof StackSlot)){
                VirtualRegister vrrt = new VirtualRegister("");
                curBB.pushTailInst(new IRMove(curBB, vrrt, rt));
                rt = vrrt;
            }
            curBB.pushTailInst(new IRFuncCall(curBB, RegCollection.vrax, libStringConcat, lt, rt));
            curBB.pushTailInst(new IRMove(curBB, store, RegCollection.vrax));
            exprSrcMap.put(node, store);
            return;
        }

        if (node.bop.equals("*")){
            IRBinary.Bop bop = IRBinary.Bop.MUL;
            curBB.pushTailInst(new IRMove(curBB, RegCollection.vrax, lt));
            curBB.pushTailInst(new IRBinary(curBB, bop, null, rt));
            curBB.pushTailInst(new IRMove(curBB, store, RegCollection.vrax));
            exprSrcMap.put(node, store);
            return;
        }
        if (node.bop.equals("/")){
            IRBinary.Bop bop = IRBinary.Bop.DIV;
            curBB.pushTailInst(new IRMove(curBB, RegCollection.vrax, lt));
            curBB.pushTailInst(new IRCdq(curBB)); //for divOp
            curBB.pushTailInst(new IRBinary(curBB, bop, null, rt));
            curBB.pushTailInst(new IRMove(curBB, store, RegCollection.vrax));
            exprSrcMap.put(node, store);
            return;
        }
        if (node.bop.equals("%")){
            IRBinary.Bop bop = IRBinary.Bop.MOD;
            curBB.pushTailInst(new IRMove(curBB, RegCollection.vrax, lt));
            curBB.pushTailInst(new IRCdq(curBB)); //for divOp
            curBB.pushTailInst(new IRBinary(curBB, bop, null, rt));
            curBB.pushTailInst(new IRMove(curBB, store, RegCollection.vrdx));
            exprSrcMap.put(node, store);
            return;
        }

        IRStorePos dest = exprDestMap.get(node);
        IRBinary.Bop bop = null;
        switch (node.bop){
            case "+":
                bop = IRBinary.Bop.ADD;
                break;
            case "-":
                bop = IRBinary.Bop.SUB;
                break;
            case ">>":
                bop = IRBinary.Bop.SAR;
                break;
            case "<<":
                bop = IRBinary.Bop.SAL;
                break;
            case "&":
                bop = IRBinary.Bop.AND;
                break;
            case "|":
                bop = IRBinary.Bop.OR;
                break;
            case "^":
                bop = IRBinary.Bop.XOR;
                break;
        }

        if (lt == dest){
            store = dest;
            if (node.bop.equals(">>") || node.bop.equals("<<")){
                curBB.pushTailInst(new IRMove(curBB, RegCollection.vrcx, rt));
                curBB.pushTailInst(new IRBinary(curBB, bop, store, RegCollection.vrcx));
            }
            else
                curBB.pushTailInst(new IRBinary(curBB, bop, store, rt));
        }
        else if (rt == dest && (!(node.bop.equals("-"))) && (!(node.bop.equals("<<"))) && (!(node.bop.equals(">>")))){
            //can revert lt and rt
            store = dest;
            curBB.pushTailInst(new IRBinary(curBB, bop, store, lt));
        }
        else {
            if (node.bop.equals(">>") || node.bop.equals("<<")){
                curBB.pushTailInst(new IRMove(curBB, store, lt));
                curBB.pushTailInst(new IRMove(curBB, RegCollection.vrcx, rt));
                curBB.pushTailInst(new IRBinary(curBB, bop, store, RegCollection.vrcx));
            }
            else {
                curBB.pushTailInst(new IRMove(curBB, store, lt));
                curBB.pushTailInst(new IRBinary(curBB, bop, store, rt));
            }
        }
        exprSrcMap.put(node, store);
    }

    private void processCmpBop(BinaryExprNode node){
        node.lt.accept(this);
        node.rt.accept(this);
        Operand lt = exprSrcMap.get(node.lt);
        Operand rt = exprSrcMap.get(node.rt);
        VirtualRegister store = new VirtualRegister("");

        IRBranch.Cop cop = null;
        switch(node.bop) {
            case "==":
                cop = IRBranch.Cop.E;
                break;
            case "!=":
                cop = IRBranch.Cop.NE;
                break;
            case ">":
                cop = IRBranch.Cop.G;
                break;
            case "<":
                cop = IRBranch.Cop.L;
                break;
            case ">=":
                cop = IRBranch.Cop.GE;
                break;
            case "<=":
                cop = IRBranch.Cop.LE;
                break;
        }
        if (node.lt.calcType instanceof TypeCustom && ((TypeCustom) node.lt.calcType).name.equals("string")){
            curBB.pushTailInst(new IRFuncCall(curBB, RegCollection.vrax, libStringCmp, lt, rt));
            curBB.pushTailInst(new IRMove(curBB, store, RegCollection.vrax));
            BasicBlock trueBB = trueDestBBMap.get(node);
            BasicBlock falseBB = falseDestBBMap.get(node);
            if (trueBB != null && falseBB != null)
                curBB.pushTailInst(new IRBranch(curBB, cop, store, new IntImm(0), trueBB, falseBB));
            return;
        }
        if (lt instanceof IRMem && rt instanceof IRMem){
            curBB.pushTailInst(new IRMove(curBB, store, lt));
            lt = store;
        }
        BasicBlock trueBB = trueDestBBMap.get(node);
        BasicBlock falseBB = falseDestBBMap.get(node);
        if (trueBB != null && falseBB != null)
            curBB.pushTailInst(new IRBranch(curBB, cop, lt, rt, trueBB, falseBB));
    }


}
