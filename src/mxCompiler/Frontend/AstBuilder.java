package mxCompiler.Frontend;

import mxCompiler.Ast.node.*;
import mxCompiler.Parser.MxBaseVisitor;
import mxCompiler.Parser.MxParser;
import mxCompiler.Utility.ErrorTable;
import mxCompiler.Utility.Location;
import org.antlr.v4.runtime.ParserRuleContext;

public class AstBuilder extends MxBaseVisitor<Node>{
    public ProgramNode astRoot;
    public ErrorTable errorTable;

    public AstBuilder(ErrorTable errorTable){
        this.astRoot = new ProgramNode();
        this.astRoot.location = new Location(0,0);
        this.errorTable = errorTable;
    }

    @Override
    public Node visitProgram(MxParser.ProgramContext ctx){
        if (ctx.declaration() != null) {
            for (ParserRuleContext x : ctx.declaration()) {
                Node decl = visit(x);
                if (decl instanceof VarListNode)
                    astRoot.addAll(((VarListNode) decl).varList);
                else if (decl instanceof FuncDeclNode)
                    astRoot.add((FuncDeclNode) decl);
                else if (decl instanceof ClassDeclNode)
                    astRoot.add((ClassDeclNode) decl);
            }
        }
        return astRoot;
    }

    @Override
    public Node visitDeclaration(MxParser.DeclarationContext ctx){
        if (ctx.classDeclaration() != null)
            return visit(ctx.classDeclaration());
        else if (ctx.functionDeclaration() != null)
            return visit(ctx.functionDeclaration());
        else if (ctx.variableDeclaration() != null)
            return visit(ctx.variableDeclaration());
        else throw new Error("invalid Declaration");
    }

    @Override
    public Node visitFunctionDeclaration(MxParser.FunctionDeclarationContext ctx){
        FuncDeclNode funcDecl = new FuncDeclNode();
        funcDecl.name = ctx.IDENTIFIER().getSymbol().getText();
        funcDecl.location = new Location(ctx);
        funcDecl.isConstructor = false;
        funcDecl.retType = (TypeNode) visit(ctx.typeType());

        if (ctx.parameterDeclarationList() != null) {
            VarListNode paraList = (VarListNode) visit(ctx.parameterDeclarationList());
            funcDecl.parameterList.addAll(paraList.varList);
        }
        BlockStmtNode body = (BlockStmtNode) visit(ctx.block());
        funcDecl.block.addAll(body.stmtList);
        return funcDecl;
    }

    @Override
    public Node visitClassDeclaration(MxParser.ClassDeclarationContext ctx){
        ClassDeclNode classDecl = new ClassDeclNode();
        classDecl.name = ctx.IDENTIFIER().getSymbol().getText();
        classDecl.location = new Location(ctx);

        if (ctx.memberDeclaration() != null) {
            for (ParserRuleContext x : ctx.memberDeclaration()) {
                Node memDecl = visit(x);
                if (memDecl instanceof FuncDeclNode) {
                    if (((FuncDeclNode) memDecl).isConstructor) {
                        if (classDecl.constructor == null)
                            classDecl.constructor = (FuncDeclNode) memDecl;
                        else errorTable.addError(new Location(x), "Class can have only one constructor");
                    } else {
                        if (((FuncDeclNode) memDecl).name.equals(classDecl.name))
                            errorTable.addError(new Location(x), "Constructor can not have returnType");
                        else classDecl.methodList.add((FuncDeclNode) memDecl);
                    }
                } else if (memDecl instanceof VarListNode)
                    classDecl.memberList.addAll(((VarListNode) memDecl).varList);
                else throw new Error("invalid ClassDeclaration");
            }
        }
        return classDecl;
    }

    @Override
    public Node visitVariableDeclaration(MxParser.VariableDeclarationContext ctx){
        TypeNode type = (TypeNode) visit(ctx.typeType());
        VarListNode varList = (VarListNode) visit(ctx.variableDeclarationList());
        if (varList.varList != null) {
            for (VarDeclNode x : varList.varList)
                x.type = type;
        }
        return varList;
    }

