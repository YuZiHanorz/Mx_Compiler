package mxCompiler.Frontend;

import mxCompiler.Ast.AstVisitor;
import mxCompiler.Ast.node.*;
import mxCompiler.Symbol.*;
import mxCompiler.Type.TypeArray;
import mxCompiler.Type.TypeBase;
import mxCompiler.Type.TypeCustom;
import mxCompiler.Type.TypeType;
import mxCompiler.Utility.ErrorTable;

import java.util.HashMap;
import java.util.Map;

public class AstScopeChecker implements AstVisitor{
    public GlobalSymbolTable globalSYmbolTable;
    public SymbolTable currentSymbolTable;
    public FuncSymbol currentFunction;
    public Map<SymbolTable, CustomTypeSymbol> classOwn;
    public ErrorTable errorTable;

    public AstScopeChecker(ErrorTable errorTable){
        this.globalSYmbolTable = new GlobalSymbolTable();
        this.currentSymbolTable = globalSYmbolTable;
        this.currentFunction = null;
        this.classOwn = new HashMap<>();
        this.errorTable = errorTable;
    }

    @Override
    public void visit(ProgramNode node){
        //not effected by forward reference
        for (ClassDeclNode x : node.globalClassList)
            classCheckIn(x);
        //wtf: type can be class that is defined later
        for (ClassDeclNode x : node.globalClassList){
            classMethodCheckIn(x);
            classMemberCheckIn(x);
        }
        for (FuncDeclNode x : node.globalFuncList)
            globalFuncCheckIn(x);
        if (errorTable.somethingWrong())
            return;

        //fucking forward reference
        for (DeclNode x : node.declList){
            if (x instanceof ClassDeclNode)
                classMethodDefine((ClassDeclNode)x);
            else if (x instanceof FuncDeclNode)
                funcDefine((FuncDeclNode) x, null);
            else globalVarCheckIn((VarDeclNode) x);
        }
        if (errorTable.somethingWrong())
            return;
    }

    @Override
    public void visit(DeclNode node){}

    @Override
    public void visit(FuncDeclNode node){
        /*funcCheckIn(node, classOwn.get(currentSymbolTable));
        funcDefine(node, classOwn.get(currentSymbolTable));*/
    }

    @Override
    public void visit(ClassDeclNode node){
        /*classCheckIn(node);
        classMemberCheckIn(node);
        classMethodCheckIn(node);
        classMethodDefine(node);*/
    }

    @Override
    public void visit(VarDeclNode node){
        varCheckIn(node);
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
        SymbolTable st = new SymbolTable(currentSymbolTable);
        into(st);
        for (StmtNode x : node.stmtList)
            x.accept(this);
        out();
    }

    @Override
    public void visit(ExprStmtNode node){
        node.expr.accept(this);
    }

    @Override
    public void visit(IfStmtNode node){
        node.condition.accept(this);
        node.thenStmt.accept(this);
        if (node.elseStmt != null)
            node.elseStmt.accept(this);
    }

    @Override
    public void visit(WhileStmtNode node){
        node.condition.accept(this);
        node.body.accept(this);
    }

    @Override
    public void visit(ForStmtNode node){
        if (node.init != null)
            node.init.accept(this);
        if (node.condition != null)
            node.condition.accept(this);
        if (node.step != null)
            node.step.accept(this);
        if (node.body != null)
            node.body.accept(this);
    }

    @Override
    public void visit(ContinueStmtNode node){}

    @Override
    public void visit(BreakStmtNode node){}

    @Override
    public void visit(ReturnStmtNode node){
        if (node.retExpr != null)
            node.retExpr.accept(this);
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
        if (node.obj.calcType instanceof TypeBase)
            errorTable.addError(node.obj.location, "BaseType has no such member or method");
        else if (node.obj.calcType instanceof TypeArray){
            if (node.method != null && node.method.funcName.equals("size"))
                node.calcType = new TypeBase("int", globalSYmbolTable.getBaseType("int"));
            else
                errorTable.addError(node.location, "ArratType has no such member or method");
        }
        else if (node.obj.calcType instanceof TypeCustom){
            TypeCustom ct = (TypeCustom) (node.obj.calcType);
            if (node.member != null){
                node.member.symbol = getVar(node.member.name, ct.symbol.classSymbolTable);
                if (node.member.symbol != null)
                    node.calcType = node.member.calcType = node.member.symbol.type;
                else
                    errorTable.addError(node.member.location,
                            "Class<"+ ct.name + "> has no member<" + node.member.name + ">");
            }
            else if (node.method != null){
                node.method.symbol = getFunc(node.method.funcName, ct.symbol.classSymbolTable);
                if (node.method.symbol != null) {
                    for (ExprNode e : node.method.argList)
                        e.accept(this);
                    node.calcType = node.method.calcType = node.method.symbol.returnType;
                }
                else
                    errorTable.addError(node.method.location,
                            "Class<"+ ct.name + "> has no method<" + node.method.funcName + ">");
            }
            else
                errorTable.addError(node.location, "what am I doing here in memberExpr?");
        }
        else
            errorTable.addError(node.obj.location, "the fucking obj has no type?");
    }

