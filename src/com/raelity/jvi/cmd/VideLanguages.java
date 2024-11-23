package com.raelity.jvi.cmd;

/**
 *
 * @author hexaredecimal
 */
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

public enum VideLanguages {
	ACTIONSCRIPT(SyntaxConstants.SYNTAX_STYLE_ACTIONSCRIPT), 
	ASSEMBLY(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86),
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
	BASH(SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL), BATCH(SyntaxConstants.SYNTAX_STYLE_WINDOWS_BATCH),
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

	public static VideLanguages fromFileType(String file_type) {
		// .as
		if (file_type.equals("as")) return ACTIONSCRIPT;
		
		// .asm .as
		if (file_type.equals("asm") || file_type.equals(".as")) return ASSEMBLY;
		//TODO: BBCODE
		
		// .c, .h
		if (file_type.equals("c") || file_type.equals("h")) return C;
		
		//clj .cljs .cljr .cljc .cljd .edn
		if (file_type.equals("clj") || file_type.equals("cljs")
			||file_type.equals("cljr") || file_type.equals("cljc")
			||file_type.equals("cljd") || file_type.equals("edn")
			) return CLOJURE;
		
		//.C, .cc, .cpp, .cxx, .c++, .H, .hh, .hpp, .hxx, .h++ .cppm, .ixx
		if (file_type.equals("C") || file_type.equals("cc")
			|| file_type.equals("cpp") || file_type.equals("cxx")
			|| file_type.equals("c++") || file_type.equals("H")
			|| file_type.equals("hh") || file_type.equals("hpp")
			|| file_type.equals("hpp") || file_type.equals("hxx")
			|| file_type.equals("h++") || file_type.equals("cppm")
			|| file_type.equals("ixx")
			) return CPLUSPLUS;
		
		// .cs, .csx
		if (file_type.equals("cs") || file_type.equals("csx")) return CSHARP;
		
		// .css
		if (file_type.equals("css")) return CSS;
		
		// TODO: Delphi
		// TODO: DTD
		
		// .f90, .f, .for
		if (file_type.equals("f90") || file_type.equals("f")
			|| file_type.equals("for")) return FORTRAN;

		// .groovy, .gvy, .gy, .gsh
		if (file_type.equals("groovy") || file_type.equals("gvy")
			|| file_type.equals("gy") || file_type.equals("gsh")) return GROOVY;

		// .html, htm
		if (file_type.equals("html") || file_type.equals("htm")) return HTML;
		
		// .java, (.class, .jar, .jmod, .war) part of java but not used for source files
		if (file_type.equals("java")) return JAVA;

		//.js .cjs .mjs .jsx
		if (file_type.equals("js") || file_type.equals("cjs")
			|| file_type.equals("mjs") || file_type.equals("jsx")) return JAVASCRIPT;

		// .json 
		if (file_type.equals("json")) return JSON;

		// .jsp, .jspx, .jspf
		if (file_type.equals("jsp") || file_type.equals("jspx")
			|| file_type.equals("jspf")) return JSP;
		
		// .latex, .tex
		if (file_type.equals("latex") || file_type.equals("tex")) return LATEX;
		
		// .lisp, .lsp, .l, .cl, .fasl
		if (file_type.equals("lisp") || file_type.equals("lsp")
			||file_type.equals("l") || file_type.equals("cl")
			||file_type.equals("fasl") 
			) return LISP;

		// .lua
		if (file_type.equals("lua")) return LUA;
		
		// Makefile
		if (file_type.equals("Makefile")) return MAKEFILE;

		// TODO: MXML
		// TODO: NSIS

		// .plx, .pls, .pl, .pm, .xs, .t, .pod, .cgi, .psgi
		if (file_type.equals("plx") || file_type.equals("pls")
			|| file_type.equals("pl") || file_type.equals("pm")
			|| file_type.equals("xs") || file_type.equals("t")
			|| file_type.equals("pod") || file_type.equals("cgi")
			|| file_type.equals("psgi")
			) return PERL;
		
		// .php,.phar,.phtml,.pht,.phps
		if (file_type.equals("php") || file_type.equals("phar")
			||file_type.equals("phtml") || file_type.equals("pht")
			||file_type.equals("phps") 
			) return PHP;
		
		// .properties
		if (file_type.equals("properties")) return PROPERTIES;

		// .py, .pyw, .pyz, .pyi, .pyc, .pyd
		if (file_type.equals("py") || file_type.equals("pyw")
			||file_type.equals("pyz") || file_type.equals("pyi")
			||file_type.equals("pyc") || file_type.equals("pyd")) return PYTHON;

		// .rb, .ru
		if (file_type.equals("rb") || file_type.equals("ru")) return RUBY;

		// .sass, .scss
		if (file_type.equals("sass") || file_type.equals("scss")) return SAS;

		// .scala, .sc
		if (file_type.equals("scala") || file_type.equals("sc")) return SCALA;

		// .sql
		if (file_type.equals("sql")) return SQL;

		// tcl, .tbc
		if (file_type.equals("tcl") || file_type.equals("tcb")) return SCALA;

		// .bash, .sh
		if (file_type.equals("bash") || file_type.equals("sh")) return BASH;

		// bat, .cmd, .btm
		if (file_type.equals("bat") || file_type.equals("cmd")
			|| file_type.equals("btm")) return BATCH;

		// .xml
		if (file_type.equals("xml")) return XML;

		// .go
		if (file_type.equals("go")) return GO;

		// .rs, .rlib
		if (file_type.equals("rs") || file_type.equals("rlib")) return RUST;
		
		// .ts, .tsx, .mts, .cts
		if (file_type.equals("ts") || file_type.equals("tsx")
			||file_type.equals("mts") || file_type.equals("cts")
			) return TYPESCRIPT;
		
		// .txt, and *
		return TXT;
	}
}