    @Override
    public Node visitTypeType(MxParser.TypeTypeContext ctx){
        if (ctx.dim().isEmpty()){
            TypeNode type = (TypeNode) visit(ctx.type());
            return type;
        }
        else {
            ArrayTypeNode arrayType = new ArrayTypeNode();
            arrayType.location = new Location(ctx);
            arrayType.elementType = (TypeNode) visit(ctx.type());
            arrayType.dim = ctx.dim().size();
            return arrayType;
        }
    }

    @Override
    public Node visitType(MxParser.TypeContext ctx){
        String name = ctx.token.getText();
        if (name.equals("int") || name.equals("void") || name.equals("bool")){
            BaseTypeNode baseType = new BaseTypeNode(name);
            baseType.location = new Location(ctx);
            return baseType;
        }
        else {
            CustomTypeNode customType = new CustomTypeNode(name);
            customType.location = new Location(ctx);
            return customType;
        }
    }

    @Override
    public Node visitParameterDeclarationList(MxParser.ParameterDeclarationListContext ctx){
        VarListNode paraList = new VarListNode();
        paraList.location = new Location(ctx);
        if (ctx.parameterDeclaration() != null) {
            for (ParserRuleContext x : ctx.parameterDeclaration()) {
                VarDeclNode para = (VarDeclNode) visit(x);
                paraList.varList.add(para);
            }
        }
        return paraList;
    }

    @Override
    public Node visitParameterDeclaration(MxParser.ParameterDeclarationContext ctx){
        VarDeclNode varDecl = new VarDeclNode();
        varDecl.type = (TypeNode) visit(ctx.typeType());
        varDecl.name = ctx.IDENTIFIER().getSymbol().getText();
        varDecl.location = new Location(ctx);
        return varDecl;
    }

    @Override
    public Node visitVariableDeclarationList(MxParser.VariableDeclarationListContext ctx){
        VarListNode varList = new VarListNode();
        varList.location = new Location(ctx);
        if (ctx.variableDeclarator() != null) {
            for (ParserRuleContext x : ctx.variableDeclarator()) {
                VarDeclNode varDecl = (VarDeclNode) visit(x);
                varList.varList.add(varDecl);
            }
        }
        return varList;
    }

    @Override
    public Node visitVariableDeclarator(MxParser.VariableDeclaratorContext ctx){
        VarDeclNode varDecl = new VarDeclNode();
        varDecl.name = ctx.IDENTIFIER().getSymbol().getText();
        varDecl.location = new Location(ctx);
        if (ctx.expr() != null)
            varDecl.init = (ExprNode) visit(ctx.expr());
        return varDecl;
    }

    @Override
    public Node visitMemberDeclaration(MxParser.MemberDeclarationContext ctx){
        if (ctx.constructorDeclaration() != null)
            return visit(ctx.constructorDeclaration());
        else if (ctx.functionDeclaration() != null)
            return visit(ctx.functionDeclaration());
        else if (ctx.variableDeclaration() != null)
            return visit(ctx.variableDeclaration());
        else throw new Error("Invalid memberDeclaration");
    }

    @Override
    public Node visitConstructorDeclaration(MxParser.ConstructorDeclarationContext ctx){
        FuncDeclNode funcDecl = new FuncDeclNode();
        funcDecl.name = ctx.IDENTIFIER().getSymbol().getText();
        funcDecl.location = new Location(ctx);
        funcDecl.isConstructor = true;
        funcDecl.retType = new BaseTypeNode("void");

        if (ctx.parameterDeclarationList() != null) {
            VarListNode paraList = (VarListNode) visit(ctx.parameterDeclarationList());
            funcDecl.parameterList.addAll(paraList.varList);
        }
        BlockStmtNode body = (BlockStmtNode) visit(ctx.block());
        funcDecl.block.addAll(body.stmtList);
        return funcDecl;
    }

    @Override
    public Node visitBlock(MxParser.BlockContext ctx){
        return visit(ctx.statementList());
    }

    @Override
    public Node visitStatementList(MxParser.StatementListContext ctx){
        BlockStmtNode block = new BlockStmtNode();
        block.location = new Location(ctx);
        if (ctx.statement() != null){
            for (ParserRuleContext x : ctx.statement()){
                Node stmt = visit(x);
                if (x instanceof MxParser.VariableStatementContext)
                    block.stmtList.addAll(((BlockStmtNode) stmt).stmtList);
                else block.stmtList.add((StmtNode)stmt);
            }
        }
        return block;
    }

