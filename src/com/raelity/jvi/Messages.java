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
import com.raelity.jvi.swing.*;

interface Messages {
  String e_abort	="Command aborted";
  String e_argreq	="Argument required";
  String e_backslash	="\\ should be followed by /, ? or &";
  String e_curdir	="Command not allowed from exrc/vimrc in current dir or tag search";
  String e_exists	="File exists (use ! to override)";
  String e_failed	="Command failed";
  String e_internal	="Internal error";
  String e_interr	="Interrupted";
  String e_invaddr	="Invalid address";
  String e_invarg	="Invalid argument";
  String e_invarg2	="Invalid argument: ";
  String e_invrange	="Invalid range";
  String e_invcmd	="Invalid command";
  String e_markinval	="Mark has invalid line number";
  String e_marknotset	="Mark not set";
  String e_nesting	="Scripts nested too deep";
  String e_noalt	="No alternate file";
  String e_noabbr	="No such abbreviation";
  String e_nobang	="No ! allowed";
  String e_noinstext	="No inserted text yet";
  String e_nolastcmd	="No previous command line";
  String e_nomap	="No such mapping";
  String e_nomatch	="No match";
  String e_nomatch2	="No match: ";
  String e_noname	="No file name";
  String e_nopresub	="No previous substitute regular expression";
  String e_noprev	="No previous command";
  String e_noprevre	="No previous regular expression";
  String e_norange	="No range allowed";
  String e_noroom	="Not enough room";
  String e_notcreate	="Can't create file ";
  String e_notmp	="Can't get temp file name";
  String e_notopen	="Can't open file ";
  String e_notread	="Can't read file ";
  String e_nowrtmsg	="No write since last change (use ! to override)";
  String e_null		="Null argument";
  String e_outofmem	="Out of memory!";
  String e_patnotf2	="Pattern not found: ";
  String e_positive	="Argument must be positive";
  String e_re_damg	="Damaged match string";
  String e_re_corr	="Corrupted regexp program";
  String e_readonly	="'readonly' option is set (use ! to override)";
  String e_scroll	="Invalid scroll size";
  String e_tagformat	="Format error in tags file \"%s\"";
  String e_tagstack	="tag stack empty";
  String e_toocompl	="Command too complex";
  String e_toombra	="Too many \\(";
  String e_toomket	="Too many \\)";
  String e_toomsbra	="Too many [";
  String e_toomany	="Too many file names";
  String e_trailing	="Trailing characters";
  String e_umark	="Unknown mark";
  String e_unknown	="Unknown";
  String e_write	="Error while writing";
  String e_zerocount	="Zero count";
}
