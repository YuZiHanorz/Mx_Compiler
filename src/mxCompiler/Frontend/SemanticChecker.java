package mxCompiler.Frontend;

import mxCompiler.Ast.AstVisitor;
import mxCompiler.Ast.node.*;
import mxCompiler.Symbol.BaseTypeSymbol;
import mxCompiler.Symbol.FuncSymbol;
import mxCompiler.Symbol.GlobalSymbolTable;
import mxCompiler.Type.TypeArray;
import mxCompiler.Type.TypeBase;
import mxCompiler.Type.TypeCustom;
import mxCompiler.Type.TypeType;
import mxCompiler.Utility.ErrorTable;

public class SemanticChecker implements AstVisitor {
    public ErrorTable errorTable;
    public GlobalSymbolTable globalSymbolTable;
    public FuncSymbol currentFunc = null;
    int loop = 0;

    public SemanticChecker(GlobalSymbolTable g, ErrorTable e){
        this.errorTable = e;
        this.globalSymbolTable = g;
    }

    @Override
    public void visit(ProgramNode node){
        for (FuncDeclNode f : node.globalFuncList)
            f.accept(this);
        for (ClassDeclNode c : node.globalClassList)
            c.accept(this);
        for (VarDeclNode v : node.globalVarList)
            v.accept(this);
        FuncSymbol mainFunc = globalSymbolTable.getFunc("main");
        if (mainFunc == null){
            errorTable.addError(node.location, "loss of <main>");
            return;
        }
        if (mainFunc.returnType instanceof TypeBase){
            if (((TypeBase) mainFunc.returnType).name.equals("int")){
                if (mainFunc.parameterNameList.size() > 0)
                    errorTable.addError(mainFunc.location, "main should not have parameters");
            }
            else errorTable.addError(mainFunc.location, "main should return Type<int>");
        }
        else errorTable.addError(mainFunc.location, "main should return Type<int>");
    }

    @Override
    public void visit(DeclNode node){}

    @Override
    public void visit(FuncDeclNode node){
        currentFunc = node.symbol;
        for (StmtNode x : node.block)
            x.accept(this);
    }

    @Override
    public void visit(ClassDeclNode node){
        for (FuncDeclNode x : node.methodList)
            x.accept(this);
        if (node.constructor != null){
            if (node.constructor.name.equals(node.name))
                node.constructor.accept(this);
            else errorTable.addError(node.constructor.location, "Constructor name should be " + node.name);
        }
    }

