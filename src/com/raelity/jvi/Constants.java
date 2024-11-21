/**
 * Title:        jVi<p>
 * Description:  A VI-VIM clone.
 * Use VIM as a model where applicable.<p>
 * Copyright:    Copyright (c) Ernie Rael<p>
 * Company:      Raelity Engineering<p>
 * @author Ernie Rael
 * @version 1.0
 */

/*
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 * 
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 * 
 * The Original Code is jvi - vi editor clone.
 * 
 * The Initial Developer of the Original Code is Ernie Rael.
 * Portions created by Ernie Rael are
 * Copyright (C) 2000 Ernie Rael.  All Rights Reserved.
 * 
 * Contributor(s): Ernie Rael <err@raelity.com>
 */
package com.raelity.jvi;

public interface Constants {

  // for <i>dir</i>
  public static final int FORWARD = 1;
  // for <i>dir</i>
  public static final int BACKWARD = -1;

  /** May be returned by add_new_completion(): */
  static final int RET_ERROR	= -1;

  static final int MAXLNUM = 0x7fffffff;
  static final int MAXCOL = 0x7fffffff;

  static final int MAXTYPEBUFLEN = 5000; // seems huge, but check out orig...

  //
  // Operator IDs; The order must correspond to opchars[] in ops.c!
  //
  static final int  OP_NOP	= 0;	// no pending operation
  static final int  OP_DELETE	= 1;	// "d"  delete operator
  static final int  OP_YANK	= 2;	// "y"  yank operator
  static final int  OP_CHANGE	= 3;	// "c"  change operator
  static final int  OP_LSHIFT	= 4;	// "<"  left shift operator
  static final int  OP_RSHIFT	= 5;	// ">"  right shift operator
  static final int  OP_FILTER	= 6;	// "!"  filter operator
  static final int  OP_TILDE	= 7;	// "g~" switch case operator
  static final int  OP_INDENT	= 8;	// "="  indent operator
  static final int  OP_FORMAT	= 9;	// "gq" format operator
  static final int  OP_COLON	= 10;	// ":"  colon operator
  static final int  OP_UPPER	= 11;	// "gU" make upper case operator
  static final int  OP_LOWER	= 12;	// "gu" make lower case operator
  static final int  OP_JOIN	= 13;	// "J"  join operator, only Visual mode
  static final int  OP_JOIN_NS	= 14;	// "gJ" join operator, only Visual mode
  static final int  OP_ROT13	= 15;	// "g?" rot-13 encoding
  static final int  OP_REPLACE	= 16;	// "r"  replace chars, only Visual mode
  static final int  OP_INSERT	= 17;	// "I"  Insert column, only Visual mode
  static final int  OP_APPEND	= 18;	// "A"  Append column, only Visual mode

  //
  // Flags for do_put
  //
  static final int PUT_FIXINDENT = 1;	// make indent look nice
  static final int PUT_CURSEND = 1;	// leave cursor after end of new text

  //
  // Motion types, used for operators and for yank/delete registers.
  //
  static final int  MCHAR	= 0;	// character-wise movement/register
  static final int  MLINE	= 1;	// line-wise movement/register
  static final int  MBLOCK	= 2;	// block-wise register

  static final int SHOWCMD_COLS = 10;	// columns needed by shown command

  //
  // values for State
  //
  // The lower byte is used to distinguish normal/visual/op_pending and cmdline/
  // insert+replace mode.  This is used for mapping.  If none of these bits are
  // set, no mapping is done.
  // The upper byte is used to distinguish between other states.
  //

  static final int  NORMAL	= 0x01;	// Normal mode, command expected
  static final int  VISUAL	= 0x02;	// Visual mode - use get_real_state()
  static final int  OP_PENDING	= 0x04;	// Normal mode, operator is pending - use
				  // get_real_state()
  static final int  CMDLINE	= 0x08;	// Editing command line
  static final int  INSERT	= 0x10;	// Insert mode

  static final int  NORMAL_BUSY	= (0x100 + NORMAL); // Normal mode, busy command
  static final int  REPLACE	= (0x200 + INSERT); // Replace mode
  static final int  VREPLACE	= (0x300 + INSERT); // Virtual replace mode
  static final int  HITRETURN	= (0x600 + NORMAL); // wait return or command
  static final int  ASKMORE	= 0x700; // Asking if you want --more--
  static final int  SETWSIZE	= 0x800; // window size has changed
  static final int  ABBREV	= 0x900; // abbreviation instead of mapping
  static final int  EXTERNCMD	= 0xa00; // executing an external command
  static final int  SHOWMATCH	= (0xb00 + INSERT); // show matching paren
  static final int  CONFIRM	= 0xc00; // ":confirm" prompt