    @Override
    public Node visitBlockStatement(MxParser.BlockStatementContext ctx){
        return visit(ctx.block());
    }

    @Override
    public Node visitExprStatement(MxParser.ExprStatementContext ctx){
        ExprStmtNode expr = new ExprStmtNode();
        expr.location = new Location(ctx);
        expr.expr = (ExprNode) visit(ctx.expr());
        return expr;
    }

    @Override
    public Node visitIfStatement(MxParser.IfStatementContext ctx){
        IfStmtNode ifStmt = new IfStmtNode();
        ifStmt.location = new Location(ctx);
        ifStmt.condition = (ExprNode) visit(ctx.expr());
        ifStmt.thenStmt = (StmtNode) visit(ctx.thenStmt);
        if (ctx.elseStmt != null)
            ifStmt.elseStmt = (StmtNode) visit(ctx.elseStmt);
        return ifStmt;
    }

    @Override
    public Node visitWhileStatement(MxParser.WhileStatementContext ctx){
        WhileStmtNode whileStmt = new WhileStmtNode();
        whileStmt.location = new Location(ctx);
        whileStmt.condition = (ExprNode) visit(ctx.expr());
        whileStmt.body = (StmtNode) visit(ctx.statement());
        return whileStmt;
    }

    @Override
    public Node visitForStatement(MxParser.ForStatementContext ctx){
        ForStmtNode forStmt = new ForStmtNode();
        forStmt.location = new Location(ctx);
        if (ctx.forInit != null)
            forStmt.init = (ExprNode) visit(ctx.forInit);
        if (ctx.forCondition != null)
            forStmt.condition = (ExprNode) visit(ctx.forCondition);
        if (ctx.forStep != null)
            forStmt.step = (ExprNode) visit(ctx.forStep);
        forStmt.body = (StmtNode) visit(ctx.statement());
        return forStmt;
    }

    @Override
    public Node visitContinueStatement(MxParser.ContinueStatementContext ctx){
        ContinueStmtNode con = new ContinueStmtNode();
        con.location = new Location(ctx);
        return con;
    }

    @Override
    public Node visitBreakStatement(MxParser.BreakStatementContext ctx){
        BreakStmtNode br = new BreakStmtNode();
        br.location = new Location(ctx);
        return br;
    }

    @Override
    public Node visitReturnStatement(MxParser.ReturnStatementContext ctx){
        ReturnStmtNode ret = new ReturnStmtNode();
        ret.location = new Location(ctx);
        if (ctx.expr() != null)
            ret.retExpr = (ExprNode) visit(ctx.expr());
        return ret;
    }

    @Override
    public Node visitVariableStatement(MxParser.VariableStatementContext ctx){
        BlockStmtNode varStmtList = new BlockStmtNode();
        varStmtList.location = new Location(ctx);
        VarListNode varList = (VarListNode) visit(ctx.variableDeclaration());
        for (VarDeclNode v : varList.varList){
            VarStmtNode vstmt = new VarStmtNode();
            vstmt.location = new Location(v);
            vstmt.varDecl = v;
            varStmtList.stmtList.add(vstmt);
        }
        return varStmtList;
    }

    @Override
    public Node visitEmptyStatement(MxParser.EmptyStatementContext ctx){
        EmptyStmtNode emp = new EmptyStmtNode();
        emp.location = new Location(ctx);
        return emp;
    }

    @Override
    public Node visitNewExpr(MxParser.NewExprContext ctx){
        return visit(ctx.creator());
    }

    @Override
    public Node visitUnaryExpr(MxParser.UnaryExprContext ctx){
        UnaryExprNode expr = new UnaryExprNode();
        expr.location = new Location(ctx);
        if (ctx.suffix != null)
            expr.uop = ctx.suffix.getText().equals("++")? "x++" : "x--";
        else if (ctx.prefix.getText().equals("++"))
            expr.uop = "++x";
        else if (ctx.prefix.getText().equals("--"))
            expr.uop = "--x";
        else expr.uop = ctx.prefix.getText();
        expr.expr = (ExprNode) visit(ctx.expr());
        return expr;
    }

