package mxCompiler.Frontend;

import mxCompiler.Ast.AstVisitor;
import mxCompiler.Ast.node.*;

public class ConstFolderOpt implements AstVisitor {
    @Override
    public void visit(ProgramNode node){
        for (FuncDeclNode f : node.globalFuncList)
            f.accept(this);
        for (ClassDeclNode c : node.globalClassList)
            c.accept(this);
        for (VarDeclNode v : node.globalVarList)
            v.accept(this);
    }

    @Override
    public void visit(DeclNode node){}

    @Override
    public void visit(FuncDeclNode node){
        for (VarDeclNode v : node.parameterList)
            v.accept(this);
        for (StmtNode s : node.block)
            s.accept(this);
    }

    @Override
    public void visit(ClassDeclNode node){
        if (node.constructor != null)
            node.constructor.accept(this);
        for (VarDeclNode v : node.memberList)
            v.accept(this);
        for (FuncDeclNode f : node.methodList)
            f.accept(this);
    }

    @Override
    public void visit(VarDeclNode node){
        if (node.init != null){
            node.init.accept(this);
            if (node.init.toConst){
                int val = node.init.constVal;
                node.init = new ConstIntNode();
                ((ConstIntNode) node.init).valueStr = String.valueOf(val);
            }
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
        for (StmtNode s: node.stmtList)
            s.accept(this);
    }

    @Override
    public void visit(ExprStmtNode node){
        node.expr.accept(this);
        if (node.expr.toConst){
            int val = node.expr.constVal;
            node.expr = new ConstIntNode();
            ((ConstIntNode) node.expr).valueStr = String.valueOf(val);
        }
    }

    @Override
    public void visit(IfStmtNode node){
        node.condition.accept(this);
        if (node.condition.toConst){
            int val = node.condition.constVal;
            node.condition = new ConstIntNode();
            ((ConstIntNode) node.condition).valueStr = String.valueOf(val);
        }
        node.thenStmt.accept(this);
        if (node.elseStmt != null)
            node.elseStmt.accept(this);
    }
    @Override
    public void visit(WhileStmtNode node){
        node.condition.accept(this);
        if (node.condition.toConst){
            int val = node.condition.constVal;
            node.condition = new ConstIntNode();
            ((ConstIntNode) node.condition).valueStr = String.valueOf(val);
        }
        if (node.body != null)
            node.body.accept(this);
    }

    @Override
    public void visit(ForStmtNode node){
        if (node.init != null) {
            node.init.accept(this);
            if (node.init.toConst){
                int val = node.init.constVal;
                node.init = new ConstIntNode();
                ((ConstIntNode) node.init).valueStr = String.valueOf(val);
            }
        }
        if (node.condition != null) {
            node.condition.accept(this);
            if (node.condition.toConst){
                int val = node.condition.constVal;
                node.condition = new ConstIntNode();
                ((ConstIntNode) node.condition).valueStr = String.valueOf(val);
            }
        }
        if (node.step != null) {
            node.step.accept(this);
            if (node.step.toConst){
                int val = node.step.constVal;
                node.step = new ConstIntNode();
                ((ConstIntNode) node.step).valueStr = String.valueOf(val);
            }
        }
        if (node.body != null)
            node.body.accept(this);
    }

    @Override
    public void visit(ContinueStmtNode node){}

    @Override
    public void visit(BreakStmtNode node){}

    @Override
    public void visit(ReturnStmtNode node){
        if (node.retExpr != null) {
            node.retExpr.accept(this);
            if (node.retExpr.toConst){
                int val = node.retExpr.constVal;
                node.retExpr = new ConstIntNode();
                ((ConstIntNode) node.retExpr).valueStr = String.valueOf(val);
            }
        }
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
        if (node.method != null)
            node.method.accept(this);
    }

    @Override
    public void visit(SubscriptExprNode node){
        node.subscript.accept(this);
        if (node.subscript.toConst){
            int val = node.subscript.constVal;
            node.subscript = new ConstIntNode();
            ((ConstIntNode) node.subscript).valueStr = String.valueOf(val);
        }
    }

    @Override
    public void visit(FuncCallExprNode node){
        for (ExprNode e : node.argList){
            e.accept(this);
            if (e.toConst){
                int val = e.constVal;
                e = new ConstIntNode();
                ((ConstIntNode) e).valueStr = String.valueOf(val);
            }
        }
    }

    @Override
    public void visit(NewExprNode node){
        for (ExprNode e : node.defineSizeList) {
            e.accept(this);
            if (e.toConst){
                int val = e.constVal;
                e = new ConstIntNode();
                ((ConstIntNode) e).valueStr = String.valueOf(val);
            }
        }
    }

    @Override
    public void visit(UnaryExprNode node){
        node.expr.accept(this);
        if (node.expr.toConst){
            int val = node.expr.constVal;
            node.expr = new ConstIntNode();
            ((ConstIntNode) node.expr).valueStr = String.valueOf(val);
        }
    }

    @Override
    public void visit(BinaryExprNode node){
        node.lt.accept(this);
        if (node.lt.toConst){
            int val = node.lt.constVal;
            node.lt = new ConstIntNode();
            ((ConstIntNode) node.lt).valueStr = String.valueOf(val);
        }
        node.rt.accept(this);
        if (node.rt.toConst){
            int val = node.rt.constVal;
            node.rt = new ConstIntNode();
            ((ConstIntNode) node.rt).valueStr = String.valueOf(val);
        }
        if (node.lt instanceof ConstIntNode && node.rt instanceof ConstIntNode){
            int lt = Integer.valueOf(((ConstIntNode) node.lt).valueStr);
            int rt = Integer.valueOf(((ConstIntNode) node.rt).valueStr);
            switch (node.bop){
                case "&&":
                case "||":
                    break;
                case "*":
                    node.constVal = lt * rt;
                    node.toConst = true;
                    break;
                case "/":
                    node.constVal = lt / rt;
                    node.toConst = true;
                    break;
                case "%":
                    node.constVal = lt % rt;
                    node.toConst = true;
                    break;
                case "+":
                    node.constVal = lt + rt;
                    node.toConst = true;
                    break;
                case "-":
                    node.constVal = lt - rt;
                    node.toConst = true;
                    break;
                case ">>":
                    node.constVal = lt >> rt;
                    node.toConst =true;
                    break;
                case "<<":
                    node.constVal = lt << rt;
                    node.toConst = true;
                    break;
                case "&":
                    node.constVal = lt & rt;
                    node.toConst = true;
                    break;
                case "|":
                    node.constVal = lt | rt;
                    node.toConst = true;
                    break;
                case "^":
                    node.constVal = lt ^ rt;
                    node.toConst = true;
                    break;
                case "<":
                case ">":
                case ">=":
                case "<=":
                case "==":
                case "!=":
                    break;
            }
        }

    }

    @Override
    public void visit(AssignExprNode node){
        if (node.rt != null) {
            node.rt.accept(this);
            if (node.rt.toConst){
                int val = node.rt.constVal;
                node.rt = new ConstIntNode();
                ((ConstIntNode) node.rt).valueStr = String.valueOf(val);
            }
        }
    }

    @Override
    public void visit(IdExprNode node){}

    @Override
    public void visit(ConstExprNode node){}

    @Override
    public void visit(ConstIntNode node){}

    @Override
    public void visit(ConstBoolNode node){}

    @Override
    public void visit(ConstNullNode node){}

    @Override
    public void visit(ConstStringNode node){}
}
