package mxCompiler;

import mxCompiler.Ast.node.ProgramNode;
import mxCompiler.Frontend.AstBuilder;
import mxCompiler.Frontend.AstPrinter;
import mxCompiler.Frontend.AstScopeChecker;
import mxCompiler.Frontend.SemanticChecker;
import mxCompiler.Parser.MxLexer;
import mxCompiler.Parser.MxParser;
import mxCompiler.Parser.SyntaxErrorListener;
import mxCompiler.Symbol.GlobalSymbolTable;
import mxCompiler.Utility.Configuration;
import mxCompiler.Utility.ErrorTable;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.*;

import static java.lang.System.err;
import static java.lang.System.exit;

public class MxCompiler {

    public static void main(String[] args) throws IOException {
        boolean debug = false;
        CharStream input;
        if (debug){
            String filename = "program.cpp";
            Configuration.fin = new FileInputStream(filename);
            Configuration.printAST = true;
            input = CharStreams.fromStream(Configuration.fin); //debug
        }

        else {
            input = CharStreams.fromStream(System.in);
        }

        //compile
        ErrorTable errorTable = new ErrorTable();

        //build AST
        MxLexer mxLexer = new MxLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(mxLexer);
        MxParser parser = new MxParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new SyntaxErrorListener(errorTable));
        ParseTree tree = parser.program();

        checkError(errorTable);

        AstBuilder astBuilder = new AstBuilder(errorTable);
        ProgramNode ast = (ProgramNode) astBuilder.visit(tree);

        checkError(errorTable);

        if(Configuration.printAST) {
            System.err.println("------------------------");
            System.err.println("Print the AST:\n");
            AstPrinter astPrinter = new AstPrinter();
            astPrinter.visit(ast);
            astPrinter.printTo(System.err);
        }

        AstScopeChecker scopeChecker = new AstScopeChecker(errorTable);
        ast.accept(scopeChecker);

        checkError(errorTable);

        GlobalSymbolTable globalSymbolTable = scopeChecker.globalSYmbolTable;
        SemanticChecker semanticChecker = new SemanticChecker(globalSymbolTable, errorTable);
        ast.accept(semanticChecker);

        checkError(errorTable);
    }

    private static void checkError(ErrorTable errorTable){
        if (errorTable.somethingWrong()){
            errorTable.printTo(err);
            exit(1);
        }
    }


}