  // Values for buflist_getfile()
  static final int  GETF_SETMARK	= 0x01;	// set pcmark before jumping
  static final int  GETF_ALT		= 0x02;	// jump to alternate file
  						// (not buf num)
  static final int  GETF_SWITCH	= 0x04;	// respect 'switchbuf' set when jumping

  //
  // flags for update_screen()
  // The higher the value, the higher the priority
  //
  static final int VALID		= 10;  // buffer not changed
  static final int INVERTED		= 20;  // redisplay inverted part
  static final int VALID_TO_CURSCHAR	= 30;  // line at/below cursor changed
  static final int VALID_BEF_CURSCHAR	= 35;  // line just above cursor changed
  static final int NOT_VALID		= 40;  // buffer changed somewhere
  static final int CLEAR		= 50;  // screen messed up, clear it

  //
  // Definitions of various common control characters
  //

  static final int  NUL	= '\000';
  static final int  BS	= '\010';
  static final int  TAB	= '\011';
  static final int  NL	= '\012';
  static final String  NL_STR	= "\012";
  static final int  FF	= '\014';
  static final int  CR	= '\015';
  static final int  ESC	= '\033';
  static final String ESC_STR	= "\033";
  static final int  DEL	= 0x7f;
  static final int  CSI	= 0x9b;

  // Bits for modifier mask
  // WAS: 0x01 cannot be used, because the modifier must be 0x02 or higher
  // not sure why 0x01 can't be used, use it, set it to match swing
  static final int MOD_MASK_SHIFT   = 0x01;
  static final int MOD_MASK_CTRL    = 0x02;
  static final int MOD_MASK_ALT	    = 0x08;
  static final int MOD_MASK_META    = 0x04;

  // Values for change_indent()
  static final int INDENT_SET	= 1;	// set indent
  static final int INDENT_INC	= 2;	// increase indent
  static final int INDENT_DEC	= 3;	// decrease indent

  // flags for beginline()
  public static final int BL_WHITE = 1;// cursor on first non-white in the line
  public static final int BL_SOL = 2;  // use 'sol' option
  public static final int BL_FIX = 4;  // don't leave cursor on a NUL

  // return values for functions
  static final int OK		= 1;
  static final int FAIL		= 0;

  //
  // Values for do_tag().
  //
  static final int DT_TAG	= 1;	/* jmp to newer pos or same tag again */
  static final int DT_POP	= 2;	/* jump to older position */
  static final int DT_NEXT	= 3;	/* jump to next match of same tag */
  static final int DT_PREV	= 4;	/* jump to previous match of same tag */
  static final int DT_FIRST	= 5;	/* jump to first match of same tag */
  static final int DT_LAST	= 6;	/* jump to first match of same tag */
  static final int DT_SELECT	= 7;	/* jump to selection from list */
  static final int DT_HELP	= 8;	/* like DT_TAG, but no wildcards */
  static final int DT_JUMP	= 9;	/* jmp to new tag or select from list */

  //
  // Values for 'options' argument in do_search() and searchit()
  //
  static final int SEARCH_REV   = 0x01;  // go in reverse of previous dir.
  static final int SEARCH_ECHO  = 0x02;  // echo search command and handle opts
  static final int SEARCH_MSG   = 0x0c;  // give messages (yes, it's not 0x04)
  static final int SEARCH_NFMSG = 0x08;  // give all messages except not found
  static final int SEARCH_OPT   = 0x10;  // interpret optional flags
  static final int SEARCH_HIS   = 0x20;  // put search pattern in history
  static final int SEARCH_END   = 0x40;  // put cursor at end of match
  static final int SEARCH_NOOF  = 0x80;  // don't add offset to position
  static final int SEARCH_START =0x100;  // start search without col offset
  static final int SEARCH_MARK  =0x200;  // set previous context mark
  static final int SEARCH_KEEP  =0x400;  // keep previous search pattern

  //
  // Values for find_ident_under_cursor()
  //
  static final int FIND_IDENT	=1;	// find identifier (word)
  static final int FIND_STRING	=2;	// find any string (WORD)

