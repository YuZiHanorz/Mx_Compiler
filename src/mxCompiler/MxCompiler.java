package mxCompiler;

import mxCompiler.Ast.node.ProgramNode;
import mxCompiler.Backend.IRRescanner;
import mxCompiler.Backend.NASMPrinter;
import mxCompiler.Backend.NASMTransformer;
import mxCompiler.Backend.SimpleRegisterAllocator;
import mxCompiler.Frontend.*;
import mxCompiler.IR.node.IRProgram;
import mxCompiler.IR.operand.IRRegister;
import mxCompiler.Parser.MxLexer;
import mxCompiler.Parser.MxParser;
import mxCompiler.Parser.SyntaxErrorListener;
import mxCompiler.Symbol.GlobalSymbolTable;
import mxCompiler.Utility.Configuration;
import mxCompiler.Utility.ErrorTable;
import mxCompiler.Utility.RegCollection;
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
            //Configuration.printAST = true;
            Configuration.printIR = true;
            Configuration.printAsmFile = true;
            input = CharStreams.fromStream(Configuration.fin); //debug
        }

        else {
            input = CharStreams.fromStream(System.in);
        }

        //compile
        ErrorTable errorTable = new ErrorTable();
        RegCollection.build();

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

        IRBuilder irBuilder = new IRBuilder(globalSymbolTable);
        ast.accept(irBuilder);
        IRProgram irProgram = irBuilder.irProgram;

        if (Configuration.printIR){
            System.err.println("------------------------");
            System.err.println("Print IR after building:\n");
            IRPrinter irPrinter = new IRPrinter();
            irPrinter.visit(irProgram);
            irPrinter.printTo(System.err);
        }

        IRRescanner irRescanner = new IRRescanner();
        irProgram.accept(irRescanner);

        if (Configuration.printIR){
            System.err.println("------------------------");
            System.err.println("Print IR after rescanning:\n");
            IRPrinter irPrinter = new IRPrinter();
            irPrinter.visit(irProgram);
            irPrinter.printTo(System.err);
        }

        SimpleRegisterAllocator simpleRegisterAllocator = new SimpleRegisterAllocator(irProgram);
        simpleRegisterAllocator.build();

        if (Configuration.printIR){
            System.err.println("------------------------");
            System.err.println("Print IR after allocation:\n");
            IRPrinter irPrinter = new IRPrinter();
            irPrinter.visit(irProgram);
            irPrinter.printTo(System.err);
        }

        NASMTransformer nasmTransformer = new NASMTransformer(irProgram);
        nasmTransformer.build();

        if (Configuration.printAsmFile){
            System.err.println("------------------------");
            System.err.println("Print nasm finally:\n");
            NASMPrinter nasmPrinter = new NASMPrinter();
            nasmPrinter.visit(irProgram);
            nasmPrinter.printTo(new PrintStream("program.asm"));
        }
        NASMPrinter nasmPrinter = new NASMPrinter();
        nasmPrinter.visit(irProgram);
        nasmPrinter.printTo(System.out);
    }

    /*private static void printHelpInfo(){
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
    }*/

    private static void checkError(ErrorTable errorTable){
        if (errorTable.somethingWrong()){
            errorTable.printTo(err);
            exit(1);
        }
    }


}