    @Override
    public void visit(VarDeclNode node){
        if (node.init != null){
            if (node.symbol.type.match(node.init.calcType))
                node.init.accept(this);
            else errorTable.addError(node.init.location, "varInitType conflicts with varDecl");
        }
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
    public void visit(IfStmtNode node){
        node.condition.accept(this);
        if (ConditionIsNotBool(node.condition))
            return;
        node.thenStmt.accept(this);
        if (node.elseStmt != null)
            node.elseStmt.accept(this);
    }

    @Override
    public void visit(WhileStmtNode node){
        node.condition.accept(this);
        if (ConditionIsNotBool(node.condition))
            return;
        ++loop;
        node.body.accept(this);
        --loop;
    }

    @Override
    public void visit(ForStmtNode node){
        if (node.condition != null){
            node.condition.accept(this);
            if (ConditionIsNotBool(node.condition))
                return;
        }
        if (node.init != null)
            node.init.accept(this);
        if (node.step != null)
            node.step.accept(this);
        ++loop;
        node.body.accept(this);
        --loop;
    }

    @Override
    public void visit(ContinueStmtNode node){
        if (loop == 0)
            errorTable.addError(node.location, "you want to continue what?");
    }

    @Override
    public void visit(BreakStmtNode node){
        if (loop == 0)
            errorTable.addError(node.location, "you want to break what?");
    }

    @Override
    public void visit(ReturnStmtNode node){
        TypeType req = currentFunc.returnType;
        TypeBase Tvoid = new TypeBase("void", globalSymbolTable.getBaseType("void"));
        if (req.match(Tvoid) && node.retExpr != null){
            errorTable.addError(node.location, "how can you return a void value?");
            return;
        }
        TypeType ret = node.retExpr == null? Tvoid : node.retExpr.calcType;
        if (!req.match(ret))
            errorTable.addError(node.location, "invalid returnType");
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
        if (node.obj.calcType instanceof TypeArray)
            node.isLvalue = false; //array.size()
        else if (node.method != null){
            node.method.accept(this);
            node.isLvalue = node.method.isLvalue;
        }
        else node.isLvalue = true; //dataMember
    }

    @Override
    public void visit(SubscriptExprNode node){
        node.array.accept(this);
        node.subscript.accept(this);
        node.isLvalue = true;
    }

    @Override
    public void visit(FuncCallExprNode node){
        int reqParaNum = node.symbol.parameterNameList.size();
        boolean pThis = node.symbol.parameterNameList.size()>0 && node.symbol.parameterNameList.get(0).equals("this");
        int paraNum = pThis? node.argList.size() + 1 : node.argList.size();
        if (paraNum != reqParaNum){
            errorTable.addError(node.location, "invalid parameterListSize");
            return;
        }
        for (int i = 0; i < node.argList.size(); ++i){
            node.argList.get(i).accept(this);
            if (pThis){
                if (!node.argList.get(i).calcType.match(node.symbol.parameterTypeList.get(i+1))){
                    errorTable.addError(node.argList.get(i).location, "invalid arg");
                    return;
                }
            }
            else {
                if (!node.argList.get(i).calcType.match(node.symbol.parameterTypeList.get(i))){
                    errorTable.addError(node.argList.get(i).location, "invalid arg");
                    return;
                }
            }
        }
        node.isLvalue = false;
    }

    @Override
    public void visit(NewExprNode node){
        for (ExprNode x : node.defineSizeList)
            x.accept(this);
        node.isLvalue = true;
    }

    @Override
    public void visit(UnaryExprNode node){
        node.expr.accept(this);
        boolean typeConflict = false;
        boolean alterConflict = false;
        switch (node.uop) {
            case "x++":
            case "x--":
                if (!node.expr.isLvalue)
                    alterConflict = true;
                if (TypeIsNotInt(node.expr.calcType))
                    typeConflict = true;
                node.isLvalue = false;
                break;

            case "++x":
            case "--x":
                if (!node.expr.isLvalue)
                    alterConflict = true;
                if (TypeIsNotInt(node.expr.calcType))
                    typeConflict = true;
                node.isLvalue = true; //lvalue
                break;

            case "+":
            case "-":
                if (TypeIsNotInt(node.expr.calcType))
                    typeConflict = true;
                node.isLvalue = false;
                break;

            case "!":
                if (TypeIsNotBool(node.expr.calcType))
                    typeConflict = true;
                node.isLvalue = false;
                break;

            case "~":
                if (TypeIsNotInt(node.expr.calcType))
                    typeConflict = true;
                node.isLvalue = false;
                break;

            default:
        }

        if (alterConflict)
            errorTable.addError(node.location, "the expr cannot be altered");
        if (typeConflict)
            errorTable.addError(node.location, "invalid type to do the operation");
    }

    @Override
    public void visit(BinaryExprNode node){
        node.lt.accept(this);
        node.rt.accept(this);
        if (!node.lt.calcType.match(node.rt.calcType)){
            errorTable.addError(node.location, "ltType conflict with rtType for the Bop");
            return;
        }
        switch (node.bop) {
            case "-":
            case "*":
            case "/":
            case "%":
            case "<<":
            case ">>":
            case "&":
            case "^":
            case "|":
                if (TypeIsNotInt(node.lt.calcType)) {
                    errorTable.addError(node.location, "invalid type for the Bop");
                    return;
                }
                break;

            case "+":
            case ">":
            case "<":
            case ">=":
            case "<=":
                if (TypeIsNotInt(node.lt.calcType) && TypeIsNotString(node.lt.calcType)) {
                    errorTable.addError(node.location, "invalid type for the Bop");
                    return;
                }
                break;

            case "&&":
            case "||":
                if (TypeIsNotBool(node.lt.calcType)) {
                    errorTable.addError(node.location, "invalid type for the Bop");
                    return;
                }
                break;

            default:
        }
        node.isLvalue = false;
    }

    @Override
    public void visit(AssignExprNode node){
        node.lt.accept(this);
        node.rt.accept(this);
        if (!node.lt.isLvalue){
            errorTable.addError(node.location, "lt to be assigned is not lvalue");
            return;
        }
        if (!node.lt.calcType.match(node.rt.calcType)){
            errorTable.addError(node.location, "ltType conflicts with rtType in Assign");
            return;
        }
        node.isLvalue = false;
    }

    @Override
    public void visit(IdExprNode node){
        if (node.name.equals("this"))
            node.isLvalue = false;
        else node.isLvalue = true;
    }

    @Override
    public void visit(ConstExprNode node){}

    @Override
    public void visit(ConstIntNode node){
        node.isLvalue = false;
    }

    @Override
    public void visit(ConstBoolNode node){
        node.isLvalue = false;
    }

    @Override
    public void visit(ConstNullNode node){
        node.isLvalue = false;
    }

    @Override
    public void visit(ConstStringNode node){
        node.isLvalue = false;
    }

    private boolean ConditionIsNotBool(ExprNode expr){
        if (expr.calcType instanceof TypeBase) {
            if (((TypeBase) expr.calcType).name.equals("bool"))
                return false;
        }
        errorTable.addError(expr.location, "conditionType must be bool");
        return true;
    }

    private boolean TypeIsNotInt(TypeType type){
        return !(type instanceof TypeBase && ((TypeBase) type).name.equals("int"));
    }

    private boolean TypeIsNotBool(TypeType type){
        return !(type instanceof TypeBase && ((TypeBase) type).name.equals("bool"));
    }

    private boolean TypeIsNotString(TypeType type){
        return !(type instanceof TypeCustom && ((TypeCustom) type).name.equals("string"));
    }

}
