package mxCompiler.Frontend;

import mxCompiler.Ast.AstVisitor;
import mxCompiler.Ast.node.*;

import java.io.PrintStream;

public class AstPrinter implements AstVisitor {
    private StringBuilder indenrStrBuilder;
    private String currentIndent;
    private static final String incIndent = "  ";

    public AstPrinter(){
        indenrStrBuilder = new StringBuilder();
        currentIndent = "";
    }

    public void printTo(PrintStream out){
        out.println(indenrStrBuilder.toString());
    }

    @Override
    public void visit(ProgramNode node){
        indenrStrBuilder.append("Program\n");
        addNewLine("Funcs:");
        indent();
        for (FuncDeclNode x : node.globalFuncList)
            visit(x);
        unindent();

        addNewLine("Classes:");
        indent();
        for (ClassDeclNode x : node.globalClassList)
            visit(x);
        unindent();

        addNewLine("GlobalVars:");
        indent();
        for (VarDeclNode x : node.globalVarList) {
            addNewLine("");
            visit(x);
        }
        unindent();
    }

    @Override
    public void visit(DeclNode node){}

    @Override
    public void visit(FuncDeclNode node){
        addNewLine("func<" + node.name + ">:");
        if (!node.isConstructor){
            addNewLine("returnType: ");
            node.retType.accept(this);
        }
        addNewLine("parameterList:");
        for (VarDeclNode x : node.parameterList){
            visit(x);
            addNewLine(",");
        }
        addNewLine("functionBlock:");
        indent();
        for (StmtNode x : node.block)
            x.accept(this);
        unindent();
    }

    @Override
    public void visit(ClassDeclNode node){
        addNewLine("class<" + node.name + ">:");
        if (node.constructor != null) {
            addNewLine("Constructor:");
            indent();
            visit(node.constructor);
            unindent();
        }
        addNewLine("dataMembers");
        indent();
        for (VarDeclNode x : node.memberList){
            addNewLine("");
            visit(x);
        }
        unindent();
        addNewLine("methods");
        indent();
        for (FuncDeclNode x : node.methodList)
            visit(x);
        unindent();
    }

    @Override
    public void visit(VarDeclNode node){
        if (node.type != null){
            node.type.accept(this);
            addCurrentLine(" ");
        }
        addCurrentLine(node.name);
        if (node.init != null){
            addCurrentLine(" = ");
            node.init.accept(this);
        }
    }

    @Override
    public void visit(VarListNode node){}

    @Override
    public void visit(TypeNode node){}

    @Override
    public void visit(ArrayTypeNode node){
        node.elementType.accept(this);
        for (int i = 0; i < node.dim; ++i)
            addCurrentLine("[]");
    }

    @Override
    public void visit(CustomTypeNode node){
        addCurrentLine(node.typeName);
    }

    @Override
    public void visit(BaseTypeNode node){
        addCurrentLine(node.typeName);
    }

    @Override
    public void visit(StmtNode node){}

    @Override
    public void visit(BlockStmtNode node){
        addNewLine("{");
        indent();
        for (StmtNode x : node.stmtList)
            x.accept(this);
        unindent();
        addNewLine("}");
    }

    @Override
    public void visit(ExprStmtNode node){
        addNewLine("");
        node.expr.accept(this);
    }

    @Override
    public void visit(IfStmtNode node){
        addNewLine("if:");
        indent();
        addNewLine("condition:");
        node.condition.accept(this);
        addNewLine("then:");
        indent();
        node.thenStmt.accept(this);
        unindent();
        if (node.elseStmt != null){
            addNewLine("else");
            indent();
            node.elseStmt.accept(this);
            unindent();
        }
        unindent();
    }
    @Override
    public void visit(WhileStmtNode node){
        addNewLine("while:");
        indent();
        addNewLine("condition:");
        node.condition.accept(this);
        addNewLine("body:");
        indent();
        node.body.accept(this);
        unindent();
        unindent();
    }