  //
  // characters for the p_cpo option:
  //
  static final int CPO_ALTREAD	= 'a';	/* ":read" sets alternate file name */
  static final int CPO_ALTWRITE	= 'A';	/* ":write" sets alternate file name */
  static final int CPO_BAR	= 'b';	/* "\|" ends a mapping */
  static final int CPO_BSLASH	= 'B';	/* backslash in mapping is not special*/
  static final int CPO_SEARCH	= 'c';
  static final int CPO_DOTTAG	= 'd';	/* "./tags" in 'tags' in current dir */
  static final int CPO_EXECBUF	= 'e';
  static final int CPO_EMPTYREGION = 'E';	/* operating on empty region is error */
  static final int CPO_FNAMER	= 'f';	/* set file name for ":r file" */
  static final int CPO_FNAMEW	= 'F';	/* set file name for ":w file" */
  static final int CPO_JOINSP	= 'j';	/* only use two spaces for join after '.' */
  static final int CPO_ENDOFSENT = 'J';	/* need two spaces to detect end of sentence */
  static final int CPO_KEYCODE	= 'k';	/* don't recognize raw key code in mappings */
  static final int CPO_KOFFSET	= 'K';	/* don't wait for key code in mappings */
  static final int CPO_LITERAL	= 'l';	/* take char after backslash in [] literal */
  static final int CPO_LISTWM	= 'L';	/* 'list' changes wrapmargin */
  static final int CPO_SHOWMATCH	= 'm';
  static final int CPO_LINEOFF	= 'o';
  static final int CPO_OVERNEW	= 'O';	/* silently overwrite new file */
  static final int CPO_LISP	= 'p';	/* 'lisp' indenting */
  static final int CPO_REDO	= 'r';
  static final int CPO_BUFOPT	= 's';
  static final int CPO_BUFOPTGLOB	= 'S';
  static final int CPO_TAGPAT	= 't';
  static final int CPO_UNDO	= 'u';	/* "u" undoes itself */
  static final int CPO_CW		= 'w';	/* "cw" only changes one blank */
  static final int CPO_FWRITE	= 'W';	/* "w!" doesn't overwrite readonly files */
  static final int CPO_ESC		= 'x';
  static final int CPO_YANK	= 'y';
  static final int CPO_DOLLAR	= '$';
  static final int CPO_FILTER	= '!';
  static final int CPO_MATCH	= '%';
  static final int CPO_STAR	= '*';	/* ":*" means ":@" */
  static final int CPO_SPECI	= '<';	/* don't recognize <> in mappings */
  static final String CPO_DEFAULT = "aABceFs";
  static final String CPO_ALL	= "aAbBcdeEfFjJkKlLmoOprsStuwWxy$!%*<";
  
  static final char BS_INDENT   = 'i';
  static final char BS_EOL      = 'o';
  static final char BS_START    = 's';

  // struct to store values from 'guicursor'
  static final int SHAPE_N	= 0; 	// Normal mode
  static final int SHAPE_V	= 1; 	// Visual mode
  static final int SHAPE_I	= 2; 	// Insert mode
  static final int SHAPE_R	= 3; 	// Replace mode
  static final int SHAPE_C	= 4; 	// Command line Normal mode
  static final int SHAPE_CI	= 5; 	// Command line Insert mode
  static final int SHAPE_CR	= 6; 	// Command line Replace mode
  static final int SHAPE_SM	= 7; 	// showing matching paren
  static final int SHAPE_O	= 8; 	// Operator-pending mode
  static final int SHAPE_VE	= 9; 	// Visual mode, 'seleciton' exclusive
  static final int SHAPE_COUNT	= 10; 

  static final int SHAPE_BLOCK	= 0; 	// block cursor
  static final int SHAPE_HOR	= 1; 	// horizontal bar cursor
  static final int SHAPE_VER	= 2; 	// vertical bar cursor
  
  // edit mode operations that can be specified through a keymap
  static final int IM = 0x40000000;
  static final int IM_SHIFT_RIGHT = 1 + IM;
  static final int IM_SHIFT_LEFT = 2 + IM;
  static final int IM_SHIFT_RIGHT_TO_PAREN = 3 + IM;
  static final int IM_SHIFT_LEFT_TO_PAREN = 4 + IM;
  static final int IM_INS_REP = 5 + IM;
}

/*
:1,$s/\<no_mapping\>/VIG.&/g
:1,$s/\<allow_keys\>/VIG.&/g
:1,$s/\<finish_op\>/VIG.&/g
:1,$s/\<State\>/VIG.&/g
:1,$s/\<curwin\>/VIG.&/g
:1,$s/\<curwin.w_cursor\>/curwin.getWCursor()/
:1,$s/\<Ctrl(/0x1f \& (int)(/g
	The Ctrl thing is pretty ugly
 */
