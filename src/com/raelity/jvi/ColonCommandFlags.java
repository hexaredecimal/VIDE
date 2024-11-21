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

/**
 * Flags used to direct ":" command parsing. Only some of these
 * are used.
 */
public interface ColonCommandFlags {
  /** allow a linespecs */
  public static final int RANGE   = 0x01;
  /** allow a ! after the command name (--USED--) */
  public static final int BANG	   = 0x02;
  /** allow extra args after command name */
  public static final int EXTRA   = 0x04;       // -- XXX
  /** expand wildcards in extra part */
  public static final int XFILE   = 0x08;
  /** no spaces allowed in the extra part */
  public static final int NOSPC   = 0x10;
  /** default file range is 1,$ */
  public static final int DFLALL  = 0x20;
  /** dont default to the current file name */
  public static final int NODFL   = 0x40;
  /** argument required */
  public static final int NEEDARG = 0x80;
  /** check for trailing vertical bar */
  public static final int TRLBAR  = 0x100;
  /** allow "x for register designation */
  public static final int REGSTR  = 0x200;
  /** allow count in argument, after command */
  public static final int COUNT   = 0x400;
  /** no trailing comment allowed */
  public static final int NOTRLCOM  = 0x800;
  /** zero line number allowed */
  public static final int ZEROR   = 0x1000;
  /** do not remove CTRL-V from argument */
  public static final int USECTRLV = 0x2000;
  /** num before command is not an address */
  public static final int NOTADR = 0x4000;
  /** has "+command" argument */
  public static final int EDITCMD = 0x8000;
  /** accepts buffer name */
  public static final int BUFNAME = 0x10000;
  /** multiple extra files allowed */
  public static final int FILES   = (XFILE | EXTRA);
  /** one extra word allowed */
  public static final int WORD1   = (EXTRA | NOSPC);
  /** 1 file allowed, defaults to current file */
  public static final int FILE1   = (FILES | NOSPC);
  /** 1 file allowed, defaults to "" */
  public static final int NAMEDF  = (FILE1 | NODFL);
  /** multiple files allowed, default is "" */
  public static final int NAMEDFS = (FILES | NODFL);


  //
  // Additional stuff
  //
  /** don't parse command into words, arg1 is one big line. */
  public static final int NOPARSE = 0x00020000;
}