    @Override
    public void visit(ForStmtNode node){
        addNewLine("for:");
        indent();

        addNewLine("ïnit:");
        indent();
        if (node.init != null)
            node.init.accept(this);
        unindent();

        addNewLine("condition:");
        indent();
        if (node.condition != null)
            node.condition.accept(this);
        unindent();

        addNewLine("step:");
        indent();
        if (node.step != null)
            node.step.accept(this);
        unindent();

        addNewLine("body");
        indent();
        node.body.accept(this);
        unindent();

        unindent();
    }

    @Override
    public void visit(ContinueStmtNode node){
        addNewLine("continue");
    }

    @Override
    public void visit(BreakStmtNode node){
        addNewLine("break");
    }

    @Override
    public void visit(ReturnStmtNode node){
        addNewLine("return:");
        if (node.retExpr != null)
            node.retExpr.accept(this);
    }

    @Override
    public void visit(VarStmtNode node){
        addNewLine("");
        node.varDecl.accept(this);
    }

    @Override
    public void visit(EmptyStmtNode node){}

    @Override
    public void visit(ExprNode node){}

    @Override
    public void visit(MemberExprNode node){
        node.obj.accept(this);
        addCurrentLine(".");
        if (node.member != null)
            node.member.accept(this);
        else node.method.accept(this);
    }

    @Override
    public void visit(SubscriptExprNode node){
        node.array.accept(this);
        addCurrentLine("[");
        node.subscript.accept(this);
        addCurrentLine("]");
    }

    @Override
    public void visit(FuncCallExprNode node){
        addCurrentLine(node.funcName+"(");
        for (ExprNode x : node.argList){
            x.accept(this);
            addCurrentLine(", ");
        }
        addCurrentLine(")");
    }

    @Override
    public void visit(NewExprNode node){
        addCurrentLine("new");
        node.type.accept(this);
        for (ExprNode x : node.defineSizeList){
            addCurrentLine("[");
            x.accept(this);
            addCurrentLine("]");
        }
        for (int i = 0; i < node.notDefine; ++i)
            addCurrentLine("[]");
    }

    @Override
    public void visit(UnaryExprNode node){
        if (node.uop.contains("x")){
            if (node.uop.charAt(0) == 'x'){
                addCurrentLine("(");
                node.expr.accept(this);
                addCurrentLine(")" + node.uop.substring(1,2));
            }
            else {
                addCurrentLine(node.uop.substring(0,1) + "(");
                node.expr.accept(this);
                addCurrentLine(")");
            }
        }
        else{
            addCurrentLine(node.uop + "(");
            node.expr.accept(this);
            addCurrentLine(")");
        }
    }

    @Override
    public void visit(BinaryExprNode node){
        addCurrentLine("(");
        node.lt.accept(this);
        addCurrentLine(")" + node.bop + "(");
        node.rt.accept(this);
        addCurrentLine(")");
    }

    @Override
    public void visit(AssignExprNode node){
        node.lt.accept(this);
        addCurrentLine(" = (");
        node.rt.accept(this);
        addCurrentLine(")");
    }

    @Override
    public void visit(IdExprNode node){
        addCurrentLine("<id>" + node.name);
    }

    @Override
    public void visit(ConstExprNode node){}

    @Override
    public void visit(ConstIntNode node){
        addCurrentLine(node.valueStr);
    }

    @Override
    public void visit(ConstBoolNode node){
        addCurrentLine(node.valueStr);
    }

    @Override
    public void visit(ConstNullNode node){
        addCurrentLine(node.valueStr);
    }

    @Override
    public void visit(ConstStringNode node){
        addCurrentLine(node.valueStr);
    }


    private void indent(){
        currentIndent = currentIndent + incIndent;
    }

    private void unindent(){
        int len = currentIndent.length() - incIndent.length();
        currentIndent = currentIndent.substring(0, len);
    }

    private void addNewLine(String str){
        indenrStrBuilder.append("\n");
        indenrStrBuilder.append(currentIndent);
        indenrStrBuilder.append(str);
    }

    private void addCurrentLine(String str){
        indenrStrBuilder.append(str);
    }



}
