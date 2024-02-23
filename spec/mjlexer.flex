/* ---------> Import section <--------- */
/* The generated lexer class should be in this package by specification */
package rs.ac.bg.etf.pp1;

import java_cup.runtime.Symbol;

/* ---------> Section with JFlex directives <--------- */
%%

%{
	private Symbol new_symbol(int type) {
		/* Generates a token and memorizes position based on 'type' */
		return new Symbol(type, yyline + 1, yycolumn);
	}

	private Symbol new_symbol(int type, Object value) {
		/* Generates a token and memorizes position based on 'type' and 'value' */
		return new Symbol(type, yyline + 1, yycolumn, value);
	}
%}

%cup    /* Yylex.java (lexer) implements the Scanner interface (cup-compatible lexer) */
%line   /* Lexer counts lines of the input file */
%column /* Lexer counts columns of the input file */

%xstate COMMENT

%eofval{
    /* Lexer returns the EOF symbol when it finishes analyzing the input file */
    return new_symbol(sym.EOF);
%eofval}

/* ---------> Section with regular expressions <--------- */
%%

/* Lexer ignores white spaces */
" " 	    { }
"\b" 	    { }
"\t" 	    { }
"\n" 	    { }
"\r\n" 	    { }
"\f" 	    { }

/* Lexer recognizes keywords */
"program"	{ return new_symbol(sym.PROG, yytext()); }
"break"  	{ return new_symbol(sym.BREAK, yytext()); }
"class"  	{ return new_symbol(sym.CLASS, yytext()); }
"else"  	{ return new_symbol(sym.ELSE, yytext()); }
"const"		{ return new_symbol(sym.CONST, yytext()); }
"if"		{ return new_symbol(sym.IF, yytext()); }
"while" 	{ return new_symbol(sym.WHILE, yytext()); }
"new" 		{ return new_symbol(sym.NEW, yytext()); }
"print" 	{ return new_symbol(sym.PRINT, yytext()); }
"read" 		{ return new_symbol(sym.READ, yytext()); }
"return" 	{ return new_symbol(sym.RETURN, yytext()); }
"void" 		{ return new_symbol(sym.VOID, yytext()); }
"extends" 	{ return new_symbol(sym.EXTENDS, yytext()); }
"continue" 	{ return new_symbol(sym.CONTINUE, yytext()); }
"foreach" 	{ return new_symbol(sym.FOREACH, yytext()); }
"findAny" 	{ return new_symbol(sym.FINDANY, yytext()); }
"findAll" 	{ return new_symbol(sym.FINDALL, yytext()); }

/* Lexer recognizes operators */
"+" 		{ return new_symbol(sym.PLUS, yytext()); }
"-" 		{ return new_symbol(sym.MINUS, yytext()); }
"*" 		{ return new_symbol(sym.MUL, yytext()); }
"/" 		{ return new_symbol(sym.DIV, yytext()); }
"%" 		{ return new_symbol(sym.MOD, yytext()); }
"++" 		{ return new_symbol(sym.INC, yytext()); }
"--" 		{ return new_symbol(sym.DEC, yytext()); }

"==" 		{ return new_symbol(sym.IS_EQUAL, yytext()); }
"!=" 		{ return new_symbol(sym.NOT_EQUAL, yytext()); }
">" 		{ return new_symbol(sym.GREATER, yytext()); }
">=" 		{ return new_symbol(sym.GREATER_EQUAL, yytext()); }
"<" 		{ return new_symbol(sym.LESS, yytext()); }
"<=" 		{ return new_symbol(sym.LESS_EQUAL, yytext()); }

"&&" 		{ return new_symbol(sym.AND, yytext()); }
"||" 		{ return new_symbol(sym.OR, yytext()); }

"=" 		{ return new_symbol(sym.EQUAL, yytext()); }

";" 		{ return new_symbol(sym.SEMI, yytext()); }
":" 		{ return new_symbol(sym.COLON, yytext()); }
"," 		{ return new_symbol(sym.COMMA, yytext()); }
"." 		{ return new_symbol(sym.POINT, yytext()); }

"(" 		{ return new_symbol(sym.LEFT_PAREN, yytext()); }
")" 		{ return new_symbol(sym.RIGHT_PAREN, yytext()); }
"[" 		{ return new_symbol(sym.LEFT_SQUARE_BRACKET, yytext()); }
"]" 		{ return new_symbol(sym.RIGHT_SQUARE_BRACKET, yytext()); }
"{" 		{ return new_symbol(sym.LEFT_BRACE, yytext()); }
"}" 		{ return new_symbol(sym.RIGHT_BRACE, yytext()); }
"=>"		{ return new_symbol(sym.ARROW, yytext()); }

/* Lexer reads and ignores comments in the COMMENT state */
"//"                            { yybegin(COMMENT); }   /* Function yybegin() moves the lexer to the COMMENT state */
<COMMENT> .                     { yybegin(COMMENT); }   /* Symbol . matches every input character (except newline) */
<COMMENT> "\n"                  { yybegin(YYINITIAL); } /* When it reaches the newline character, returns to YYINITIAL */
<COMMENT> "\r\n"                { yybegin(YYINITIAL); } /* When it reaches the newline character, returns to YYINITIAL */

/* Lexer recognizes constants */
[0-9]+                          { return new_symbol(sym.INT_CONST, new Integer (yytext())); }
'.'                             { return new_symbol(sym.CHAR_CONST, new Character(yytext().charAt(1))); }
("true"|"false")                { return new_symbol(sym.BOOL_CONST, new Boolean(yytext())); }

/* Lexer recognizes identifiers */
([a-z]|[A-Z])[a-z|A-Z|0-9|_]*   { return new_symbol(sym.IDENT, yytext()); }

/* Lexer otherwise prints an error message */
.  { System.err.println("Lexical error: [" + yytext() + "] : at line " + (yyline + 1) + " : in column " + (yycolumn + 1)); }