    @Override
    public void visit(SubscriptExprNode node){
        node.array.accept(this);
        node.subscript.accept(this);
        if (node.array.calcType instanceof TypeArray) {
            TypeType eT = ((TypeArray) node.array.calcType).elementType;
            int dim = ((TypeArray) node.array.calcType).dim;
            if (dim == 1)
                node.calcType = eT;
            else node.calcType = new TypeArray(eT, dim-1);
        }
        else
            errorTable.addError(node.location, "obj[] is invalid since obj is not an array");
    }

    @Override
    public void visit(FuncCallExprNode node){
        FuncSymbol fs = getFunc(node.funcName, currentSymbolTable);
        if (fs != null){
            for (ExprNode x : node.argList)
                x.accept(this);
            node.calcType = fs.returnType;
            node.symbol = fs;
            //if (currentFunction != null)
                //currentFunction.funcCallSet.add(fs);
        }
        else
            errorTable.addError(node.location, "Func <" + node.funcName+"> really exists?");
    }

    @Override
    public void visit(NewExprNode node){
        for (ExprNode x : node.defineSizeList)
            x.accept(this);
        TypeType eT = decideVarType(node.type);
        if (eT == null){
            errorTable.addError(node.type.location, "cannot decide type");
            return;
        }
        if (node.type instanceof BaseTypeNode && ((BaseTypeNode) node.type).typeName.equals("void")){
            errorTable.addError(node.type.location, "cannot new voidType");
            return;
        }
        int dim = node.defineSizeList.size() + node.notDefine;
        if (dim == 0)
            node.calcType = eT;
        else node.calcType = new TypeArray(eT, dim);
    }

    @Override
    public void visit(UnaryExprNode node){
        node.expr.accept(this);
        node.calcType = node.expr.calcType;
    }

    @Override
    public void visit(BinaryExprNode node){
        node.lt.accept(this);
        node.rt.accept(this);
        if (node.bop.equals("==") || node.bop.equals("!=")
            || node.bop.equals("<") || node.bop.equals(">")
            || node.bop.equals("<=") || node.bop.equals(">="))
            node.calcType = new TypeBase("bool", globalSYmbolTable.getBaseType("bool"));
        else node.calcType = node.lt.calcType;
    }

    @Override
    public void visit(AssignExprNode node){
        node.lt.accept(this);
        node.rt.accept(this);
        node.calcType = new TypeBase("void", globalSYmbolTable.getBaseType("void"));
    }

    @Override
    public void visit(IdExprNode node){
        VarSymbol vs = getVar(node.name, currentSymbolTable);
        if (vs == null){
            errorTable.addError(node.location, "var <" + node.name + "> really exists?");
            return;
        }
        node.symbol = vs;
        node.calcType = node.symbol.type;
        if (vs.isGlobal && currentFunction != null)
            currentFunction.globalVarSet.add(vs);

    }

    @Override
    public void visit(ConstExprNode node){}

    @Override
    public void visit(ConstIntNode node){
        node.calcType = new TypeBase("int", globalSYmbolTable.getBaseType("int"));
    }

    @Override
    public void visit(ConstBoolNode node){
        node.calcType = new TypeBase("bool", globalSYmbolTable.getBaseType("bool"));
    }

    @Override
    public void visit(ConstNullNode node){
        node.calcType = new TypeCustom("null", globalSYmbolTable.getCustomType("null"));
    }

    @Override
    public void visit(ConstStringNode node){
        node.calcType = new TypeCustom("string", globalSYmbolTable.getCustomType("string"));
    }

    private void into(SymbolTable st){
        currentSymbolTable = st;
    }

    private void out(){
        assert (currentSymbolTable.Fa != null);
        currentSymbolTable = currentSymbolTable.Fa;
    }

    private TypeType decideVarType(TypeNode node){
        if (node instanceof BaseTypeNode){
            BaseTypeSymbol sb = globalSYmbolTable.getBaseType(((BaseTypeNode) node).typeName);
            if (sb != null)
                return new TypeBase(sb.name, sb);
            else return null;
        }
        else if (node instanceof CustomTypeNode){
            CustomTypeSymbol sb = globalSYmbolTable.getCustomType(((CustomTypeNode) node).typeName);
            if (sb != null)
                return new TypeCustom(sb.name, sb);
            else return null;
        }
        else if (node instanceof ArrayTypeNode){
            if (((ArrayTypeNode) node).elementType instanceof BaseTypeNode
                   && ((BaseTypeNode) ((ArrayTypeNode) node).elementType).typeName.equals("void")){
               errorTable.addError(((ArrayTypeNode) node).elementType.location, "arrayBaseType cannot be void");
               return null;
            }
            TypeType baseType = decideVarType(((ArrayTypeNode) node).elementType);
            if (baseType != null)
                return new TypeArray(baseType, ((ArrayTypeNode) node).dim);
            else return null;
        }
        else return null;
    }

