package rs.ac.bg.etf.pp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import java_cup.runtime.Symbol;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.util.Log4JUtils;

public class MJParserTest {
    /* Configures log4j */
    static {
        DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
        Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
    }

    public static void main(String[] args) {
        Logger log = Logger.getLogger(MJLexerTest.class);
        Reader br = null;

        /* Reads the input file */
        try {
            /* The input file is: 'test/program.mj' */
            File sourceCode = new File("test/test301.mj");

            /* Prints the path to the source file */
            log.info("Translating file with source code: " + sourceCode.getAbsolutePath());

            /* Creates a lexer */
            br = new BufferedReader(new FileReader(sourceCode));
            Yylex lexer = new Yylex(br);

            MJParser parser = new MJParser(lexer);
            Symbol s = parser.parse();
            Program prog = (Program) (s.value);

            // Prints the syntax tree if no errors were detected
            if (!parser.errorDetected)
                log.info(prog.toString(""));
            log.info("===================================");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            /* Closes the open input file */
            if (br != null) try {
                br.close();
            } catch (IOException e1) {
                log.error(e1.getMessage(), e1);
            }
        }
    }
}
