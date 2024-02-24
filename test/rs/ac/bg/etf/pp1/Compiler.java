package rs.ac.bg.etf.pp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;

import java_cup.runtime.Symbol;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.util.CommonUtils;
import rs.ac.bg.etf.pp1.util.Log4JUtils;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;

public class Compiler {
    private static final Logger log = Logger.getLogger(Compiler.class);

    private static Reader reader = null;
    private static File sourceFile = null;

    private static MJParser parser = null;
    private static Program prog = null;

    static {
        DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
        Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
    }

    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                log.error("Specify command line arguments.");
                return;
            }

            // Print the path to the source file
            String sourceFilePath = args[0];
            String objFilePath = args[1];

            sourceFile = new File(sourceFilePath);
            log.info("Compiling source file: " + sourceFile.getAbsolutePath());

            // Perform lexical and syntax analysis
            lexicalAndSyntaxAnalysis();

            // Initialize symbol table
            CommonUtils.initSymbolTable();

            // Perform semantic analysis and code generation
            semanticAnalysisAndCodeGeneration(objFilePath);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally { // Close the opened input file
            if (reader != null) try {
                reader.close();
            } catch (IOException e1) {
                log.error(e1.getMessage(), e1);
            }
        }
    }

    private static void lexicalAndSyntaxAnalysis() throws Exception {
        // Lexical and syntax analysis
        reader = new BufferedReader(new FileReader(sourceFile));
        Yylex lexer = new Yylex(reader);

        parser = new MJParser(lexer);
        Symbol s = parser.parse();
        prog = (Program) (s.value);
    }

    private static void semanticAnalysisAndCodeGeneration(String objFilePath) throws Exception {
        // Perform semantic analysis
        SemanticAnalyzer semanticAnalyzer = new SemanticAnalyzer();
        log.info("\n\nSemantic analysis:");
        prog.traverseBottomUp(semanticAnalyzer);

        // Print the symbol table
        log.info("===================================");
        Tab.dump();

        // Code generation
        if (!parser.errorDetected && semanticAnalyzer.semanticAnalysisPassed()) {
            // Code is syntactically and semantically correct
            log.info("Parsing has been successfully completed!");

            // Open the object file
            File objFile = new File(objFilePath);
            if (objFile.exists()) objFile.delete();

            // Traverse the tree and generate code into a buffer
            CodeGenerator cg = new CodeGenerator();
            prog.traverseBottomUp(cg);

            // Set values for 'mainPC' and 'dataSize'
            Code.mainPc = cg.getMainPC();
            Code.dataSize = semanticAnalyzer.getNumberOfVariables();

            // Write from buffer to object file
            Code.write(Files.newOutputStream(objFile.toPath()));
            log.info("Code generation successfully completed!");
        } else {
            log.error("Semantic analysis and code generation were NOT successfully completed.");
        }
    }
}
