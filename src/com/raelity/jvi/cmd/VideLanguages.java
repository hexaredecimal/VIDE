package com.raelity.jvi.cmd;

/**
 *
 * @author hexaredecimal
 */
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

public enum VideLanguages {
	ACTIONSCRIPT(SyntaxConstants.SYNTAX_STYLE_ACTIONSCRIPT), ASSEMBLY(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86),
	BBCODE(SyntaxConstants.SYNTAX_STYLE_BBCODE), C(SyntaxConstants.SYNTAX_STYLE_C),
	CLOJURE(SyntaxConstants.SYNTAX_STYLE_CLOJURE), CPLUSPLUS(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS),
	CSHARP(SyntaxConstants.SYNTAX_STYLE_CSHARP), CSS(SyntaxConstants.SYNTAX_STYLE_CSS),
	DELPHI(SyntaxConstants.SYNTAX_STYLE_DELPHI), DTD(SyntaxConstants.SYNTAX_STYLE_DTD),
	FORTRAN(SyntaxConstants.SYNTAX_STYLE_FORTRAN), GROOVY(SyntaxConstants.SYNTAX_STYLE_GROOVY),
	HTML(SyntaxConstants.SYNTAX_STYLE_HTML), JAVA(SyntaxConstants.SYNTAX_STYLE_JAVA),
	JAVASCRIPT(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT), JSON(SyntaxConstants.SYNTAX_STYLE_JSON),
	JSP(SyntaxConstants.SYNTAX_STYLE_JSP), LATEX(SyntaxConstants.SYNTAX_STYLE_LATEX),
	LISP(SyntaxConstants.SYNTAX_STYLE_LISP), LUA(SyntaxConstants.SYNTAX_STYLE_LUA),
	MAKEFILE(SyntaxConstants.SYNTAX_STYLE_MAKEFILE), MXML(SyntaxConstants.SYNTAX_STYLE_MXML),
	NSIS(SyntaxConstants.SYNTAX_STYLE_NSIS), PERL(SyntaxConstants.SYNTAX_STYLE_PERL),
	PHP(SyntaxConstants.SYNTAX_STYLE_PHP), PROPERTIES(SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE),
	PYTHON(SyntaxConstants.SYNTAX_STYLE_PYTHON), RUBY(SyntaxConstants.SYNTAX_STYLE_RUBY),
	SAS(SyntaxConstants.SYNTAX_STYLE_SAS), SCALA(SyntaxConstants.SYNTAX_STYLE_SCALA),
	SQL(SyntaxConstants.SYNTAX_STYLE_SQL), TCL(SyntaxConstants.SYNTAX_STYLE_TCL),
	Bash(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL), BATCH(SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH),
	XML(SyntaxConstants.SYNTAX_STYLE_XML), TXT(SyntaxConstants.SYNTAX_STYLE_NONE),
	GO(SyntaxConstants.SYNTAX_STYLE_GO), RUST(SyntaxConstants.SYNTAX_STYLE_RUST), 
	TYPESCRIPT(SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT);
	private String style;

	VideLanguages(String syntaxStyle) {
		style = syntaxStyle;
	}

	public String getHighlight() {
		return style;
	}
}