    private void classCheckIn(ClassDeclNode classDecl){
        if (globalSYmbolTable.getVar(classDecl.name) != null){
            errorTable.addError(classDecl.location, "className conflicts with existed globalVar");
            return;
        }
        if (globalSYmbolTable.getCustomType(classDecl.name) != null){
            errorTable.addError(classDecl.location, "className conflicts with existed class");
            return;
        }
        if (globalSYmbolTable.getFunc(classDecl.name) != null){
            errorTable.addError(classDecl.location, "className conflicts with existed function");
            return;
        }

        classDecl.symbol = new CustomTypeSymbol();
        classDecl.symbol.name = classDecl.name;
        classDecl.symbol.location = classDecl.location;
        classDecl.symbol.classSymbolTable = new SymbolTable(globalSYmbolTable);

        classOwn.put(classDecl.symbol.classSymbolTable, classDecl.symbol);
        globalSYmbolTable.putCustomType(classDecl.name, classDecl.symbol);
    }

    private void globalFuncCheckIn(FuncDeclNode funcDecl){
        if (globalSYmbolTable.getVar(funcDecl.name) != null){
            errorTable.addError(funcDecl.location, "funcName conflicts with existed globalVar");
            return;
        }
        if (globalSYmbolTable.getCustomType(funcDecl.name) != null){
            errorTable.addError(funcDecl.location, "funcName conflicts with existed class");
            return;
        }
        if (globalSYmbolTable.getFunc(funcDecl.name) != null){
            errorTable.addError(funcDecl.location, "funcName conflicts with existed function");
            return;
        }

        funcDecl.symbol = new FuncSymbol();
        funcDecl.symbol.name = funcDecl.name;
        funcDecl.symbol.location = funcDecl.location;
        funcDecl.symbol.isGlobalFunc = true;
        funcDecl.symbol.funcSymbolTable = null;
        funcDecl.symbol.returnType = decideVarType(funcDecl.retType);
        if (funcDecl.symbol.returnType == null){
            errorTable.addError(funcDecl.retType.location, "can not decide returnType");
            return;
        }
        for (VarDeclNode x : funcDecl.parameterList){
            TypeType type = decideVarType(x.type);
            if (type == null){
                errorTable.addError(funcDecl.retType.location, "can not decide paraType");
                return;
            }
            funcDecl.symbol.parameterTypeList.add(type);
            funcDecl.symbol.parameterNameList.add(x.name);
        }
        globalSYmbolTable.putFunc(funcDecl.name, funcDecl.symbol);
    }

    private void globalVarCheckIn(VarDeclNode varDecl){
        TypeType type = decideVarType(varDecl.type);
        if (type == null){
            errorTable.addError(varDecl.type.location, "can not decide varType");
            return;
        }
        if (type instanceof TypeBase && ((TypeBase) type).name.equals("void") ){
            errorTable.addError(varDecl.location, "varType cannot be void");
            return;
        }
        if (type instanceof TypeCustom && ((TypeCustom) type).name.equals("null")) {
            errorTable.addError(varDecl.location, "varType cannot be null");
            return;
        }
        if (globalSYmbolTable.getVar(varDecl.name) != null){
            errorTable.addError(varDecl.location, "varName conflicts with existed globalVar");
            return;
        }
        if (globalSYmbolTable.getCustomType(varDecl.name) != null){
            errorTable.addError(varDecl.location, "varName conflicts with existed class");
            return;
        }
        if (globalSYmbolTable.getFunc(varDecl.name) != null){
            errorTable.addError(varDecl.location, "varName conflicts with existed function");
            return;
        }
        if (varDecl.init != null) {
            globalSYmbolTable.globalinitVarSet.add(varDecl.symbol);
            varDecl.init.accept(this);
        }
        varDecl.symbol = new VarSymbol(varDecl.name, type, varDecl.location, true, false);
        globalSYmbolTable.putVar(varDecl.name, varDecl.symbol);
    }

