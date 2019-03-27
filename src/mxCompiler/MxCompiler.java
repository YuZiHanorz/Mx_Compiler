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
        try{
            String input = "program.cpp";
            for (int i = 0; i < args.length; ++i){
                String arg = args[i];
                switch (arg) {
                    case "--printAST":
                        Configuration.printAST = true;
                        break;
                    case "-o":
                        if (i < args.length - 1){
                            ++i;
                            arg = args[i];
                            Configuration.fout = new PrintStream(arg);
                        }
                        else needHelp();
                        break;
                    default:
                        if(Configuration.fin == null){
                            input = arg;
                            Configuration.fin = new FileInputStream(arg);
                        }
                        else needHelp();
                        break;
                }
            }
            if (Configuration.fin == null)
                Configuration.fin = new FileInputStream(input);
            if (Configuration.fout == null)
                Configuration.fout = new PrintStream(output(input));
            Configuration.printAST = true;
        }
        catch (FileNotFoundException err){
            System.out.println(err.toString());
            exit(0);
        }
        
        //compile
        ErrorTable errorTable = new ErrorTable();

        //build AST
        CharStream input = CharStreams.fromStream(Configuration.fin);
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
    
    private static void printHelpInfo(){
        System.out.println("This is a uncompleted, somewhat silly compiler for Mx* Language\n");
        System.out.println("\tUsage:  Mx_Compiler [--printAST] [source] [-o file]");
        System.out.println("\tSource default is program.cpp");
        System.out.println("\toutput default is [inputName].asm");
        System.out.println("\t--printAST print the abstract syntax tree");
    }

    private static void needHelp(){
        System.out.println("It seems that you need some little help\n");
        printHelpInfo();
        exit(0);
    }

    private static String output(String input){
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < input.length(); ++i){
            str.append(input.charAt(i));
            if (input.charAt(i) == '.') break;
        }
        str.append("asm");
        return str.toString();
    }

    private static void checkError(ErrorTable errorTable){
        if (errorTable.somethingWrong()){
            errorTable.printTo(err);
            exit(1);
        }
    }


}