    @Override
    public Node visitPrimaryExpr(MxParser.PrimaryExprContext ctx){
        return visit(ctx.primaryExpression());
    }

    @Override
    public Node visitSubscriptExpr(MxParser.SubscriptExprContext ctx){
        SubscriptExprNode expr = new SubscriptExprNode();
        expr.location = new Location(ctx);
        expr.array = (ExprNode) visit(ctx.expr(0));
        expr.subscript = (ExprNode) visit(ctx.expr(1));
        return expr;
    }

    @Override
    public Node visitMemberExpr(MxParser.MemberExprContext ctx){
        MemberExprNode expr = new MemberExprNode();
        expr.location = new Location(ctx);
        expr.obj = (ExprNode) visit(ctx.expr());
        if (ctx.IDENTIFIER() != null)
            expr.member = new IdExprNode(ctx.IDENTIFIER().getSymbol());
        else expr.method = (FuncCallExprNode) visit(ctx.functionCall());
        return expr;
    }

    @Override
    public Node visitBinaryExpr(MxParser.BinaryExprContext ctx){
        BinaryExprNode expr = new BinaryExprNode();
        expr.location = new Location(ctx);
        expr.bop = ctx.bop.getText();
        expr.lt = (ExprNode) visit(ctx.expr(0));
        expr.rt = (ExprNode) visit(ctx.expr(1));
        return expr;
    }

    @Override
    public Node visitFuncCallExpr(MxParser.FuncCallExprContext ctx){
        return visit(ctx.functionCall());
    }

    @Override
    public Node visitAssignExpr(MxParser.AssignExprContext ctx){
        AssignExprNode expr = new AssignExprNode();
        expr.location = new Location(ctx);
        expr.lt = (ExprNode) visit(ctx.expr(0));
        expr.rt = (ExprNode) visit(ctx.expr(1));
        return expr;
    }

    @Override
    public Node visitPrimaryExpression(MxParser.PrimaryExpressionContext ctx){
        if (ctx.token == null)
            return visit(ctx.expr());
        else if (ctx.token.getType() == MxParser.THIS || ctx.token.getType() == MxParser.IDENTIFIER)
            return new IdExprNode(ctx.token);
        else if (ctx.token.getType() == MxParser.IntLiteral)
            return new ConstIntNode(ctx.token);
        else if (ctx.token.getType() == MxParser.StringLiteral)
            return new ConstStringNode(ctx.token);
        else if (ctx.token.getType() == MxParser.BoolLiteral)
            return new ConstBoolNode(ctx.token);
        else if (ctx.token.getType() == MxParser.NullLiteral)
            return new ConstNullNode(ctx.token);
        else
            throw new Error("cannot resolve primaryExpr");
    }

    @Override
    public Node visitFunctionCall(MxParser.FunctionCallContext ctx){
        FuncCallExprNode expr = new FuncCallExprNode();
        expr.location = new Location(ctx);
        expr.funcName = ctx.IDENTIFIER().getSymbol().getText();
        if (ctx.parameterList() != null && ctx.parameterList().expr() != null){
            for (ParserRuleContext x : ctx.parameterList().expr()){
                expr.argList.add((ExprNode) visit(x));
            }
        }
        return expr;
    }

    //not used
    @Override
    public Node visitParameterList(MxParser.ParameterListContext ctx){
        return super.visitParameterList(ctx);
    }

    @Override
    public Node visitCreator(MxParser.CreatorContext ctx){
        NewExprNode expr = new NewExprNode();
        expr.location = new Location(ctx);
        expr.type = (TypeNode) visit(ctx.type());
        if (ctx.expr() != null){
            for (ParserRuleContext x : ctx.expr()){
                ExprNode e = (ExprNode) visit(x);
                expr.defineSizeList.add(e);
            }
        }
        if (ctx.dim() != null)
            expr.notDefine = ctx.dim().size();
        else
            expr.notDefine = 0;
        return expr;
    }

    //not used
    @Override
    public Node visitDim(MxParser.DimContext ctx){
        return super.visitDim(ctx);
    }

}