    private void funcCheckIn(FuncDeclNode funcDecl, CustomTypeSymbol Tclass){
        if (currentSymbolTable.getVar(funcDecl.name) != null){
            errorTable.addError(funcDecl.location, "funcName conflicts with existed var");
            return;
        }
        if (currentSymbolTable.getFunc(funcDecl.name) != null){
            errorTable.addError(funcDecl.location, "funcName conflicts with existed function");
            return;
        }
        funcDecl.symbol = new FuncSymbol();
        funcDecl.symbol.name = Tclass.name + "." + funcDecl.name;
        funcDecl.symbol.location = funcDecl.location;
        funcDecl.symbol.isGlobalFunc = false;
        funcDecl.symbol.funcSymbolTable = null;
        funcDecl.symbol.returnType = decideVarType(funcDecl.retType);
        if (funcDecl.symbol.returnType == null){
            errorTable.addError(funcDecl.retType.location, "can not decide returnType");
            return;
        }
        TypeCustom tc = new TypeCustom(Tclass.name, Tclass);
        funcDecl.symbol.parameterTypeList.add(tc);
        funcDecl.symbol.parameterNameList.add("this");
        for (VarDeclNode x : funcDecl.parameterList){
            TypeType t = decideVarType(x.type);
            if (t == null){
                errorTable.addError(x.location, "can not decide paraType");
                return;
            }
            funcDecl.symbol.parameterTypeList.add(t);
            funcDecl.symbol.parameterNameList.add(x.name);
        }
        currentSymbolTable.putFunc(funcDecl.name, funcDecl.symbol);
    }

    private void classMethodCheckIn(ClassDeclNode classDecl){
        CustomTypeSymbol Tclass = globalSYmbolTable.getCustomType(classDecl.name);
        into(Tclass.classSymbolTable);
        if (classDecl.constructor != null){
            funcCheckIn(classDecl.constructor, Tclass);
        }
        for (FuncDeclNode x : classDecl.methodList){
            funcCheckIn(x, Tclass);
        }
        out();
    }

    private void varCheckIn(VarDeclNode varDecl){
        TypeType type = decideVarType(varDecl.type);
        if (type == null){
            errorTable.addError(varDecl.type.location, "can not decide varType");
            return;
        }
        if (type instanceof TypeBase && ((TypeBase) type).name.equals("void") ){
            errorTable.addError(varDecl.location, "varType cannot be void");
            return;
        }
        if (type instanceof TypeCustom && ((TypeCustom) type).name.equals("null")) {
            errorTable.addError(varDecl.location, "varType cannot be null");
            return;
        }
        if (currentSymbolTable.getVar(varDecl.name) != null){
            errorTable.addError(varDecl.location, "varName conflicts with existed Var");
            return;
        }
        if (currentSymbolTable.getFunc(varDecl.name) != null){
            errorTable.addError(varDecl.location, "varName conflicts with existed function");
            return;
        }
        if (varDecl.init != null){
            varDecl.init.accept(this);
        }
        boolean isGlobal = currentSymbolTable == globalSYmbolTable;
        boolean isClassMember = classOwn.containsKey(currentSymbolTable);
        varDecl.symbol = new VarSymbol(varDecl.name, type, varDecl.location, isGlobal, isClassMember);
        currentSymbolTable.putVar(varDecl.name, varDecl.symbol);
    }

    private void classMemberCheckIn(ClassDeclNode classDecl){
        CustomTypeSymbol Tclass = globalSYmbolTable.getCustomType(classDecl.name);
        into(Tclass.classSymbolTable);
        for (VarDeclNode x : classDecl.memberList)
            varCheckIn(x);
        out();
    }

    private void funcDefine(FuncDeclNode funcDecl, CustomTypeSymbol Tclass){
        currentFunction = currentSymbolTable.getFunc(funcDecl.name);
        currentFunction.funcSymbolTable = new SymbolTable(currentSymbolTable);
        into(currentFunction.funcSymbolTable);
        if (Tclass != null){
            CustomTypeNode c = new CustomTypeNode(Tclass.name);
            VarDeclNode v = new VarDeclNode(c, "this", null);
            varCheckIn(v);
        }
        for (VarDeclNode v : funcDecl.parameterList)
            varCheckIn(v);
        for (StmtNode x : funcDecl.block)
            x.accept(this);
        out();
        currentFunction = null;
    }

    private void classMethodDefine(ClassDeclNode classDecl){
        CustomTypeSymbol Tclass = globalSYmbolTable.getCustomType(classDecl.name);
        into(Tclass.classSymbolTable);
        if (classDecl.constructor != null){
            funcDefine(classDecl.constructor, Tclass);
        }
        for (FuncDeclNode x : classDecl.methodList){
            funcDefine(x, Tclass);
        }
        out();
    }

    private VarSymbol getVar(String name, SymbolTable st){
        VarSymbol v = st.getVar(name);
        if (v != null)
            return v;
        else if(st.Fa != null)
            return getVar(name, st.Fa);
        else return null;
    }

    private FuncSymbol getFunc(String name, SymbolTable st){
        FuncSymbol f = st.getFunc(name);
        if (f != null)
            return f;
        else if (st.Fa != null)
            return getFunc(name, st.Fa);
        else return null;
    }

}
