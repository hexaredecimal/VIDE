/* vi:set ts=8 sw=2
 *
 * VIM - Vi IMproved	by Bram Moolenaar
 *
 * Do ":help uganda"  in Vim to read copying and usage conditions.
 * Do ":help credits" in Vim to see a list of people who contributed.
 */
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

import com.raelity.jvi.ViTextView.JLOP;
import com.raelity.jvi.ViTextView.TAGOP;
import javax.swing.text.Segment;

import com.raelity.jvi.swing.KeyBinding;

import static com.raelity.jvi.Constants.*;
import static com.raelity.jvi.KeyDefs.*;

/**
 * Contains the main routine for processing characters in command mode.
 * Communicates closely with the code in ops.c to handle the operators.
 * <p>
 * This class started with VIM's normal.c. The idea is to work this into
 * something that can be used in a JEditorPane framework to implement vi.
 * So things that are handled by swing text are taken out.
 * Here is a partial list of changes.
 * <ul>
 * <li>Visual mode has been commented out for now.
 * </ul>
 * </p>
 *
 * <br>nv_*(): functions called to handle Normal and Visual mode commands.
 * <br>n_*(): functions called to handle Normal mode commands.
 * <br>v_*(): functions called to handle Visual mode commands.
 */

public class Normal {

  // for normal_cmd() use, stuff that was declared static in the function
  static int	opnum = 0;		    /* count before an operator */
  static int   restart_VIsual_select = 0;
  static int   old_mapped_len = 0;

  
  
  static int redo_VIsual_mode = NUL; /* 'v', 'V', or Ctrl-V */
  static int /*linenr_t*/ redo_VIsual_line_count; /* number of lines */
  static int /*colnr_t*/  redo_VIsual_col;/* number of cols or end column */
  static int /*long*/ redo_VIsual_count;/* count for Visual operator */

  static int seltab[] = {
    /* key		unshifted	shift included */
    K_S_RIGHT,		K_RIGHT,	1,
    K_S_LEFT,		K_LEFT,		1,
    K_S_UP,		K_UP,		1,
    K_S_DOWN,		K_DOWN,		1,
    K_S_HOME,		K_HOME,		1,
    K_S_END,		K_END,		1,
    // K_KHOME,		K_KHOME,	0,
    // K_XHOME,		K_XHOME,	0,
    // K_KEND,		K_KEND,		0,
    // K_XEND,		K_KEND,		0,
    K_PAGEUP,		K_PAGEUP,	0,
    // K_KPAGEUP,		K_KPAGEUP,	0,
    K_PAGEDOWN,		K_PAGEDOWN,	0,
    // K_KPAGEDOWN,	K_KPAGEDOWN,	0,
  };

  //
  // These are the info for parser state between characters.
  //

  static boolean newChunk = true;
  static boolean lookForDigit;
  static boolean pickupExtraChar;
  static boolean firstTimeHere01;
  static boolean editBusy;

  /**
   * Vi comands can have up to three chunks: buffer-operator-motion.
   * <br/>For example: <b>"a3y4&lt;CR&gt;</b>
   * <ul>
   * <li>has buffer: "a</li>
   * <li>and operator: 3y</li>
   * <li>and motion: 4&lt;CR&gt;</li>
   * </ul>
   * The original vim invokes normal 3 times, one for each chunk. The caller
   * had no knowledge of how many chunks there were in a single command,
   * it just called normal in a loop. normal kept control,
   * calling safe_getc as needed, and returned as each chunk was completed.
   * Accumulated information was saved in OPARG.
   * normal detected when a complete command was ready and then executed it,
   * often times in do_pending_op.
   * CMDARG has per chunk information, and OPARG is cleared after each
   * command.
   * <p>
   * To fit in the swing environment, we want to be able to parse one
   * character at a time in the event thread. This means we have to return
   * after each character. So we must maintain a bunch of "where am i" state
   * information between each character. This is messy, but not too bad.
   * (Only breiefly considered having a separate thread.)
   * </p><p>
   * Operator and motion are very similar, they have <count><char>
   * or sometimes more than one char.
   * </p>
   */

  static public void processInputChar(int c, boolean toplevel) {
    try {
      if(editBusy) {
        // NEEDSWORK: if exception from edit, turn edit off and finishupEdit
        Edit.edit(c, false, 0);
        if(!editBusy) {
          finishupEdit();
          newChunk = true;
          willStartNewChunk();
        }
      } else {
        normal_cmd(c, toplevel);
        if(!editBusy && newChunk) {
          willStartNewChunk();
        }
      }
    } catch(Exception e) {
      e.printStackTrace();
      Util.beep_flush();
      resetCommand();
    }
  }

  static void finishupEdit() {
    Misc.endInsertUndo();
  }

  static public void resetCommand() {
    oap.clearop();
    newChunk = true;
    willStartNewChunk();
    if(editBusy) {
      editBusy = false; // NEEDSWORK: what else
      finishupEdit();
    }
    clear_showcmd();
    Misc.showmode();
    Misc.ui_cursor_shape();
  }

  /**
   * This has the few things that would have exectued at the beginning
   * of normal just before getting a character. Only the stuff with
   * global implications needs to be put here.
   */
  static void willStartNewChunk() {
    //
    // If there is an operator pending, then the command we take this time
    // will terminate it. Finish_op tells us to finish the operation before
    // returning this time (unless the operation was cancelled).
    //
    boolean tflag = G.finish_op;
    G.finish_op = (oap.op_type != OP_NOP);
    if (G.finish_op != tflag) {
      Misc.ui_cursor_shape();	/* may show different cursor shape */
    }

    G.State = NORMAL_BUSY;
  }

  static OPARG oap = new OPARG();
  static CMDARG	    ca; 	   /* command arguments */
  static boolean    ctrl_w;	   /* got CTRL-W command */
  static boolean    need_flushbuf; /* need to call out_flush() */
  static int	    mapped_len;

  /**
   * normal
   *
   * Parse and execute a command in Normal mode.
   *
   * This is basically a big switch with the cases arranged in rough categories
   * in the following order:
   *
   *    <ol>
   *    <li> Macros (q, @)
   *    <li> Screen positioning commands (^U, ^D, ^F, ^B, ^E, ^Y, z)
   *    <li> Control commands (:, <help>, ^L, ^G, ^^, ZZ, *, ^], ^T)
   *    <li> Cursor motions (G, H, M, L, l, K_RIGHT,  , h, K_LEFT, ^H, k, K_UP,
   *	 ^P, +, CR, LF, j, K_DOWN, ^N, _, |, B, b, W, w, E, e, $, ^, 0)
   *    <li> Searches (?, /, n, N, T, t, F, f, ,, ;, ], [, %, (, ), {, })
   *    <li> Edits (., u, K_UNDO, ^R, U, r, J, p, P, ^A, ^S)
   *    <li> Inserts (A, a, I, i, o, O, R)
   *    <li> Operators (~, d, c, y, &gt;, &lt;, !, =)
   *    <li> Abbreviations (x, X, D, C, s, S, Y, &amp;)
   *    <li> Marks (m, ', `, ^O, ^I)
   *   <li> Register name setting ('"')
   *   <li> Visual (v, V, ^V)
   *   <li> Suspend (^Z)
   *   <li> Window commands (^W)
   *   <li> extended commands (starting with 'g')
   *   <li> mouse click
   *   <li> scrollbar movement
   *   <li> The end (ESC)
   *   </ol>
   */

  static public void normal_cmd(int c, boolean toplevel) {
                                        //NEEDSWORK: toplevel NOT USED

    Misc.update_curswant();	// in vim called just before calling normal_cmd

    if(newChunk) {
      newChunk = false;
      lookForDigit = true;
      pickupExtraChar = false;
      firstTimeHere01 = true;
      ca = new CMDARG();
      // vim_memset(&ca, 0, sizeof(ca)); cleared when created
      ca.oap = oap;

      ctrl_w = false;
      need_flushbuf = false;

      // look at willStartNewChunk for code that used to be here
      // like setting G.finish_op and G.State

      // following test if this chunk starting a new command.
      if (!G.finish_op && oap.regname == 0)
        opnum = 0;

      mapped_len = GetChar.typebuf_maplen();

      // c = GetChar.safe_vgetc();

      old_mapped_len = 0;

      /*
       * If a mapping was started in Visual or Select mode, remember the length
       * ...... REMOVED, put old_mapped_len = 0 above
       */

      if (c == NUL)
        c = K_ZERO;
      else if (c == K_KMULTIPLY)  /* K_KMULTIPLY is same as '*' */
        c = '*';
      else if (c == K_KMINUS)	/* K_KMINUS is same as '-' */
        c = '-';
      else if (c == K_KPLUS)	/* K_KPLUS is same as '+' */
        c = '+';
      else if (c == K_KDIVIDE)	/* K_KDIVIDE is same as '/' */
        c = '/';

      /*
       * In Visual/Select mode, a few keys are handled in a special way.
       * ........ REMOVED
       */

      // Don't set need flush for the first char of a command.
      // This will prevent single char commands from being displayed.
      // need_flushbuf = add_to_showcmd(c);
      if (G.finish_op || oap.regname != 0) {
        // in the middle of some command
        need_flushbuf = add_to_showcmd(c);
      } else {
        // first char of a new command
        add_to_showcmd(c);
      }
    } else {
      need_flushbuf = add_to_showcmd(c);
    }


    if(lookForDigit) {
      if (!(G.VIsual_active && G.VIsual_select)) {
        if (ctrl_w) {
          --G.no_mapping;
          --G.allow_keys;
        }
        // Pick up any leading digits and compute ca.count0
        if (    (c >= '1' && c <= '9')
                   || (ca.count0 != 0 && (/*c == K_DEL ||*/ c == '0'))) {
          /* *************************************************
             if (c == K_DEL)
             {
             ca.count0 /= 10;
          // NEEDSWORK: del_from_showcmd argument is not correct i bet.
          del_from_showcmd(4);	// delete the digit and ~@%
          }
          else
           **************************************************/
          ca.count0 = ca.count0 * 10 + (c - '0');
          if (ca.count0 < 0)	    // got too large!
            ca.count0 = 999999999;

          if (ctrl_w) {
            ++G.no_mapping;
            ++G.allow_keys;		// no mapping for nchar, but keys
          }
          return; // go get another char

        }

        //
        // If we got CTRL-W there may be a/another count
        //
        if (c == (0x1f & (int)('W')) && !ctrl_w && oap.op_type == OP_NOP)
        {
          ctrl_w = true;
          opnum = ca.count0;		// remember first count
          ca.count0 = 0;
          ++G.no_mapping;
          ++G.allow_keys;		// no mapping for nchar, but keys
          return; // go get another char
        }
      }
    }

    if(firstTimeHere01) {

      lookForDigit = false;
      firstTimeHere01 = false;

      ca.cmdchar = c;

      //
      // If we're in the middle of an operator (including after entering a yank
      // buffer with '"') AND we had a count before the
      // operator, then that count overrides the current value of ca.count0.
      // What * this means effectively, is that commands like "3dw" get turned
      // into "d3w" which makes things fall into place pretty neatly.
      // If you give a count before AND after the operator, they are multiplied.
      //
      if (opnum != 0) {
        if (ca.count0 != 0)
          ca.count0 *= opnum;
        else
          ca.count0 = opnum;
      }

      //
      // Always remember the count.  It will be set to zero (on the next call,
      // above) when there is no pending operator.
      // When called from main(), save the count for use by the "count" built-in
      // variable.
      //
      opnum = ca.count0;
      ca.count1 = (ca.count0 == 0 ? 1 : ca.count0);
    }

    //
    // Get an additional character if we need one.
    // For CTRL-W we already got it when looking for a count.
    //
    if (ctrl_w) {
      ca.nchar = ca.cmdchar;
      ca.cmdchar = (0x1f & (int)('W'));
    } else {
      if(!pickupExtraChar) {
        // check if additional char needed
        if (  (oap.op_type == OP_NOP
                      && Util.vim_strchr("@zm\"", ca.cmdchar) != null)
                 || (oap.op_type == OP_NOP
                     && (ca.cmdchar == 'r'
                         || (!G.VIsual_active && ca.cmdchar == 'Z')))
                 || Util.vim_strchr("tTfF[]g'`", ca.cmdchar) != null
                 || (ca.cmdchar == 'q'
                     && oap.op_type == OP_NOP
                     && !G.Recording
                     && !G.Exec_reg)
                 || ((ca.cmdchar == 'a' || ca.cmdchar == 'i')
                     && (oap.op_type != OP_NOP || G.VIsual_active)))
        {
          ++G.no_mapping;
          ++G.allow_keys;		/* no mapping for nchar, but allow key codes */
          pickupExtraChar = true;
          if (ca.cmdchar == 'r') {
            G.State = REPLACE;	// pretend Replace mode, for cursor shape
            Misc.ui_cursor_shape();	// show different cursor shape
          }
          return; // and get the extra char
        }
      } else {
        // we've got the extra char
        if (ca.cmdchar == 'g') {
          ca.nchar = c;
        }
        if (ca.cmdchar == 'g' && ca.nchar == 'r') {
          G.State = REPLACE;	// pretend Replace mode, for cursor shape
          Misc.ui_cursor_shape();	// show different cursor shape
        }
        // For 'g' commands, already got next char above, "gr" still needs an
        // extra one though.
        //
        if (ca.cmdchar != 'g') {
          ca.nchar = c;
        } else if (ca.nchar == 'r') {
          ca.extra_char = c;
          throw new RuntimeException("need yet another char for gr");
        }
        G.State = NORMAL_BUSY;
        --G.no_mapping;
        --G.allow_keys;
      }
    }

    //
    //
    // All the characters needed for the comand have been collected
    //
    //

    /*
     * Flush the showcmd characters onto the screen so we can see them while
     * the command is being executed.  Only do this when the shown command was
     * actually displayed, otherwise this will slow down a lot when executing
     * mappings.
     */
    if (need_flushbuf)
      Misc.out_flush();

    // c not used past this point
    // ctrl_w not used past this point
    // need_flushbuf not used past this point
    // mapped_len not used past this point

    int		    intflag = 0;
    boolean	    flag = false;
    boolean	    type = false;	    /* type of operation */
    int		    dir = FORWARD;	    /* search direction */
    CharBuf	    searchbuff = new CharBuf(); /* buffer for search string */
    boolean	    dont_adjust_op_end = false;
    FPOS	    old_pos;		    /* cursor position before command */
    int		    old_col = G.curwin.getWCurswant();

middle_code:
    do {
      G.State = NORMAL;
      if (ca.nchar == ESC)
      {
	clearop(oap);
	if (p_im != 0 && G.restart_edit == 0)
	  G.restart_edit = 'a';
	break middle_code;	// used to be goto normal_end
      }

      /* when 'keymodel' contains "startsel" some keys start Select/Visual mode */
      if (!G.VIsual_active && Util.vim_strchr(G.p_km, 'a') != null)
      {
	int	i;

	for (i = 0; i < seltab.length; i += 3)
	{
	  if (seltab[i] == ca.cmdchar
	      && (seltab[i + 2] != 0 || (G.mod_mask & MOD_MASK_SHIFT) != 0))
	  {
	    ca.cmdchar = seltab[i + 1];
	    start_selection();
	    break;
	  }
	}
      }

      msg_didout = false;    /* don't scroll screen up for normal command */
      msg_col = 0;
      old_pos = (FPOS)G.curwin.getWCursor().copy();/* remember cursor was */

      /*
       * Generally speaking, every command below should either clear any pending
       * operator (with *clearop*()), or set the motion type variable
       * oap.motion_type.
       *
       * When a cursor motion command is made,
       * it is marked as being a character or
       * line oriented motion.  Then, if an operator is in effect, the operation
       * becomes character or line oriented accordingly.
       */
      /*
       * Variables available here:
       * ca.cmdchar	command character
       * ca.nchar	extra command character
       * ca.count0	count before command (0 if no count given)
       * ca.count1	count before command (1 if no count given)
       * oap		Operator Arguments (same as ca.oap)
       * flag		is FALSE, use as you like.
       * dir		is FORWARD, use as you like.
       */
      try {
	switch (ca.cmdchar)
	{
	  /*
	   * 0: Macros
	   */
	  case 'q':
	    nv_q(ca);
	    break;

	  case '@':		/* execute a named register */
	    nv_at(ca);
	    break;

	    /*
	     * 1: Screen positioning commands
	     */
	  case 0x1f & (int)('D'):	// Ctrl
	  case 0x1f & (int)('U'):	// Ctrl
	    nv_halfpage(ca);
	    break;

	  case 0x1f & (int)('B'):	// Ctrl
	  case K_S_UP:
	  case K_PAGEUP:
	    // case K_KPAGEUP:
	    dir = BACKWARD;

	  case 0x1f & (int)('F'):	// Ctrl
	  case K_S_DOWN:
	  case K_PAGEDOWN:
	  // case K_KPAGEDOWN:
	    if (checkclearop(oap))
	      break;
	    Misc.onepage(dir, ca.count1);
	    break;

	  case 0x1f & (int)('E'):	// Ctrl
	    flag = true;
	    // FALLTHROUGH

	  case 0x1f & (int)('Y'):	// Ctrl
	    nv_scroll_line(ca, flag);
	    break;

	  case 'z':
	    if (!checkclearop(oap))
	      nv_zet(ca);
	    break;

	    //
	    // 2: Control commands
	    //
	  case ':':
	    nv_colon(ca);
	    break;

	  case 'Q':
	    //
	    // Ignore 'Q' in Visual mode, just give a beep.
	    //
	    notSup("exmode");	// XXX
	    if (G.VIsual_active)
	      Util.vim_beep();
	    else if (!checkclearop(oap))
	      do_exmode();
	    break;

	  case K_HELP:
	  case K_F1:
	  // case K_XF1:
	    notImp("help");
	    if (!checkclearopq(oap))
	      do_help(null);
	    break;

	  case 0x1f & (int)('L'):	// Ctrl
	    notSup("update_screen");
	    if (!checkclearop(oap))
	    {
	      update_screen(CLEAR);
	    }
	    break;

	  case 0x1f & (int)('G'):	// Ctrl
	    nv_ctrlg(ca);
	    break;

	  case K_CCIRCM:	 // CTRL-^, short for ":e #"
	    notImp(":e #");
	    if (!checkclearopq(oap))
	      buflist_getfile((int)ca.count0, 0,
			      GETF_SETMARK|GETF_ALT, false);
	    break;

	  case 'Z':
	    notImp("close file");
	    nv_zzet(ca);
	    break;

	  case 163:		// the pound sign, '#' for English keyboards
	    ca.cmdchar = '#';
	    //FALLTHROUGH

	  case 0x1f & (int)(']'):   // :ta to current identifier 	// Ctrl
	  case 'K':		    // run program for current identifier
	  case '*':		    // / to current identifier or string
	  case K_KMULTIPLY:	    // same as '*'
	  case '#':		    // ? to current identifier or string
	    if (ca.cmdchar == K_KMULTIPLY)
	      ca.cmdchar = '*';
	    if(nv_ident(ca, searchbuff))
                return;
	    break;

	  case 0x1f & (int)('T'):    // backwards in tag stack	// Ctrl
	    if (!checkclearopq(oap)) {
              // do_tag((char_u *)"", DT_POP, (int)ca.count1, FALSE, TRUE);
              ViManager.getViFactory().tagStack(TAGOP.OLDER, ca.count1);
            }
	    break;

	    //
	    // Cursor motions
	    //
	  case 'G':
	    // ? curbuf.b_ml.ml_line_count
	    nv_goto(ca, G.curwin.getLineCount());
	    break;

	  case 'H':
	  case 'M':
	  case 'L':
	    nv_scroll(ca);
	    break;

	  case K_RIGHT:
	    if ((G.mod_mask & MOD_MASK_CTRL) != 0)
	    {
	      oap.inclusive = false;
	      nv_wordcmd(ca, true);
	      break;
	    }
          //case K_S_SPACE:
            c = ' ';
            // FALTHROUGH

	  case 'l':
	  case ' ':
	    nv_right(ca);
	    break;

	  case K_LEFT:
	    if ((G.mod_mask & MOD_MASK_CTRL) != 0)
	    {
	      nv_bck_word(ca, true);
	      break;
	    }
	  case 'h':
	    dont_adjust_op_end = nv_left(ca);
	    break;

          //case K_S_BS:
            //c = K_BS;
            // FALTHROUGH

	  case K_BS:
	  case 0x1f & (int)('H'):	// Ctrl
	    if (G.VIsual_active && G.VIsual_select)
	    {
	      ca.cmdchar = 'x';	// BS key behaves like 'x' in Select mode
	      v_visop(ca);
	    }
	    else
	      dont_adjust_op_end = nv_left(ca);
	    break;

	  case '-':
	  case K_KMINUS:
	    flag = true;
	    // FALLTHROUGH

	  case 'k':
	  case K_UP:
	  case 0x1f & (int)('P'):	// Ctrl
	    oap.motion_type = MLINE;
	    if (Edit.cursor_up(ca.count1, oap.op_type == OP_NOP) == FAIL)
	      clearopbeep(oap);
	    else if (flag)
	      Edit.beginline(BL_WHITE | BL_FIX);
	    break;

	  case '+':
	  case K_KPLUS:
	  case CR:
	  case K_KENTER:
	    flag = true;
	    // FALLTHROUGH

	  case 'j':
	  case K_DOWN:
	  case 0x1f & (int)('N'):	// Ctrl
	  case NL:	// 0x1f & (int)('J')
	    oap.motion_type = MLINE;
	    //if (G.curwin.cursor_down(ca.count1, oap.op_type == OP_NOP) == FAIL)
	    if (Edit.cursor_down(ca.count1, oap.op_type == OP_NOP) == FAIL)
	      clearopbeep(oap);
	    else if (flag)
	      Edit.beginline(BL_WHITE | BL_FIX);
	    break;

	    //
	    // This is a strange motion command that helps make operators more
	    // logical. It is actually implemented, but not documented in the
	    // real Vi. This motion command actually refers to
	    // "the current line".
	    // Commands like "dd" and "yy" are really an alternate form of
	    // "d_" and "y_". It does accept a count, so "d3_" works to
	    // delete 3 lines.
	    //
	  case '_':
	    nv_lineop(ca);
	    break;

	  case K_HOME:
	  // case K_KHOME:
	  // case K_XHOME:
	  case K_S_HOME:
	    if ((G.mod_mask & MOD_MASK_CTRL) != 0) // CTRL-HOME = goto line 1
	    {
	      nv_goto(ca, 1);
	      break;
	    }
	    ca.count0 = 1;
	    // FALLTHROUGH

	  case '|':
	    nv_pipe(ca);
	    break;

	    //
	    // Word Motions
	    //
	  case 'B':
	    type = true;
	    // FALLTHROUGH

	  case 'b':
	  case K_S_LEFT:
	    nv_bck_word(ca, type);
	    break;

	  case 'E':
	    type = true;
	    // FALLTHROUGH

	  case 'e':
	    oap.inclusive = true;
	    nv_wordcmd(ca, type);
	    break;

	  case 'W':
	    type = true;
	    // FALLTHROUGH

	  case 'w':
	  case K_S_RIGHT:
	    oap.inclusive = false;
	    nv_wordcmd(ca, type);
	    break;

	  case K_END:
	  // case K_KEND:
	  // case K_XEND:
	  case K_S_END:
	    if ((G.mod_mask & MOD_MASK_CTRL) != 0) {
	      // CTRL-END = goto last line
	      // nv_goto(oap, curbuf.b_ml.ml_line_count);
	      nv_goto(ca, G.curwin.getLineCount());
	    }
	    // FALLTHROUGH

	  case '$':
	    nv_dollar(ca);
	    break;

	  case '^':
	    intflag = BL_WHITE | BL_FIX;		// XXX
	    // FALLTHROUGH

	  case '0':
	    oap.motion_type = MCHAR;
	    oap.inclusive = false;
	    Edit.beginline(intflag);
	    break;

	    /*
	     * 4: Searches
	     */
	  case K_KDIVIDE:
	    ca.cmdchar = '/';
	    // FALLTHROUGH
	  case '?':
	  case '/':
            if(ca.nchar != 0)
                nv_search_finish(ca);
            else {
                nv_search(ca, searchbuff, false);
                return;
            }
	    break;

	  case 'N':
	    intflag = SEARCH_REV;
	    // FALLTHROUGH

	  case 'n':
	    nv_next(ca, intflag);
	    break;

	    //
	    // Character searches
	    //
	  case 'T':
	    dir = BACKWARD;
	    // FALLTHROUGH

	  case 't':
	    nv_csearch(ca, dir, true);
	    break;

	  case 'F':
	    dir = BACKWARD;
	    // FALLTHROUGH

	  case 'f':
	    nv_csearch(ca, dir, false);
	    break;

	  case ',':
	    intflag = 1;		// XXX why not dir??
	    // FALLTHROUGH

	  case ';':
	    // ca.nchar == NUL, thus repeat previous search
	    nv_csearch(ca, intflag, false);
	    break;

	    //
	    // section or C function searches
	    //
	  case '[':
	    dir = BACKWARD;
	    // FALLTHROUGH

	  case ']':
	    notSup("bracket commands");
	    nv_brackets(ca, dir);
	    break;

	  case '%':
	    nv_percent(ca);
	    break;

	  case '(':
	    dir = BACKWARD;
	    // FALLTHROUGH

	  case ')':
	    nv_brace(ca, dir);
	    break;

	  case '{':
	    dir = BACKWARD;
	    // FALLTHROUGH

	  case '}':
	    nv_findpar(ca, dir);
	    break;

	    /*
	     * 5: Edits
	     */
	  case '.':    /* redo command */
	    if (!checkclearopq(oap))
	    {
	      /*
	       * if restart_edit is TRUE, the last but one command is repeated
	       * instead of the last command (inserting text). This is used for
	       * CTRL-O <.> in insert mode
	       */
	      if (GetChar.start_redo(ca.count0,
                                    G.restart_edit != 0 && !arrow_used) == FAIL)
		clearopbeep(oap);
	    }
	    break;

	  case 'u':    /* undo */
	    if (G.VIsual_active || oap.op_type == OP_LOWER)
	    {
	      /* translate "<Visual>u" to "<Visual>gu" and "guu" to "gugu" */
	      ca.cmdchar = 'g';
	      ca.nchar = 'u';
	      nv_operator(ca);
	      break;
	    }
	    /* FALLTHROUGH */

	  case K_UNDO:
	    if (!checkclearopq(oap))
	    {
	      Misc.u_undo((int)ca.count1);
	      G.curwin.setWSetCurswant(true);
	    }
	    break;

	  case 0x1f & (int)('R'):	/* undo undo */	// Ctrl
	    if (!checkclearopq(oap))
	    {
	      Misc.u_redo((int)ca.count1);
	      G.curwin.setWSetCurswant(true);
	    }
	    break;

	  case 'U':		/* Undo line */
	    nv_Undo(ca);
	    break;

	  case 'r':		/* replace character */
	    nv_replace(ca);
	    break;

	  case 'J':
	    nv_join(ca);
	    break;

	  case 'P':
	  case 'p':
	    nv_put(ca);
	    break;

	  case 0x1f & (int)('A'):	    /* add to number */	// Ctrl
	  case 0x1f & (int)('X'):	    /* subtract from number */	// Ctrl
	    notSup("add/sub");
	    if (!checkclearopq(oap) && do_addsub((int)ca.cmdchar, ca.count1) == OK)
	      prep_redo_cmd(ca);
	    break;

	    /*
	     * 6: Inserts
	     */
	  case 'A':
	  case 'a':
	  case 'I':
	  case 'i':
	  case K_INS:
	    nv_edit(ca);
	    break;

	  case 'o':
	  case 'O':
	    if (G.VIsual_active)  /* switch start and end of visual */
	      v_swap_corners(ca);
	    else
	      n_opencmd(ca);
	    break;

	  case 'R':
	    nv_Replace(ca);
	    break;

	    /*
	     * 7: Operators
	     */
	  case '~':	    /* swap case */
	    /*
	     * if tilde is not an operator and Visual is off: swap case
	     * of a single character
	     */
	    if (	   ! G.p_to.getBoolean()
			   && !G.VIsual_active
			   && oap.op_type != OP_TILDE)
	    {
	      n_swapchar(ca);
	      break;
	    }
	    /*FALLTHROUGH*/

	  case 'd':
	  case 'c':
	  case 'y':
	  case '>':
	  case '<':
	  case '!':
	  case '=':
	    if(Util.vim_strchr("!=", ca.cmdchar) != null) {
	      notImp("oper");
	    }
	    nv_operator(ca);
	    break;

	    /*
	     * 8: Abbreviations
	     */

	  case 'S':
	  case 's':
	    if (G.VIsual_active)	/* "vs" and "vS" are the same as "vc" */
	    {
	      if (ca.cmdchar == 'S')
		G.VIsual_mode = 'V';
	      ca.cmdchar = 'c';
	      nv_operator(ca);
	      break;
	    }
	    /* FALLTHROUGH */
	  case K_DEL:
	  case 'Y':
	  case 'D':
	  case 'C':
	  case 'x':
	  case 'X':
	    if (ca.cmdchar == K_DEL)
	      ca.cmdchar = 'x';		/* DEL key behaves like 'x' */

	    /* with Visual these commands are operators */
	    if (G.VIsual_active)
	    {
	      v_visop(ca);
	      break;
	    }
	    /* FALLTHROUGH */

	  case '&':
	    if(ca.cmdchar == '&') notSup("&");	// XXX
	    nv_optrans(ca);
	    opnum = 0;
	    break;

	    /*
	     * 9: Marks
	     */
	  case 'm':
	    if (!checkclearop(oap))
	    {
	      if (MarkOps.setmark(ca.nchar, ca.count0) == FAIL)
		clearopbeep(oap);
	    }
	    break;

	  case '\'':
	    flag = true;
	    /* FALLTHROUGH */

	  case '`':
	    nv_gomark(ca, flag);
	    break;

	  case 0x1f & (int)('O'):	// Ctrl
	    /* switch from Select to Visual mode for one command */
	    if (G.VIsual_active && G.VIsual_select)
	    {
	      G.VIsual_select = false;
	      Misc.showmode();
	      restart_VIsual_select = 2;
	      break;
	    }
	    nv_pcmark(JLOP.PREV_JUMP, ca);
            break;

	  case 0x1f & (int)('I'):	// Ctrl	/* goto newer pcmark */
          case K_TAB:
	    nv_pcmark(JLOP.NEXT_JUMP, ca);
	    break;

	    /*
	     * 10. Register name setting
	     */
	  case '"':
	    MutableInt mi = new MutableInt(opnum);
	    nv_regname(ca, mi);
	    opnum = mi.getValue();
	    break;

	    /*
	     * 11. Visual
	     */
	  case 'v':
	  case 'V':
	  //case 0x1f & (int)('V'):	// Ctrl // DISABLE ON RELEASE
	    if (!checkclearop(oap))
                nv_visual(ca, false);
	    break;

	    /*
	     * 12. Suspend
	     */

	  case 0x1f & (int)('Z'):	// Ctrl
	    notSup("ctrl-z");
	    clearop(oap);
	    if (G.VIsual_active)
	      end_visual_mode();		    /* stop Visual */
	    GetChar.stuffReadbuff(":st\r");   /* with autowrite */
	    break;

	    /*
	     * 13. Window commands
	     */

	  case 0x1f & (int)('W'):	// Ctrl
	    if (!checkclearop(oap))
	      Misc.do_window(ca.nchar, ca.count0); // everything is in window.c
	    break;

	    /*
	     *   14. extended commands (starting with 'g')
	     */
	  case 'g':
	    nv_g_cmd(ca, searchbuff);
	    break;

	    /*
	     * 15. mouse click
	     * == REMOVED ==
	     */

	    //
	    // 16. scrollbar movement
	    // == REMOVED ==
	    //

	    //
	    // case K_SELECT:	    /* end of Select mode mapping */
	    //     nv_select(ca);
	    //     break;

	    /*
	     * 17. The end
	     */
	    /* CTRL-\ CTRL-N goes to Normal mode: a no-op */
	  case 0x1f & (int)('\\'):	// Ctrl
	    notSup("normal");
	    nv_normal(ca);
	    break;

	  case 0x1f & (int)('C'):	// Ctrl
	    notSup("ctrl-c");
	    G.restart_edit = 0;
	    /*FALLTHROUGH*/

	  case ESC:
	    nv_esc(ca, opnum);
	    break;

	  default:			/* not a known command */
	    clearopbeep(oap);
	    break;

	}	/* end of switch on command character */

        /*
        * if we didn't start or finish an operator, reset oap.regname, unless we
        * need it later.
        */
        if (!G.finish_op && oap.op_type == 0 &&
            Util.vim_strchr("\"DCYSsXx.", ca.cmdchar) == null)
          oap.regname = 0;

        /*
        * If an operation is pending, handle it...
        */
        do_pending_operator(ca, searchbuff,
                            null, old_col, false, dont_adjust_op_end);
      } catch(NotSupportedException e) {
	clearopbeep(oap);
      }

      /*
       * Wait when a message is displayed that will be overwritten by the mode
       * ...... VISUAL MODE stuff for pause about showing message
       */
    } while(false);
// normal_end: this target replace by break from do {} while(0)

    /*
     * Finish up after executing a Normal mode command.
     */

    msg_nowait = false;

    boolean wasFinishOp = G.finish_op;
    G.finish_op = false;
    /* Redraw the cursor with another shape, if we were in Operator-pending
     * mode or did a replace command. */
    if (wasFinishOp || ca.cmdchar == 'r')
      Misc.ui_cursor_shape();		/* may show different cursor shape */

    // NEEDSWORK: The following is kind of like what vim does in screen.c
    // (not a jVi file). Maybe make redraw_cmdline global and put this stuff
    // in the exit from keystroke area in getchar
    if(G.clear_cmdline || redraw_cmdline) {
        G.clear_cmdline = false;
        redraw_cmdline = false;
        Misc.showmode();
    }
    if (oap.op_type == OP_NOP && oap.regname == 0)
      clear_showcmd();

    // if (modified) ....

    MarkOps.checkpcmark();	/* check if we moved since setting pcmark */

    // vim_free(searchbuff);
    // SCROLLBIND stuff
    // May restart edit() ..... VISUAL MODE STUFF DELETED

    newChunk = true;
  }

  /**
   * Handle an operator after visual mode or when the movement is finished
   */
  static void do_pending_operator(CMDARG cap,
			   CharBuf searchbuff,
			   MutableBoolean command_busy,
			   int old_col,
			   boolean gui_yank,
			   boolean dont_adjust_op_end)
  throws NotSupportedException
  {
    OPARG	oap = cap.oap;
    FPOS	old_cursor;
    boolean	empty_region_error;


//#if defined(USE_CLIPBOARD) && !defined(MSWIN)
    /*
     * Yank the visual area into the GUI selection register before we operate
     * on it and lose it forever.  This could call do_pending_operator()
     * recursively, but that's OK because gui_yank will be TRUE for the
     * nested call.  Note also that we call clip_copy_selection() and not
     * clip_auto_select().  This is because even when 'autoselect' is not set,
     * if we operate on the text, eg by deleting it, then this is considered to
     * be an explicit request for it to be put in the global cut buffer, so we
     * always want to do it here. -- webb
     */
    /* MSWINDOWS: don't do this, there is no automatic copy to the clipboard */
    /* Don't do it if a specific register was specified, so that ""x"*P works */
//    if (clipboard.available
//    && oap->op_type != OP_NOP
//    && !gui_yank
//    && VIsual_active
//    && oap->regname == 0
//    && !redo_VIsual_busy)
//clip_copy_selection();
//#endif
    // old_cursor = G.curwin.getWCursor();

    /*
     * If an operation is pending, handle it...
     */
    if ((G.VIsual_active || G.finish_op) && oap.op_type != OP_NOP) {
      do_xop("do_pending_operator");
      oap.is_VIsual = G.VIsual_active;

      old_cursor = (FPOS)G.curwin.getWCursor().copy(); // this was outside the if, look above

      /* only redo yank when 'y' flag is in 'cpoptions' */
      if ((Util.vim_strchr(G.p_cpo, CPO_YANK) != null || oap.op_type != OP_YANK)
	  && !G.VIsual_active)
      {
	prep_redo(oap.regname, cap.count0,
		  get_op_char(oap.op_type), get_extra_op_char(oap.op_type),
		  cap.cmdchar, cap.nchar);
	if (cap.cmdchar == '/' || cap.cmdchar == '?') /* was a search */
	{
	  /*
	   * If 'cpoptions' does not contain 'r', insert the search
	   * pattern to really repeat the same command.
	   */
	  if (Util.vim_strchr(G.p_cpo, CPO_REDO) == null)
	    { GetChar.AppendToRedobuff(searchbuff.toString()); }
	  GetChar.AppendToRedobuff(NL_STR);
	}
      }

      if (G.redo_VIsual_busy)
      {
          oap.start = (FPOS)G.curwin.getWCursor().copy();
          int line = oap.start.getLine()+ redo_VIsual_line_count - 1;
          if(line > G.curwin.getLineCount())
              line = G.curwin.getLineCount();
          G.curwin.setCaretPosition(
                    line, Misc.check_cursor_col(line, oap.start.getColumn()));
          G.VIsual_mode = redo_VIsual_mode;
          if (G.VIsual_mode == 'v')
          {
              if (redo_VIsual_line_count <= 1)
                  G.curwin.setCaretPosition(oap.start.getLine(),
                                            oap.start.getColumn() + redo_VIsual_col - 1);
              else
                  G.curwin.setCaretPosition(oap.start.getLine(), redo_VIsual_col);
          }
          if (redo_VIsual_col == MAXCOL)
          {
              G.curwin.setWCurswant(MAXCOL);
              Misc.coladvance(MAXCOL);
          }
          cap.count0 = redo_VIsual_count;
          if (redo_VIsual_count != 0)
              cap.count1 = redo_VIsual_count;
          else
              cap.count1 = 1;
      } else if (G.VIsual_active) {
            /* In Select mode, a linewise selection is operated upon like a
             * characterwise selection. */
          /*if (G.VIsual_select && G.VIsual_mode == 'V') {
              if (lt(VIsual, curwin->w_cursor)) {
                  VIsual.col = 0;
                  curwin->w_cursor.col =
                          STRLEN(ml_get(curwin->w_cursor.lnum));
              } else {
                  curwin->w_cursor.col = 0;
                  VIsual.col = STRLEN(ml_get(VIsual.lnum));
              }
              VIsual_mode = 'v';
          }*/
          /* If 'selection' is "exclusive", backup one character for
          * charwise selections. */
          if (!G.VIsual_select && G.VIsual_mode == 'v')
              unadjust_for_sel();

          /* Save the current VIsual area for '< and '> marks, and "gv" */
          G.curwin.setMarkOffset((ViMark)G.curbuf.b_visual_start,
                                 G.VIsual.getOffset(), false);
          G.curwin.setMarkOffset((ViMark)G.curbuf.b_visual_end,
                                 G.curwin.getWCursor().getOffset(), false);
          G.curbuf.b_visual_mode = G.VIsual_mode;

          oap.start = G.VIsual;
          if (G.VIsual_mode == 'V')
              oap.start.setPosition(oap.start.getLine(), 0);
      }

      /*
       * Set oap.start to the first position of the operated text, oap.end
       * to the end of the operated text.  w_cursor is equal to oap.start.
       */
	     // (lt(oap.start, curwin.w_cursor))
      if (oap.start.compareTo(G.curwin.getWCursor()) < 0) {
	oap.end = (FPOS)G.curwin.getWCursor().copy();
	G.curwin.getWindow().setWCursor(oap.start);
      }
      else
      {
	oap.end = (FPOS)oap.start.copy();
	oap.start = (FPOS)G.curwin.getWCursor().copy();
      }
      oap.line_count = oap.end.getLine() - oap.start.getLine() + 1;

      if (G.VIsual_active || G.redo_VIsual_busy) {
          if (G.VIsual_mode == Util.ctrl('V'))  /* block mode */
          {
              //colnr_t start, end;

              oap.block_mode = true;
//            getvcol(curwin, &(oap->start),
//                    &oap->start_vcol, NULL, &oap->end_vcol);
//            if (!redo_VIsual_busy)
//            {
//                getvcol(curwin, &(oap->end), &start, NULL, &end);
//                if (start < oap->start_vcol)
//                    oap->start_vcol = start;
//                if (end > oap->end_vcol)
//                {
//                    if (*p_sel == 'e' && start - 1 >= oap->end_vcol)
//                        oap->end_vcol = start - 1;
//                    else
//                        oap->end_vcol = end;
//                }
//            }
//
//            /* if '$' was used, get oap->end_vcol from longest line */
//            if (curwin->w_curswant == MAXCOL)
//            {
//                curwin->w_cursor.col = MAXCOL;
//                oap->end_vcol = 0;
//                for (curwin->w_cursor.lnum = oap->start.lnum;
//                        curwin->w_cursor.lnum <= oap->end.lnum;
//                        ++curwin->w_cursor.lnum)
//                {
//                    getvcol(curwin, &curwin->w_cursor, NULL, NULL, &end);
//                    if (end > oap->end_vcol)
//                        oap->end_vcol = end;
//                }
//            }
//            else if (redo_VIsual_busy)
//                oap->end_vcol = oap->start_vcol + redo_VIsual_col - 1;
//            /*
//             * Correct oap->end.col and oap->start.col to be the
//             * upper-left and lower-right corner of the block area.
//             */
//            curwin->w_cursor.lnum = oap->end.lnum;
//            coladvance(oap->end_vcol);
//            oap->end = curwin->w_cursor;
//            curwin->w_cursor = oap->start;
//            coladvance(oap->start_vcol);
//            oap->start = curwin->w_cursor;
          }
          if (!G.redo_VIsual_busy && !gui_yank)
          {
              /*
               * Prepare to reselect and redo Visual: this is based on the
               * size of the Visual text
               */
              resel_VIsual_mode = G.VIsual_mode;
              if (G.curwin.getWCurswant() == MAXCOL)
                  resel_VIsual_col = MAXCOL;
              else if (G.VIsual_mode == Util.ctrl('V'))
                  resel_VIsual_col = oap.end_vcol - oap.start_vcol + 1;
              else if (oap.line_count > 1)
                  resel_VIsual_col = oap.end.getColumn();
              else
                  resel_VIsual_col = oap.end.getColumn() - oap.start.getColumn() + 1;
              resel_VIsual_line_count = oap.line_count;
          }

          /* can't redo yank (unless 'y' is in 'cpoptions') and ":" */
          if ((Util.vim_strchr(G.p_cpo, CPO_YANK) != null || oap.op_type != OP_YANK)
                  && oap.op_type != OP_COLON)
          {
              prep_redo(oap.regname, 0, NUL, 'v', get_op_char(oap.op_type),
                      get_extra_op_char(oap.op_type));
              redo_VIsual_mode = resel_VIsual_mode;
              redo_VIsual_col = resel_VIsual_col;
              redo_VIsual_line_count = resel_VIsual_line_count;
              redo_VIsual_count = cap.count0;
          }

          /*
           * oap->inclusive defaults to TRUE.
           * If oap->end is on a NUL (empty line) oap->inclusive becomes
           * FALSE.  This makes "d}P" and "v}dP" work the same.
           */
          oap.inclusive = true;
          if (G.VIsual_mode == 'V')
              oap.motion_type = MLINE;
          else {
              oap.motion_type = MCHAR;
              if (G.VIsual_mode != Util.ctrl('V') && oap.end.getColumn() > MAXCOL) {
                  oap.inclusive = false;
                  /* Try to include the newline, unless it's an operator
                  * that works on lines only */
                  if (G.p_sel.charAt(0) != 'o'
                            && !op_on_lines(oap.op_type)
                            && oap.end.getLine() < G.curwin.getLineCount())
                  {
                      oap.end.setPosition(oap.end.getLine()+1, 0);
                      oap.line_count++;
                  }
              }
          }
	  G.redo_VIsual_busy = false;
            /*
            * Switch Visual off now, so screen updating does
            * not show inverted text when the screen is redrawn.
            * With OP_YANK and sometimes with OP_COLON and OP_FILTER there is
            * no screen redraw, so it is done here to remove the inverted
            * part.
            */
          if (!gui_yank) {
              G.VIsual_active = false;
              G.curwin.updateVisualState();
              if (G.p_smd.value)
                  G.clear_cmdline = true;   /* unshow visual mode later */
              if (oap.op_type == OP_YANK || oap.op_type == OP_COLON ||
                      oap.op_type == OP_FILTER)
                  update_curbuf(NOT_VALID);
            }
      }


      G.curwin.setWSetCurswant(true);

      /*
       * oap.empty is set when start and end are the same.  The inclusive
       * flag affects this too, unless yanking and the end is on a NUL.
       */
      oap.empty = (oap.motion_type == MCHAR
	       && (!oap.inclusive
		   || (oap.op_type == OP_YANK
		       && Misc.gchar_pos(oap.end) == '\n'))
	       && oap.start.equals(oap.end));
      /*
       * For delete, change and yank, it's an error to operate on an
       * empty region, when 'E' inclucded in 'cpoptions' (Vi compatible).
       */
      empty_region_error = (oap.empty
			    && Util.vim_strchr(G.p_cpo, CPO_EMPTYREGION) != null);

      /* Force a redraw when operating on an empty Visual region */
//       if (oap.is_VIsual && oap.empty)
// 	redraw_curbuf_later(NOT_VALID);

      /*
       * If the end of an operator is in column one while oap.motion_type
       * is MCHAR and oap.inclusive is FALSE, we put op_end after the last
       * character in the previous line. If op_start is on or before the
       * first non-blank in the line, the operator becomes linewise
       * (strange, but that's the way vi does it).
       */
      if (	   oap.motion_type == MCHAR
		   && oap.inclusive == false
		   && !dont_adjust_op_end
		   && oap.end.getColumn() == 0
		   && (!oap.is_VIsual || G.p_sel.charAt(0) == 'o')
		   && oap.line_count > 1)
      {
	oap.end_adjusted = true;	    /* remember that we did this */
	--oap.line_count;
	int new_line = oap.end.getLine() - 1;
        int new_col = 0;
	if (Misc.inindent(0))
	  oap.motion_type = MLINE;
	else
	{
          new_col = Util.lineLength(new_line);
	  if (new_col != 0)
	  {
            --new_col;
	    oap.inclusive = true;
	  }
	}
        oap.end.setPosition(new_line, new_col);
      }
      else
	oap.end_adjusted = false;

      switch (oap.op_type)
      {
	case OP_LSHIFT:
        case OP_RSHIFT:
          Misc.beginUndo();
          try {
            Misc.op_shift(oap, true, oap.is_VIsual ? (int)cap.count1 : 1);
          } finally {
            Misc.endUndo();
          }
	  break;

	case OP_JOIN_NS:
	case OP_JOIN:
	  if (oap.line_count < 2) {
	    oap.line_count = 2;
	  }
	      // .... -1 > curbuf.b_ml.ml_line_count...
	  if (G.curwin.getWCursor().getLine() + oap.line_count - 1 >
	      G.curwin.getLineCount()) {
	    Util.beep_flush();
	  } else {
            Misc.beginUndo();
            try {
              Misc.do_do_join(oap.line_count, oap.op_type == OP_JOIN, true);
            } finally {
              Misc.endUndo();
            }
	  }
	  break;

	case OP_DELETE:
	  G.VIsual_reselect = false;	    /* don't reselect now */
	  if (empty_region_error)
	    Util.vim_beep();
	  else {
            Misc.beginUndo();
            try {
              Misc.op_delete(oap);
            } finally {
              Misc.endUndo();
            }
	  }
	  break;

	case OP_YANK:
	  if (empty_region_error)
	  {
	    if (!gui_yank)
	      Util.vim_beep();
	  }
	  else
	    Misc.op_yank(oap, false, !gui_yank);
	  Misc.check_cursor_col();
	  break;

	case OP_CHANGE:
	  G.VIsual_reselect = false;	    /* don't reselect now */
	  if (empty_region_error)
	    Util.vim_beep();
	  else
	  {
	    // don't restart edit after typing <Esc> in edit()
	    G.restart_edit = 0;
	    Misc.beginInsertUndo();
            Misc.op_change(oap); // will call edit()
	  }
	  break;

	case OP_FILTER:
	  if (Util.vim_strchr(G.p_cpo, CPO_FILTER) != null)
	    GetChar.AppendToRedobuff("!\r");  /* use any last used !cmd */
	  else
	    bangredo = true;    /* do_bang() will put cmd in redo buffer */

	case OP_INDENT:
	case OP_COLON:

	  op_colon(oap);
	  break;

	case OP_TILDE:
	case OP_UPPER:
	case OP_LOWER:
	case OP_ROT13:
	  if (empty_region_error)
	    Util.vim_beep();
	  else {
            Misc.beginUndo();
            try {
              Misc.op_tilde(oap);
              Misc.check_cursor_col();
            } finally {
              Misc.endUndo();
            }
          }
	  break;

	case OP_FORMAT:
	  if (G.p_fp != null)
	    op_colon(oap);		/* use external command */
	  else
	    op_format(oap);		/* use internal function */
	  break;

	case OP_INSERT:
	case OP_APPEND:
	  G.VIsual_reselect = false;	/* don't reselect now */
	  Util.vim_beep();
	  break;

	case OP_REPLACE:
	  G.VIsual_reselect = false;	/* don't reselect now */
	  Util.vim_beep();
	  break;

	default:
	  clearopbeep(oap);
      }
      if (!gui_yank)
      {
	/*
	 * if 'sol' not set, go back to old column for some commands
	 */
	if (G.p_notsol.getBoolean() && oap.motion_type == MLINE
            && !oap.end_adjusted
	    && (oap.op_type == OP_LSHIFT || oap.op_type == OP_RSHIFT
		|| oap.op_type == OP_DELETE)) {
	  G.curwin.setWCurswant(old_col);
	  Misc.coladvance(old_col);
	}
	oap.op_type = OP_NOP;
      }
      else {
	G.curwin.getWindow().setWCursor(old_cursor);
      }
      oap.block_mode = false;
      oap.regname = 0;
    }
  }
  
  static private  void	op_colon (OPARG oap) throws NotSupportedException {
      do_op("op_colon");
      StringBuffer range = new StringBuffer();
      if(oap.is_VIsual) {
        range.append("'<,'>");
      } else {
	//
	// Make the range look nice, so it can be repeated.
	//
	if (oap.start.getLine() == G.curwin.getWCursor().getLine())
          range.append('.');
	else
          range.append(oap.start.getLine());
	if (oap.end.getLine() != oap.start.getLine())
	{
          range.append(',');
          if (oap.end.getLine() == G.curwin.getWCursor().getLine())
            range.append('.');
          else if (oap.end.getLine() == G.curwin.getLineCount())
            range.append('$');
          else if (oap.start.getLine() == G.curwin.getWCursor().getLine())
          {
            range.append(".+");
            range.append(oap.line_count - 1);
          }
          else
            range.append(oap.end.getLine());
      }
    }
    if (oap.op_type != OP_COLON) {
      notImp("op_colon '!'");
      range.append("!");
    }
    if (oap.op_type == OP_INDENT)
    {
      notImp("op_colon OP_INDENT");
//#ifndef CINDENT
//	if (*p_ep == NUL)
//	    stuffReadbuff((char_u *)"indent");
//	else
//#endif
//	    stuffReadbuff(p_ep);
//	stuffReadbuff((char_u *)"\n");
    }
    else if (oap.op_type == OP_FORMAT)
    {
      notImp("op_colon OP_FORMAT");
//	if (*p_fp == NUL)
//	    stuffReadbuff((char_u *)"fmt");
//	else
//	    stuffReadbuff(p_fp);
//	stuffReadbuff((char_u *)"\n");
    }
      
    ColonCommands.doColonCommand(range);
  }

  static void end_visual_mode() {
      do_op("end_visual_mode");
// #ifdef USE_CLIPBOARD
    /*
     * If we are using the clipboard, then remember what was selected in case
     * we need to paste it somewhere while we still own the selection.
     * Only do this when the clipboard is already owned.  Don't want to grab
     * the selection when hitting ESC.
     */
//    if (clipboard.available && clipboard.owned)
//        clip_auto_select();
//#endif

    G.VIsual_active = false;
//#ifdef USE_MOUSE
//    setmouse();
//    mouse_dragging = 0;
//#endif

    /* Save the current VIsual area for '< and '> marks, and "gv" */
    G.curwin.setMarkOffset((ViMark)G.curbuf.b_visual_start,
                           G.VIsual.getOffset(), false);
    G.curwin.setMarkOffset((ViMark)G.curbuf.b_visual_end,
                           G.curwin.getWCursor().getOffset(), false);
    G.curbuf.b_visual_mode = G.VIsual_mode;

    if (G.p_smd.value)
        G.clear_cmdline = true;/* unshow visual mode later */
    G.curwin.updateVisualState();

    /* Don't leave the cursor past the end of the line */
    if (G.curwin.getWCursor().getColumn() > 0 && Util.getChar() == '\n')
        G.curwin.setCaretPosition(G.curwin.getCaretPosition() -1);
  }
  /**
   * 
   * <p>
   * This is the vim comment.
   * Find the identifier under or to the right of the cursor.  If none is
   * found and find_type has FIND_STRING, then find any non-white string.  The
   * length of the string is returned, or zero if no string is found.  If a
   * string is found, a pointer to the string is put in *string, but note that
   * the caller must use the length returned as this string may not be NUL
   * terminated.
   * <p>
   * @param mi use this to return the length of the match, 0 if none
   * @param find_type mask of FIND_IDENT, FIND_STRING
   * @return a segment with its CharacterIterator initialized to found string,
   * or null if no match.
   */
  static Segment find_ident_under_cursor(MutableInt mi, int find_type) {
    int	    col = 0;	    // init to shut up GCC
    int	    i;

    //
    // if i == 0: try to find an identifier
    // if i == 1: try to find any string
    //
    Segment seg = Util.ml_get_curline();
    for (i = ((find_type & FIND_IDENT) != 0) ? 0 : 1;	i < 2; ++i)
    {
      //
      // skip to start of identifier/string
      ///
      col = G.curwin.getWCursor().getColumn();
      while (seg.array[col + seg.offset] != '\n'
             && (i == 0
                 ? !Misc.vim_iswordc(seg.array[col + seg.offset])
                 : Misc.vim_iswhite(seg.array[col + seg.offset])))
        ++col;

      //
      // Back up to start of identifier/string. This doesn't match the
      // real vi but I like it a little better and it shouldn't bother
      // anyone.
      // When FIND_IDENT isn't defined, we backup until a blank.
      ///
      while (col > 0
             && (i == 0
                ? Misc.vim_iswordc(seg.array[col - 1 + seg.offset])
                : (!Misc.vim_iswhite(seg.array[col - 1 + seg.offset])
                   && (!((find_type & FIND_IDENT) != 0)
                       || !Misc.vim_iswordc(seg.array[col - 1 + seg.offset])))))
        --col;

      //
      // if we don't want just any old string, or we've found an identifier,
      // stop searching.
      ///
      if (!((find_type & FIND_STRING) != 0)
          || Misc.vim_iswordc(seg.array[col + seg.offset]))
        break;
    }
    //
    // didn't find an identifier or string
    ///
    if (seg.array[col + seg.offset] == '\n'
        || (!Misc.vim_iswordc(seg.array[col + seg.offset]) && i == 0))
    {
      if ((find_type & FIND_STRING) != 0)
        Msg.emsg("No string under cursor");
      else
        Msg.emsg("No identifier under cursor");
      mi.setValue(0); // length of match
      return null;
    }
    // ptr += col;
    // *string = ptr;
    //mi.setValue(G.curwin.getLineStartOffset(G.curwin.getWCursor().getLine()) + col);
    seg.setIndex(seg.offset + col);

    int len = 0;
    while (i == 0
           ? Misc.vim_iswordc(seg.array[len + col + seg.offset])
           : (seg.array[len + col + seg.offset] != '\n'
              && !Misc.vim_iswhite(seg.array[len + col + seg.offset])))
    {
      ++len;
    }
    mi.setValue(len);
    return seg;
  }

  /**
   * Prepare for redo of a normal command.
   */
  static private void prep_redo_cmd(CMDARG cap) {
    prep_redo(cap.oap.regname, cap.count0,
	      NUL, cap.cmdchar, NUL, cap.nchar);
  }

  /**
   * Prepare for redo of any command.
   */
  static private void prep_redo(int regname, int num,
			 int cmd1, int cmd2, int cmd3, int cmd4) {
    GetChar.ResetRedobuff();
    if (regname != 0) {	/* yank from specified buffer */
      GetChar.AppendCharToRedobuff('"');
      GetChar.AppendCharToRedobuff(regname);
    }
    if (num != 0) {
      GetChar.AppendNumberToRedobuff(num);
    }

    if (cmd1 != NUL) { GetChar.AppendCharToRedobuff(cmd1); }
    if (cmd2 != NUL) { GetChar.AppendCharToRedobuff(cmd2); }
    if (cmd3 != NUL) { GetChar.AppendCharToRedobuff(cmd3); }
    if (cmd4 != NUL) { GetChar.AppendCharToRedobuff(cmd4); }
  }

  /**
   * check for operator active and clear it
   *
   * @return TRUE if operator was active
   */
  static private  boolean checkclearop(OPARG oap) {
    do_xop("checkclearop");
    if (oap.op_type == OP_NOP)
      return false;
    clearopbeep(oap);
    return true;
  }

  /**
   * check for operator or Visual active and clear it
   *
   * @return TRUE if operator was active
   */
  static private boolean checkclearopq(OPARG oap) {
    do_xop("checkclearopq");
    if (oap.op_type == OP_NOP && !G.VIsual_active)
      return false;
    clearopbeep(oap);
    return true;
  }

  static private void clearop(OPARG oap) {
    do_xop("clearop");
    oap.clearop();
  }

  /* *******************************
  static void clearopInstance() {
    oap.clearop();
  }
  ********************************/

  static private void clearopbeep(OPARG oap) {
    do_xop("clearopbeep");
    clearop(oap);
    Util.beep_flush();
  }

  //
  // Routines for displaying a partly typed command
  //

  static StringBuffer showcmd_buf = new StringBuffer();
  static StringBuffer old_showcmd_buf = new StringBuffer();
  static boolean showcmd_is_clear = true;
  // static char_u old_showcmd_buf[SHOWCMD_COLS + 1];  /* For push_showcmd() */

  static void clear_showcmd() {
    do_xop("clear_showcmd");

    if (!G.p_sc.getBoolean())
      return;

    showcmd_buf.setLength(0);

    /*
     * Don't actually display something if there is nothing to clear.
     */
    if (showcmd_is_clear)
      return;

    display_showcmd();
  }

  /**
   * Add 'c' to string of shown command chars.
   * Return TRUE if output has been written (and setcursor() has been called).
   */
  static boolean add_to_showcmd(int c) {

    do_xop("add_to_showcmd");

    if (!G.p_sc.getBoolean())
      return false;
    if((c & 0xf000) == VIRT)
        return false;

    add_one_to_showcmd_buffer(c);

    if (GetChar.char_avail())
      return false;

    display_showcmd();

    return true;
  }

  /** Add char to show commmand buffer. */
  static private void add_one_to_showcmd_buffer(int c) {
    String  p;
    int	    old_len;
    int	    extra_len;
    int	    overflow;

    p = Misc.transchar(c);
    old_len = showcmd_buf.length();
    extra_len = p.length();
    overflow = old_len + extra_len - SHOWCMD_COLS;
    if(overflow > 0) {
      showcmd_buf.delete(0, overflow);
    }
    showcmd_buf.append(p);
  }

  static void add_to_showcmd_c(int c) {
    // if (!add_to_showcmd(c))
	// setcursor();
    add_to_showcmd(c);
  }

  /**
   * Delete 'len' characters from the end of the shown command.
   */
  static private void del_from_showcmd(int len) {
    int	    old_len;

    if (!G.p_sc.getBoolean())
      return;

    old_len = showcmd_buf.length();
    if (len > old_len)
      len = old_len;
    showcmd_buf.delete(old_len - len, SHOWCMD_COLS);

    if (!GetChar.char_avail())
      display_showcmd();
  }

  static void push_showcmd() {
    if (!G.p_sc.getBoolean())
      return;

    old_showcmd_buf.setLength(0);
    old_showcmd_buf.append(showcmd_buf.toString());
  }

  static void pop_showcmd() {
    if (!G.p_sc.getBoolean())
      return;

    showcmd_buf.setLength(0);
    showcmd_buf.append(old_showcmd_buf.toString());

    display_showcmd();
  }

  static private void display_showcmd() {
    int	    len;

    // cursor_off();

    len = showcmd_buf.length();
    if (len == 0)
      showcmd_is_clear = true;
    else
    {
      showcmd_is_clear = false;
    }

    //G.curwin.getStatusDisplay()
	      //.displayCommand(showcmd_buf.toString());
    Misc.setCommandCharacters(showcmd_buf.toString());

    //
    // clear the rest of an old message by outputing up to SHOWCMD_COLS spaces
    //
    // screen_puts((char_u *)"          " + len, (int)Rows - 1, sc_col + len, 0)

    // setcursor();	    /* put cursor back where it belongs */
  }
  
  static private void nv_scroll_line(CMDARG cap, boolean is_ctrl_e) {
    do_xop("nv_scroll_line");
    if(checkclearop(cap.oap))
      return;
    scroll_redraw(is_ctrl_e, cap.count1);
  }
  
  static private void scroll_redraw(boolean up, int count) {
    if(G.curwin.getLineCount() <= G.curwin.getViewLines())
      return;
    
    int prev_topline = G.curwin.getViewTopLine();
    int prev_lnum = G.curwin.getWCursor().getLine();
    
    int new_topline = prev_topline + (up ? count : -count);
    
    new_topline = Misc.adjustTopLine(new_topline);
    int new_bottomline = new_topline + G.curwin.getViewLines() -1;
    
    int new_lnum = prev_lnum;
    int so = Misc.getScrollOff();
    if(new_lnum < new_topline + so)
      new_lnum = new_topline + so;
    else if(new_lnum > new_bottomline - so)
      new_lnum = new_bottomline - so;
    
    if(new_lnum != prev_lnum) {
      G.curwin.setCaretPosition(new_lnum, 0);
      Misc.coladvance(G.curwin.getWCurswant());
    }
    G.curwin.setViewTopLine(new_topline);
  }

  /** nv_zet is simplified a bunch, only do vi compat */
  static private  void	nv_zet (CMDARG cap) {
    switch(cap.nchar) {
      case NL:		    // put curwin->w_cursor at top of screen
      case K_KENTER:
      case CR:
      case '.':		// put curwin->w_cursor in middle of screen and set cursor at the first character of that line
      case 'z':		// put curwin->w_cursor in middle of screen
      case '-':		// put curwin->w_cursor at bottom of screen
        // Scroll screen so current line at top, middle or bottom
	// the scrolloff version doesn't really change anything about how stuff
	// is displayed, so just use it.
	nv_zet_scrolloff(cap);
        return;
          
      default:
        // maybe its a fold operation
	G.curwin.foldOperation(cap.nchar);
        // clearop(cap.oap); // NEEDSWORK: needed?
	return;
    }
    
  }
  
  /* nv_zet_original NOT USED, does compile ok in java
  static private  void	nv_zet_original (CMDARG cap) {
    if(G.p_so.getInteger() != 0) {
      nv_zet_scrolloff(cap);
      return;
    }
    
    do_xop("nv_zet");

    int nchar = cap.nchar;
    int target = G.curwin.getWCursor().getLine();
    boolean change_line = false;
    int top = 0;

    if(cap.count0 != 0 && cap.count0 != target) {
      MarkOps.setpcmark();
      if(cap.count0 > G.curwin.getLineCount()) {
	target = G.curwin.getLineCount();
      } else {
	target = cap.count0;
      }
    }

    switch(nchar) {
      case NL:		    // put curwin->w_cursor at top of screen
      case CR:
	top = target;
	break;

      case '.':		// put curwin->w_cursor in middle of screen
	top = target - G.curwin.getViewLines() / 2 - 1;
	break;

      case '-':		// put curwin->w_cursor at bottom of screen
	top = target - G.curwin.getViewLines() + 1;
	break;

      default:
	clearopbeep(cap.oap);
	return;
    }

    // Keep getlineSegment before setViewTopLine,
    // don't want to fetch segment changing during rendering
    Segment seg = G.curwin.getLineSegment(target);
    int col = Edit.beginlineColumnIndex(BL_WHITE | BL_FIX, seg);
    G.curwin.setViewTopLine(Misc.adjustTopLine(top));
    G.curwin.setCaretPosition(target, col);
  }
  */

  static private  void	nv_zet_scrolloff (CMDARG cap) {
    do_xop("nv_zet");

    int so = Misc.getScrollOff();
    int nchar = cap.nchar;
    int target = G.curwin.getWCursor().getLine();
    boolean change_line = false;
    int top = 0;

    if(cap.count0 != 0 && cap.count0 != target) {
      MarkOps.setpcmark();
      if(cap.count0 > G.curwin.getLineCount()) {
	target = G.curwin.getLineCount();
      } else {
	target = cap.count0;
      }
    }

    switch(nchar) {
      case NL:		    // put curwin->w_cursor at top of screen
      case K_KENTER:
      case CR:
	top = target - so;
	break;

      case '.':		// put curwin->w_cursor in middle of screen
      case 'z':		// put curwin->w_cursor in middle of screen
        top = target - G.curwin.getViewLines() / 2 - 1;
	break;

      case '-':		// put curwin->w_cursor at bottom of screen
	top = target - G.curwin.getViewLines() + 1 + so;
	break;

      default:
        clearopbeep(cap.oap);
	return;
    }

    // Keep getlineSegment before setViewTopLine,
    // don't want to fetch segment changing during rendering
    Segment seg = G.curwin.getLineSegment(target);
    int col = nchar == 'z' ? G.curwin.getWCursor().getColumn()
                           : Edit.beginlineColumnIndex(BL_WHITE | BL_FIX, seg);
    G.curwin.setViewTopLine(Misc.adjustTopLine(top));
    G.curwin.setCaretPosition(target, col);
  }

  static private void nv_colon (CMDARG cap) {
    do_xop("nv_colon");
    if (G.VIsual_active) {
       // VISUAL REPAINT HACK
       G.drawSavedVisualBounds = true;
       // END VISUAL REPAINT HACK
       nv_operator(cap);
    } else if( ! checkclearop(cap.oap)) {
      // translate "count:" into ":.,.+(count - 1)"
      StringBuffer range = new StringBuffer();
      if(cap.count0 != 0) {
        range.append(".");
        // since count1 != 0, then probably > 0. Oh well, just like vim
        if(cap.count0 > 1) {
          range.append(",.+");
          range.append(cap.count0-1);
        }
      }
      ColonCommands.doColonCommand(range);
    }
  }

  static private  void	nv_ctrlg (CMDARG cap) {
    do_xop("nv_ctrlg");
    G.curwin.displayFileInfo();
  }

  /**
   * Handle the commands that use the word under the cursor.
   *
   * @return TRUE for "*" and "#" commands, indicating that the next search
   * should not set the pcmark.
   */
  static private boolean nv_ident (CMDARG cap, CharBuf searchp)
                               throws NotSupportedException
  {
    do_xop("nv_ident");
    Segment     ptrSeg = null;
    int		n = 0;		// init for GCC
    int		cmdchar;
    boolean	g_cmd;		// "g" command
    //char_u	*aux_offset;
    //int	isman;
    //int	isman_s;
    
    MutableInt  mi = new MutableInt(0);
    boolean     fDoingSearch = false;


    if (cap.cmdchar == 'g')	// "g*", "g#", "g]" and "gCTRL-]"
    {
      cmdchar = cap.nchar;
      g_cmd = true;
    }
    else
    {
      cmdchar = cap.cmdchar;
      g_cmd = false;
    }

    //
    // The "]", "CTRL-]" and "K" commands accept an argument in Visual mode.
    //
    if (cmdchar == ']' || cmdchar == Util.ctrl(']') || cmdchar == 'K')
    {
      // ################ notImp("nv_ident subset");  
      if (G.VIsual_active)	// :ta to visual highlighted text
      {
        if (G.VIsual_mode != 'V')
          unadjust_for_sel();
        if (G.VIsual.getLine() != G.curwin.getWCursor().getLine())
        {
          clearopbeep(cap.oap);
          return fDoingSearch;
        }
        if(G.curwin.getWCursor().compareTo(G.VIsual) < 0)
        {
          ptrSeg = Util.ml_get_pos(G.curwin.getWCursor());
          n = G.VIsual.getColumn() - G.curwin.getWCursor().getColumn() + 1;
        }
        else
        {
          ptrSeg = Util.ml_get_pos(G.VIsual);
          n = G.curwin.getWCursor().getColumn() - G.VIsual.getColumn() + 1;
        }
        end_visual_mode();
        G.VIsual_reselect = false;
        redraw_curbuf_later(NOT_VALID);    // update the inversion later
      }
      if (checkclearopq(cap.oap))
        return fDoingSearch;
    }

    if (ptrSeg == null) {
      if((ptrSeg = find_ident_under_cursor(mi,
                              (cmdchar == '*' || cmdchar == '#')
                              ? FIND_IDENT|FIND_STRING : FIND_IDENT)) == null) {
        clearop(cap.oap);
        return fDoingSearch;
      }
      // found something, get the length
      n = mi.getValue();
    }

    /*
    isman = (STRCMP(p_kp, "man") == 0);
    isman_s = (STRCMP(p_kp, "man -s") == 0);
    if (cap.count0 && !(cmdchar == 'K' && (isman || isman_s))
        && !(cmdchar == '*' || cmdchar == '#'))
      stuffnumReadbuff(cap.count0);
    */
    switch (cmdchar)
    {
      case '*':
      case '#':
        //
        // Put cursor at start of word, makes search skip the word
        // under the cursor.
        // Call setpcmark() first, so "*``" puts the cursor back where
        // it was.
        ///
        MarkOps.setpcmark();
        G.curwin.setCaretPosition(G.curwin.getWCursor().getLine(),
                                  ptrSeg.getIndex() - ptrSeg.getBeginIndex());

        if (!g_cmd && Misc.vim_iswordc(ptrSeg.current()))
          GetChar.stuffReadbuff("\\<");
        // no_smartcase = TRUE;	// don't use 'smartcase' now
        break;
        
      case 0x1f & (int)(']'):   // Ctrl-]
        // give the environment a chance at it
        // pass in the extracted identifier,
        // though system probably looks under cursor
        G.curwin.setCaretPosition(G.curwin.getWCursor().getLine(),
                                  ptrSeg.getIndex() - ptrSeg.getBeginIndex());
	G.curwin.jumpDefinition(new String(ptrSeg.array, ptrSeg.getIndex(), n));
        return fDoingSearch;

      /*
      case 'K':
        if (*p_kp == NUL)
          stuffReadbuff((char_u *)":he ");
        else
        {
          stuffReadbuff((char_u *)":! ");
          if (!cap.count0 && isman_s)
            stuffReadbuff((char_u *)"man");
          else
            stuffReadbuff(p_kp);
          stuffReadbuff((char_u *)" ");
          if (cap.count0 && (isman || isman_s))
          {
            stuffnumReadbuff(cap.count0);
            stuffReadbuff((char_u *)" ");
          }
        }
        break;

      case ']':
        stuffReadbuff((char_u *)":ts ");
        break;

      default:
        if (curbuf.b_help)
          stuffReadbuff((char_u *)":he ");
        else if (g_cmd)
          stuffReadbuff((char_u *)":tj ");
        else
          stuffReadbuff((char_u *)":ta ");
      */
      default:
        notImp("nv_ident subset");
    }

    //
    // Now grab the chars in the identifier
    ///
    /*
    if (cmdchar == '*' || cmdchar == '#')
      aux_offset = (char_u *)(p_magic ? "/?.*~[^$\\" : "/?^$\\");
    else
      aux_offset = escape_chars;
    while (n--)
    {
      // put a backslash before \ and some others
      if (vim_strchr(aux_offset, *offset) != NULL)
        stuffcharReadbuff('\\');
      // don't interpret the characters as edit commands
      else if (*offset < ' ' || *offset > '~')
        stuffcharReadbuff(Ctrl('V'));
      stuffcharReadbuff(*offset++);
    }
    */
    String escapeMe = "/?.*~[^$\\";
    while(n-- != 0) {
      int c = ptrSeg.current();
      if(Util.vim_strchr(escapeMe, c) != null) {
        GetChar.stuffcharReadbuff('\\');
      }
      // don't quote control characters, shouldn't be any....
      GetChar.stuffcharReadbuff(c);
      ptrSeg.next();
    }

    if (       !g_cmd
            && (cmdchar == '*' || cmdchar == '#')
            && Misc.vim_iswordc(ptrSeg.previous()))
      GetChar.stuffReadbuff("\\>");
    GetChar.stuffReadbuff("\n");

    //
    // The search commands may be given after an operator.  Therefore they
    // must be executed right now.
    ///
    if (cmdchar == '*' || cmdchar == '#')
    {
      if (cmdchar == '*')
        cap.cmdchar = '/';
      else
        cap.cmdchar = '?';
      
      nv_search(cap, searchp, true);
      fDoingSearch = true;
    }
    return fDoingSearch;
  }

  /**
   * Handle scrolling command 'H', 'L' and 'M'.
   */
  static private void nv_scroll(CMDARG cap) {
    if(G.p_so.getInteger() != 0) {
      nv_scroll_scrolloff(cap);
      return;
    }
    do_xop("nv_scroll");
    int	    used = 0;
    int    n;

    cap.oap.motion_type = MLINE;
    MarkOps.setpcmark();
    int newcursorline = -1;

    if (cap.cmdchar == 'L') {
      Misc.validate_botline();	    // make sure curwin.w_botline is valid
      newcursorline = G.curwin.getViewBottomLine() - 1;
      if (cap.count1 - 1 >= newcursorline) {
	newcursorline = 1;
      } else {
	newcursorline -= cap.count1 - 1;
      }
    } else {
      int topline = G.curwin.getViewTopLine();
      int line_count = G.curwin.getLineCount();
      if (cap.cmdchar == 'M') {
	Misc.validate_botline();	    // make sure w_empty_rows is valid
	for (n = 0; topline + n < line_count; ++n)
	  if ((used += Misc.plines(topline + n)) >=
	      (G.curwin.getViewLines() - G.curwin.getViewBlankLines() + 1) / 2)
	    break;
	if (n != 0 && used > G.curwin.getViewLines())
	  --n;
      } else {
	n = cap.count1 - 1;
      }
      newcursorline = topline + n;
      if (newcursorline > line_count)
	newcursorline = line_count;
    }

    if(newcursorline > 0) {
      G.curwin.setCaretPosition(G.curwin.getLineStartOffset(newcursorline));
    }
    Misc.cursor_correct();	// correct for 'so'
    Edit.beginline(BL_SOL | BL_FIX);
  }

  static private void nv_scroll_scrolloff(CMDARG cap) {
    do_xop("nv_scroll");
    int	    used = 0;
    int    n;

    cap.oap.motion_type = MLINE;
    MarkOps.setpcmark();
    int newcursorline = -1;

    if(cap.cmdchar == 'M') {
      int topline = G.curwin.getViewTopLine();
      int line_count = G.curwin.getLineCount();
      Misc.validate_botline();	    // make sure w_empty_rows is valid
      for (n = 0; topline + n < line_count; ++n)
	if ((used += Misc.plines(topline + n)) >=
	    (G.curwin.getViewLines() - G.curwin.getViewBlankLines() + 1) / 2)
	  break;
      if (n != 0 && used > G.curwin.getViewLines())
	--n;
      newcursorline = topline + n;
      if (newcursorline > line_count)
	newcursorline = line_count;
    } else {
      // cap.cmdchar == 'H' or 'L'
      //Misc.validate_botline(); 'L' // make sure curwin.w_botline is valid
      int so = Misc.getScrollOff();
      int adjust = so;
      if(cap.count1 -1 > so) {
	adjust = cap.count1 -1;
      }
      if (cap.cmdchar == 'L') {
	newcursorline = G.curwin.getViewBottomLine() - 1 - adjust;
	if(newcursorline < G.curwin.getViewTopLine() + so) {
	  newcursorline = G.curwin.getViewTopLine() + so;
	}
      } else {
	// 'H'
	newcursorline = G.curwin.getViewTopLine() + adjust;
	if(newcursorline > G.curwin.getViewBottomLine() - 1 - so) {
	  newcursorline = G.curwin.getViewBottomLine() - 1 - so;
	}
      }
    }

    if(newcursorline > 0) {
      G.curwin.setCaretPosition(
		    G.curwin.getLineStartOffset(newcursorline));
    }
    Misc.cursor_correct();	// correct for 'so'
    Edit.beginline(BL_SOL | BL_FIX);
  }

  /**
   * Cursor right commands.
   */
  static private  void	nv_right (CMDARG cap) {
    long	n;
    boolean	past_line;

    cap.oap.motion_type = MCHAR;
    cap.oap.inclusive = false;
    past_line = (G.VIsual_active && G.p_sel.charAt(0) != 'o');
    for (n = cap.count1; n > 0; --n) {
      FPOS fpos = G.curwin.getWCursor();
      Segment seg = G.curwin.getLineSegment(fpos.getLine());
      if ((!past_line && Edit.oneright() == FAIL)
	    || (past_line && seg.array[fpos.getColumn()+seg.offset] == '\n')
	  )
      {

	//
	//	  <Space> wraps to next line if 'whichwrap' bit 1 set.
	//	      'l' wraps to next line if 'whichwrap' bit 2 set.
	// CURS_RIGHT wraps to next line if 'whichwrap' bit 3 set
	//
	if (       ((cap.cmdchar == ' ' && G.p_ww_sp.getBoolean())
		    || (cap.cmdchar == 'l' && G.p_ww_l.getBoolean())
		    || (cap.cmdchar == K_RIGHT && G.p_ww_rarrow.getBoolean()))
		   // && curwin.w_cursor.lnum < curbuf.b_ml.ml_line_count
		   && fpos.getLine() < G.curwin.getLineCount())
	{
	  // When deleting we also count the NL as a character.
	  // Set cap.oap.inclusive when last char in the line is
	  // included, move to next line after that
	  if (	   (cap.oap.op_type == OP_DELETE
		    || cap.oap.op_type == OP_CHANGE)
		   && !cap.oap.inclusive
		   && !Util.lineempty(fpos.getLine())) {
	    cap.oap.inclusive = true;
	  } else {
	    G.curwin.setCaretPosition(fpos.getLine() + 1, 0);
	    G.curwin.setWSetCurswant(true);
	    cap.oap.inclusive = false;
	  }
	  continue;
	}
	if (cap.oap.op_type == OP_NOP) {
	  // Only beep and flush if not moved at all
	  if (n == cap.count1)
	    Util.beep_flush();
	} else {
	  if (!Util.lineempty(fpos.getLine())) {
	    cap.oap.inclusive = true;
	  }
	}
	break;
      } else if (past_line) {
	// NEEDSWORK: (maybe no work) pastline always false since no select
	 G.curwin.setWSetCurswant(true);
         G.curwin.setCaretPosition(fpos.getOffset()+1);
      }
    }
  }

  /**
   * Cursor left commands.
   * @return TRUE when operator end should not be adjusted.
   */
  static private  boolean nv_left (CMDARG cap) {
    long	n;
    boolean	retval = false;

    cap.oap.motion_type = MCHAR;
    cap.oap.inclusive = false;
    for (n = cap.count1; n > 0; --n) {
      if (Edit.oneleft() == FAIL) {
	FPOS fpos = G.curwin.getWCursor();
	// <BS> and <Del> wrap to previous line if 'whichwrap' has 'b'.
	//		 'h' wraps to previous line if 'whichwrap' has 'h'.
	//	   CURS_LEFT wraps to previous line if 'whichwrap' has '<'.
	//
	if (       (((cap.cmdchar == K_BS
		      || cap.cmdchar == Util.ctrl('H'))
		     && G.p_ww_bs.getBoolean())
		    || (cap.cmdchar == 'h'
			&& G.p_ww_h.getBoolean())
		    || (cap.cmdchar == K_LEFT
			&& G.p_ww_larrow.getBoolean()))
		   && fpos.getLine() > 1)
	{
	  /* **********************************
	  int offset = G.curwin.getLineOffset(fpos.getLine() - 1);
	  Segment seg = G.curwin.getLineSegment(fpos.getLine() - 1);
	  int idx = Misc.coladvanceColumnIndex(MAXCOL, seg);
	  G.curwin.setCaretPosition(offset + idx);
	  ************************************/

	  // use a different algorithm with swing document
	  G.curwin.setCaretPosition(fpos.getOffset() - 1);
	  Misc.check_cursor_col();
	  G.curwin.setWSetCurswant(true);

	  // When the NL before the first char has to be deleted we
	  // put the cursor on the NUL after the previous line.
	  // This is a very special case, be careful!
	  // don't adjust op_end now, otherwise it won't work
	  if (	   (cap.oap.op_type == OP_DELETE
		    || cap.oap.op_type == OP_CHANGE)
		   && !Util.lineempty(fpos.getLine()))
	  {
	    G.curwin.setCaretPosition(fpos.getOffset() + 1);
	    retval = true;
	  }
	  continue;
	}
	// Only beep and flush if not moved at all
	else if (cap.oap.op_type == OP_NOP && n == cap.count1) {
	  Util.beep_flush();
	}
	break;
      }
    }

    return retval;
  }

  /**
   * Handle the "$" command.
   */
  static private void nv_dollar(CMDARG cap) {
    cap.oap.motion_type = MCHAR;
    cap.oap.inclusive = true;
    G.curwin.setWCurswant(MAXCOL);	// so we stay at the end
    if (Edit.cursor_down(cap.count1 - 1, cap.oap.op_type == OP_NOP) == FAIL) {
      clearopbeep(cap.oap);
    }
  }

  /**
   * Implementation of '?' and '/' commands.
   * <p>
   * This starts the search by bringing up a dialog to enter the search pattern.
   * When the dialog finishes Normal.processInputChar to normal_cmd to
   * nv_search_finish() which should do all the regular things that finish
   * up a command. So any invocation of nv_search() should return from normal_cmd
   * directly without doing the finishing steps.
   */
  static private void nv_search(CMDARG cap,
                                CharBuf searchp,
                                boolean dont_set_mark) {
    do_xop("nv_search");
    
    // This flags normal_cmd to pick up another character
    // for the search command. This othe character will be issued
    // after the pattern is input.
    pickupExtraChar = true;
    
    Search.inputSearchPattern(cap,
                              cap.count1,
                              (dont_set_mark ? 0 : SEARCH_MARK)
                               | SEARCH_OPT | SEARCH_ECHO | SEARCH_MSG);
  }
  
  static private void nv_search_finish(CMDARG cap) {
    do_xop("nv_search_finish");
    assert cap.nchar == K_X_INCR_SEARCH_DONE
           || cap.nchar == K_X_SEARCH_CANCEL
           || cap.nchar == K_X_SEARCH_FINISH;
    
    if(cap.nchar == K_X_SEARCH_CANCEL) {
      Msg.clearMsg();
      clearop(oap);
      return;
    }
    
    OPARG oap = cap.oap;
    int i;
    
    oap.motion_type = MCHAR;
    oap.inclusive = false;
    G.curwin.setWSetCurswant(true);
    
    if(cap.nchar == K_X_INCR_SEARCH_DONE)
      i = 1; // NEEDSWORK: retrieve from Search.xxx (but always 1 anyway)
    else if(cap.nchar == K_X_SEARCH_FINISH)
      i = Search.doSearch();
    else
      i = 0;
                        
    if(i == 0) {
      clearop(oap);
    } else if(i == 2) {
      oap.motion_type = MLINE;
    }

    /* *********************************************************
    OPARG	*oap = cap.oap;
    int		i;

    if (cap.cmdchar == '?' && cap.oap.op_type == OP_ROT13)
    {
      // Translate "g??" to "g?g?"
      cap.cmdchar = 'g';
      cap.nchar = '?';
      nv_operator(cap);
      return;
    }

    if ((*searchp = getcmdline(cap.cmdchar, cap.count1, 0)) == NULL)
    {
      clearop(oap);
      return;
    }
    oap.motion_type = MCHAR;
    oap.inclusive = FALSE;
    curwin.w_set_curswant = TRUE;

    i = do_search(oap, cap.cmdchar, *searchp, cap.count1,
		  (dont_set_mark ? 0 : SEARCH_MARK) |
		  SEARCH_OPT | SEARCH_ECHO | SEARCH_MSG);
    if (i == 0)
      clearop(oap);
    else if (i == 2)
      oap.motion_type = MLINE;

    // "/$" will put the cursor after the end of the line, may need to
    // correct that here
    adjust_cursor();
    ******************************************************/
  }

  /**
   * Handle "N" and "n" commands.
   */
  static private  void	nv_next (CMDARG cap, int flag) {
    do_xop("nv_next");
    int rc = Search.doNext(cap, cap.count1,
	      SEARCH_MARK | SEARCH_OPT | SEARCH_ECHO | SEARCH_MSG | flag);
    if(rc == 0) {
      clearop(cap.oap);
    }
    /* *******************************************************
    cap.oap.motion_type = MCHAR;
    cap.oap.inclusive = FALSE;
    curwin.w_set_curswant = TRUE;
    if (!do_search(cap.oap, 0, NULL, cap.count1,
	       SEARCH_MARK | SEARCH_OPT | SEARCH_ECHO | SEARCH_MSG | flag))
      clearop(cap.oap);

    // "/$" will put the cursor after the end of the line, may need to
    // correct that here
    adjust_cursor();
    *********************************************************/
  }

  static private void nv_csearch(CMDARG cap, int dir, boolean type) {
    cap.oap.motion_type = MCHAR;
    if (dir == BACKWARD)
      cap.oap.inclusive = false;
    else
      cap.oap.inclusive = true;
    if (cap.nchar >= 0x100
		|| !Search.searchc(cap.nchar, dir, type, cap.count1)) {
      clearopbeep(cap.oap);
    } else {
      G.curwin.setWSetCurswant(true);
      // NEEDSWORK: visual mode: adjust_for_sel(cap);
    }
  }

  /**
   * Handle Normal mode "%" command.
   */
  static private void nv_percent(CMDARG cap) {
    do_xop("nv_percent");

    cap.oap.inclusive = true;
    if (cap.count0 != 0) {	    // {cnt}% : goto {cnt} percentage in file
      if (cap.count0 > 100) {
	clearopbeep(cap.oap);
      } else {
	cap.oap.motion_type = MLINE;
	MarkOps.setpcmark();
	// round up, so CTRL-G will give same value
        int line = (G.curwin.getLineCount()
                   * cap.count0 + 99) / 100;
        G.curwin.setCaretPosition(line, 0);
	Edit.beginline(BL_SOL | BL_FIX);
      }
    } else {	    // "%" : go to matching paren
      cap.oap.motion_type = MCHAR;
      int startingOffset = G.curwin.getCaretPosition();
      int firstbraceOffset = startingOffset;
      // Try to position caret on next brace char
      int c;
      // NEEDSWORK: use getLineSegment for performance
      while(true) {
        c = Util.getCharAt(firstbraceOffset);
        if(Util.vim_strchr("{}[]()", c) != null || c == '\n') {
          break;
        }
        firstbraceOffset++;
      }
      G.curwin.setCaretPosition(firstbraceOffset);
      G.curwin.findMatch();
      int endingOffset = G.curwin.getCaretPosition();
      if(firstbraceOffset == endingOffset) {
        // put back to original position
        G.curwin.setCaretPosition(startingOffset);
	clearopbeep(cap.oap);
      } else {
	MarkOps.setpcmark(startingOffset);
        G.curwin.setWSetCurswant(true);
	adjust_for_sel(cap);
      }
      /* **************************************************
      ViFPOS fpos = Search.findmatch(cap.oap, NUL);
      if (fpos == null) {
	clearopbeep(cap.oap);
      } else {
	MarkOps.setpcmark();
        G.curwin.setCaretPosition(fpos.getOffset());
        G.curwin.setWSetCurswant(true);
	adjust_for_sel(cap);
      }
      ****************************************************/
    }
  }

  
  /*
 * Handle "(" and ")" commands.
 */
 
  static private void nv_brace(CMDARG cap, int dir) {
    do_xop("nv_brace");

    cap.oap.motion_type = MCHAR;

    if (cap.cmdchar == ')')
      cap.oap.inclusive = false;
    else
      cap.oap.inclusive = true;

    G.curwin.setWSetCurswant(true);

    if (!Search.findsent(dir, cap.count1))
      clearopbeep(cap.oap);
  }
  
/*
 * Handle the "{" and "}" commands.
 */

static private void nv_findpar(CMDARG cap, int dir)
{
  cap.oap.motion_type = MCHAR;
  cap.oap.inclusive = false;
  G.curwin.setWSetCurswant(true);
  if (!Search.findpar(cap, dir, cap.count1, NUL, false))
    clearopbeep(cap.oap);
}

  /**
   * Handle the "r" command.
   */
  static private void nv_replace(CMDARG cap) {

    if (checkclearop(cap.oap))
      return;

    // NEEDSWORK: be very lazy, use Edit.edit for the 'r' command

    //
    // Replacing with a TAB is done by edit(), because it is complicated when
    // 'expandtab', 'smarttab' or 'softtabstop' is set.
    // Other characters are done below to avoid problems with things like
    // CTRL-V 048 (for edit() this would be R CTRL-V 0 ESC).
    //

    // if (cap.nchar == '\t' && (curbuf.b_p_et || p_sta)) ....
    GetChar.stuffnumReadbuff(cap.count1);
    GetChar.stuffcharReadbuff('R');
    GetChar.stuffcharReadbuff((char)cap.nchar);
    GetChar.stuffcharReadbuff(ESC);
    return;
  }
  
  /**
   * 'o': Exchange start and end of Visual area.
   * 'O': same, but in block mode exchange left and right corners.
   */
  static private  void	v_swap_corners (CMDARG cap) {
    do_op("v_swap_corners");
    FPOS old_cursor;
    int /*colnr_t*/ left, right;

    if ((cap.cmdchar == 'O') && G.VIsual_mode == Util.ctrl('V'))
    {
//TODO: FIXME_VISUAL BLOCK MODE
//       old_cursor = (FPOS)G.curwin.getWCursor().copy();
//       //getvcols(&old_cursor, &VIsual, &left, &right);
//       curwin.w_cursor.lnum = VIsual.lnum;
//       coladvance(left);
//       G.VIsual = G.curwin.w_cursor;
//       G.curwin.w_cursor.lnum = old_cursor.lnum;
//       coladvance(right);
//       G.curwin.w_curswant = right;
//       if (G.curwin.w_cursor.col == old_cursor.col)
//       {
//           G.curwin->w_cursor.lnum = VIsual.lnum;
//           coladvance(right);
//           G.VIsual = G.curwin.w_cursor;
//           curwin->w_cursor.lnum = old_cursor.lnum;
//           coladvance(left);
//           curwin->w_curswant = left;
//       }
    }
    if (cap.cmdchar != 'O' || G.VIsual_mode != Util.ctrl('V'))
    {
        old_cursor = (FPOS)G.curwin.getWCursor().copy();
        G.curwin.setCaretPosition(G.VIsual.getOffset());
        G.VIsual = old_cursor;
        G.curwin.updateVisualState();
    }
  }

  /**
   * "R".
   */
  static private void nv_Replace(CMDARG cap)
  {
    if (G.VIsual_active)		/* "R" is replace lines */
    {
      cap.cmdchar = 'c';
      G.VIsual_mode = 'V';
      nv_operator(cap);
    } else if (!checkclearopq(cap.oap)) {
      if (u_save_cursor() == OK) {
        Misc.beginInsertUndo();
        Edit.edit('R', false, cap.count1);
      }
    }
  }

  /**
   * Swap case for "~" command, when it does not work like an operator.
   */
  static private  void	n_swapchar (CMDARG cap) {
    do_op("n_swapchar");
    int	n;

    if (checkclearopq(cap.oap))
      return;

    if (Util.lineempty(G.curwin.getWCursor().getLine())
                && G.p_ww_tilde.getBoolean()) {
      clearopbeep(cap.oap);
      return;
    }

    prep_redo_cmd(cap);

    if (u_save_cursor() == FAIL)
      return;

    Misc.beginUndo();
    try {
      for (n = cap.count1; n > 0; --n) {
        Misc.swapchar(cap.oap.op_type, G.curwin.getWCursor());
        Misc.inc_cursor();
        if (Misc.gchar_cursor() == '\n') {
          if (G.p_ww_tilde.getBoolean()
              && G.curwin.getWCursor().getLine() < G.curwin.getLineCount())
          {
            G.curwin.setCaretPosition(G.curwin.getWCursor().getLine() + 1, 0);
            // redraw_curbuf_later(NOT_VALID);
          }
          else
            break;
        }
      }

      Misc.adjust_cursor();
      G.curwin.setWSetCurswant(true);
    } finally {
      Misc.endUndo();
    }
    // changed();

    /* assume that the length of the line doesn't change, so w_botline
     * remains valid */
    // update_screenline();
  }

  static private  void	nv_cursormark (CMDARG cap, boolean flag, ViFPOS pos) {
    do_xop("nv_cursormark");
    if (MarkOps.check_mark((ViMark)pos) == FAIL)
      clearop(cap.oap);
    else
    {
      if (cap.cmdchar == '\'' || cap.cmdchar == '`')
	MarkOps.setpcmark();
      G.curwin.setWCurswant(pos.getColumn());
      Misc.gotoLine(pos.getLine(), flag ? BL_WHITE | BL_FIX : -1);
    }
    cap.oap.motion_type = flag ? MLINE : MCHAR;
    cap.oap.inclusive = false;		/* ignored if not MCHAR */
    G.curwin.setWSetCurswant(true);
  }
  
  /**
   * Handle commands that are operators in Visual mode.
   */
  static private  void	v_visop (CMDARG cap) {
      do_op("v_visop");
      String trans = "YyDdCcxdXdAAIIrr";

    /* Uppercase means linewise, except in block mode, then "D" deletes till
     * the end of the line, and "C" replaces til EOL */
    if (Character.isUpperCase(cap.cmdchar))
    {
       if (G.VIsual_mode != Util.ctrl('V'))
           G.VIsual_mode = 'V';
       else if (cap.cmdchar == 'C' || cap.cmdchar == 'D')
           G.curwin.setWCurswant(MAXCOL);
    }
    cap.cmdchar = Util.vim_strchr(trans, cap.cmdchar).charAt(1);
    nv_operator(cap);
  }

  static String[] nv_optrans_ar = new String[] { "dl", "dh", "d$", "c$",
    					         "cl", "cc", "yy", ":s\r"};
  /**
   * Translate a command into another command.
   */
  static private  void	nv_optrans (CMDARG cap) {
    do_xop("nv_optrans");

    String str = "xXDCsSY&";
    if (!checkclearopq(cap.oap))
    {
      if (cap.count0 != 0) {
	GetChar.stuffnumReadbuff(cap.count0);
      }

      GetChar.stuffReadbuff(nv_optrans_ar[str.indexOf(cap.cmdchar)]);
    }
  }

  /**
   * Handle "'" and "`" commands.
   */
  static private void nv_gomark(CMDARG cap, boolean flag) {
    do_xop("nv_gomark");
    ViFPOS	pos;

    pos = MarkOps.getmark(cap.nchar, (cap.oap.op_type == OP_NOP));
    if (pos == MarkOps.otherFile) {	    /* jumped to other file */
      if (flag) {
	Misc.check_cursor_lnumBeginline(BL_WHITE | BL_FIX);
	// Edit.beginline(BL_WHITE | BL_FIX);
      } else {
	Misc.adjust_cursor();
      }
    } else {
      nv_cursormark(cap, flag, pos);
    }
  }

  /**
   * Handle CTRL-O and CTRL-I commands.
   */
  static private void nv_pcmark(JLOP op, CMDARG cap)
  throws NotSupportedException {
    do_xop("nv_g_cmd");
    G.curwin.jumpList(op, cap.count1);
//    ViFPOS	fpos;
//
//    if (!checkclearopq(cap.oap)) {
//      fpos = movemark((int)cap.count1);
//      if (fpos == MarkOps.otherFile) {      /* jump to other file */
//	curwin.w_set_curswant = TRUE;
//	Misc.adjust_cursor();
//      } else if (fpos != NULL) {	    /* can jump */
//	nv_cursormark(cap, FALSE, fpos);
//      } else {
//	clearopbeep(cap.oap);
//      }
//    }
  }


  /**
   * Handle '"' command.
   */
  static private  void	nv_regname (CMDARG cap, MutableInt opnump) {
    do_xop("nv_regname");
    if (checkclearop(cap.oap))
      return;
    if (cap.nchar != NUL && Misc.valid_yank_reg(cap.nchar, false)) {
      cap.oap.regname = cap.nchar;
      opnump.setValue(cap.count0);    // remember count before '"'
    } else {
      clearopbeep(cap.oap);
    }
  }
  
  /**
   * Handle "v", "V" and "CTRL-V" commands.
   * Also for "gh", "gH" and "g^H" commands.
   */
  
  static private  void	nv_visual(CMDARG cap, boolean selectmode) {
      do_op("nv_visual");
      if (G.VIsual_active) {/* change Visual mode */
          if (G.VIsual_mode == cap.cmdchar) {    /* stop visual mode */
              end_visual_mode();
          } else {/* toggle char/block mode */
              /*or char/line mode */
              G.VIsual_mode = cap.cmdchar;
              Misc.showmode();
              /* update the screen cursor position */
              G.curwin.updateVisualState();
              //G.curwin.setWCurswant(G.curwin.getCaretPosition());
          }
          update_curbuf(NOT_VALID);/* update the inversion */
      } else { /* start Visual mode */
          if (cap.count0 > 0) { /*use previously selected part */
              if (resel_VIsual_mode == NUL)   /* there is none */
              {
                  Util.beep_flush();
                  return;
              }
              G.VIsual = (FPOS)G.curwin.getWCursor().copy();
              G.VIsual_active = true;
              G.VIsual_reselect = true;
              if (!selectmode)
              /* start Select mode when 'selectmode' contains "cmd" */
                  may_start_select('c');
              if (G.p_smd.value)
                  redraw_cmdline = true;  /* show visual mode later */
              /*
               * For V and ^V, we multiply the number of lines even if there
               * was only one -- webb
               */
              if (resel_VIsual_mode != 'v' || resel_VIsual_line_count > 1)
              {
                  int line = G.curwin.getWCursor().getLine()
                                    + resel_VIsual_line_count * cap.count0 - 1;
                  if(line > G.curwin.getLineCount())
                      line = G.curwin.getLineCount();
                  // Not sure about how column should be set, but at least
                  // make sure it stays in the correct line
                  int col = Misc.check_cursor_col(line,
                                            G.curwin.getWCursor().getColumn());
                  G.curwin.setCaretPosition(line, col);
              }
              G.VIsual_mode = resel_VIsual_mode;
              if (G.VIsual_mode == 'v')
              {
                  if (resel_VIsual_line_count <= 1)
                      // NEEDSWORK: ? can column overflow here?
                      G.curwin.setCaretPosition(G.curwin.getWCursor().getLine(),
                              G.curwin.getWCursor().getColumn()
                                + resel_VIsual_col * cap.count0 - 1);
                  else
                      G.curwin.setCaretPosition(G.curwin.getWCursor().getLine(),
                              resel_VIsual_col);
              }
              if (resel_VIsual_col == MAXCOL)
              {
                  G.curwin.setWCurswant(MAXCOL);
                  Misc.coladvance(MAXCOL);
              }
              else if (G.VIsual_mode == Util.ctrl('V'))
              {
                  //TODO: FIXME_VISUAL BLOCK MODE
                  //validate_virtcol();
                  G.curwin.setWCurswant(G.curwin.getWCursor().getColumn()/*G.curwin.w_virtcol*/ + resel_VIsual_col * cap.count0 - 1);
                  Misc.coladvance(G.curwin.getWCurswant());
              }
              else
                  G.curwin.setWSetCurswant(true);
              update_curbuf(NOT_VALID); /* show the inversion */
              /* update the screen cursor position */
              G.curwin.updateVisualState();
          } else {
              if (!selectmode)
/* start Select mode when 'selectmode' contains "cmd" */
                  may_start_select('c');
              n_start_visual_mode(cap.cmdchar);
              /* update the screen cursor position */
              G.curwin.updateVisualState();
          }
      }
  }
  
  /*
  * Start Select mode, if "c" is in 'selectmode' and not in a mapping or menu.
  */
  static void may_start_select(int c) {
      G.VIsual_select = (GetChar.stuff_empty() && typebuf_typed()
        && (Util.vim_strchr(G.p_slm.getString(), c) != null));
  }
  
  /*
   * Start Visual mode "c".
   * Should set VIsual_select before calling this.
   */
  static private  void	n_start_visual_mode(int c) {
      do_op("n_start_visual_mode");
      G.VIsual = (FPOS) G.curwin.getWCursor().copy();
      G.VIsual_mode = c;
      G.VIsual_active = true;
      G.VIsual_reselect = true;
      if (G.p_smd.value)
          redraw_cmdline = true; /* show visual mode later */
//#ifdef USE_CLIPBOARD
    /* Make sure the clipboard gets updated.  Needed because start and
     * end may still be the same, and the selection needs to be owned */
//    clipboard.vmode = NUL;
//#endif
      //update_screenline();/* start the inversion */
  }
  static private void nv_g_cmd(CMDARG cap, CharBuf searchbuff)
  throws NotSupportedException {
    do_xop("nv_g_cmd");
    int i;
    ViFPOS tpos;
    switch (cap.nchar) {
      case ',':
	nv_pcmark(JLOP.NEXT_CHANGE, cap);
        break;
      case ';':
	nv_pcmark(JLOP.PREV_CHANGE, cap);
        break;
      case 'g':
        nv_goto(cap, 1);
        break;
        /*
         * "gv": Reselect the previous Visual area.  If Visual already active,
         * exchange previous and current Visual area.
         */
      case 'v':
        if (checkclearop(oap))
            break;
        if (     MarkOps.check_mark(G.curbuf.b_visual_start, true) == FAIL
              || MarkOps.check_mark(G.curbuf.b_visual_end, true) == FAIL
              || G.curbuf.b_visual_start.getLine() == 0
              || G.curbuf.b_visual_start.getLine() > G.curwin.getLineCount()
              || G.curbuf.b_visual_end.getLine() == 0)
            Util.beep_flush();
        else
        {
            /* set w_cursor to the start of the Visual area, tpos to the end */
            if (G.VIsual_active)
            {
                i = G.VIsual_mode;
                G.VIsual_mode = G.curbuf.b_visual_mode;
                G.curbuf.b_visual_mode = i;
                tpos = G.curbuf.b_visual_end;
                G.curwin.setMarkOffset((ViMark)G.curbuf.b_visual_end,
                                       G.curwin.getWCursor().getOffset(), false);
                G.curwin.setCaretPosition(G.curbuf.b_visual_start.getOffset());
                G.curwin.setMarkOffset((ViMark)G.curbuf.b_visual_start,
                                       G.VIsual.getOffset(), false);
            }
            else
            {
                G.VIsual_mode = G.curbuf.b_visual_mode;
                tpos = G.curbuf.b_visual_end;
                G.curwin.setCaretPosition(G.curbuf.b_visual_start.getOffset());
            }

            G.VIsual_active = true;
            G.VIsual_reselect = true;

            /* Set Visual to the start and w_cursor to the end of the Visual
             * area.  Make sure they are on an existing character. */
            Misc.adjust_cursor();
            G.VIsual = (FPOS) G.curwin.getWCursor().copy();
            G.curwin.setCaretPosition(tpos.getOffset());
            Misc.adjust_cursor();
            //update_topline();
            /*
             * When called from normal "g" command: start Select mode when
             * 'selectmode' contains "cmd".  When called for K_SELECT, always
             * start Select mode
             */
//TODO: FIXME_VISUAL SELECT MODE
            if (searchbuff == null)
                G.VIsual_select = true;
            else
                may_start_select('c');
            //#ifdef USE_MOUSE
            //setmouse();
            //#endif
            //#ifdef USE_CLIPBOARD
            ///* Make sure the clipboard gets updated.  Needed because start and
            //* end are still the same, and the selection needs to be owned */
            //clipboard.vmode = NUL;
            //#endif
            update_curbuf(NOT_VALID);
            Misc.showmode();
        }
        break;
      default:
        notSup("g" + new String(new char[] {(char)cap.nchar}));
        break;
    }
  }

  /**
   * Handle "o" and "O" commands.
   */
  static private void n_opencmd(CMDARG cap)
  {
    do_xop("n_opencmd");

    ViFPOS fpos = G.curwin.getWCursor();
    if (!checkclearopq(cap.oap)) {
      // if (u_save((curwin.w_cursor.lnum - (cap.cmdchar == 'O' ? 1 : 0)) .....
      // NEEDSWORK: would like the beginUndo only if actually making changes
      boolean startEdit = false;  
      try {
        Misc.beginInsertUndo();
        startEdit = Misc.open_line(cap.cmdchar == 'O' ? BACKWARD : FORWARD,
                                   true, false, 0);
        if(startEdit)
	  Edit.edit(cap.cmdchar, true, cap.count1);
      } finally {
          if(!startEdit)
            Misc.endInsertUndo();
      }
    }
    return;
  }
  
  /*
   * Handle "U" command.
   */
  static private  void	nv_Undo (CMDARG cap) throws NotSupportedException {
      do_op("nv_Undo");
         // In Visual mode and typing "gUU" triggers an operator
    if (G.VIsual_active || cap.oap.op_type == OP_UPPER)
    {
         // translate "gUU" to "gUgU"
          cap.cmdchar = 'g';
          cap.nchar = 'U';
          nv_operator(cap);
    } else if (!checkclearopq(cap.oap)) {
          //u_undoline();
          notSup("nv_Undo");
          G.curwin.setWSetCurswant(true);
    }

  }

  /**
   * Handle an operator command.
   */
  static private void nv_operator(CMDARG cap) {
    int	    op_type;
    do_xop("nv_operator");

    op_type = get_op_type(cap.cmdchar, cap.nchar);

    if (op_type == cap.oap.op_type)	    // double operator works on lines
      nv_lineop(cap);
    else if (!checkclearop(cap.oap))
    {
      cap.oap.start = (FPOS)G.curwin.getWCursor().copy();
      cap.oap.op_type = op_type;
    }
  }

  /**
   * Handle linewise operator "dd", "yy", etc.
   */
  static private void nv_lineop(CMDARG cap) {
    do_xop("nv_lineop");
    cap.oap.motion_type = MLINE;
    if (Edit.cursor_down(cap.count1 - 1, cap.oap.op_type == OP_NOP) == FAIL)
      clearopbeep(cap.oap);
    else if (  cap.oap.op_type == OP_DELETE
	       || cap.oap.op_type == OP_LSHIFT
	       || cap.oap.op_type == OP_RSHIFT)
      Edit.beginline(BL_SOL | BL_FIX);
    else if (cap.oap.op_type != OP_YANK)	/* 'Y' does not move cursor */
      Edit.beginline(BL_WHITE | BL_FIX);
  }

  /**
   * Handle "|" command.
   */
  static private  void	nv_pipe (CMDARG cap) {
    do_xop("nv_pipe");

    cap.oap.motion_type = MCHAR;
    cap.oap.inclusive = false;
    Edit.beginline(0);
    if (cap.count0 > 0) {
      Misc.coladvance(cap.count0 - 1);
      G.curwin.setWCurswant(cap.count0 - 1);
    }
    else
      G.curwin.setWCurswant(0);
    // keep curswant at the column where we wanted to go, not where
    // we ended; differs is line is too short
    G.curwin.setWSetCurswant(false);
  }

  static private void nv_goto (CMDARG cap, int lnum) {
    do_xop("nv_goto");
    cap.oap.motion_type = MLINE;
    MarkOps.setpcmark();
    
    // When a count is given, use it instead of the default lnum
    if(cap.count0 != 0)
        lnum = cap.count0;
    if(lnum < 1)
        lnum = 1;
    else if(lnum > G.curwin.getLineCount())
        lnum = G.curwin.getLineCount();
    Misc.gotoLine(lnum, BL_SOL | BL_FIX);
  }

  /**
   * Handle back-word command "b".
   */
  static void nv_bck_word(CMDARG cap, boolean type) {
    do_xop("nv_bck_word");
    cap.oap.motion_type = MCHAR;
    cap.oap.inclusive = false;
    G.curwin.setWSetCurswant(true);
    if (Search.bck_word(cap.count1, type, false) == FAIL)
      clearopbeep(cap.oap);
  }

  /**
   * Handle word motion commands "e", "E", "w" and "W".
   */
  static private void nv_wordcmd(CMDARG cap, boolean type) {
    int	    n;
    boolean    word_end;
    boolean    flag = false;
    do_xop("nv_wordcmd");

    //
    // Inclusive has been set for the "E" and "e" command.
    //
    word_end = cap.oap.inclusive;

    //
    // "cw" and "cW" are a special case.
    //
    if (!word_end && cap.oap.op_type == OP_CHANGE) {
      n = Misc.gchar_cursor();
      if (n != '\n') {			/* not an empty line */
	if (Misc.vim_iswhite(n)) {
	  //
	  // Reproduce a funny Vi behaviour: "cw" on a blank only
	  // changes one character, not all blanks until the start of
	  // the next word.  Only do this when the 'w' flag is included
	  // in 'cpoptions'.
          //
          // Note: the p_cpo_w flag is false for vi behavour.
	  //
	  if (cap.count1 == 1 && ! G.p_cpo_w.getBoolean()) {
	    cap.oap.inclusive = true;
	    cap.oap.motion_type = MCHAR;
	    return;
	  }
	} else {
	  //
	  // This is a little strange. To match what the real Vi does,
	  // we effectively map 'cw' to 'ce', and 'cW' to 'cE', provided
	  // that we are not on a space or a TAB.  This seems impolite
	  // at first, but it's really more what we mean when we say
	  // 'cw'.
	  // Another strangeness: When standing on the end of a word
	  // "ce" will change until the end of the next wordt, but "cw"
	  // will change only one character! This is done by setting
	  // flag.
	  //
	  cap.oap.inclusive = true;
	  word_end = true;
	  flag = true;
	}
      }
    }

    cap.oap.motion_type = MCHAR;
    G.curwin.setWSetCurswant(true);
    if (word_end) {
      n = Search.end_word(cap.count1, type, flag, false);
    } else {
      n = Search.fwd_word(cap.count1, type, cap.oap.op_type != OP_NOP);
    }

    // Don't leave the cursor on the NUL past a line
    if (G.curwin.getWCursor().getColumn() != 0
		&& Misc.gchar_cursor() == '\n') {
      Misc.dec_cursor();
      cap.oap.inclusive = true;
    }

    if (n == FAIL && cap.oap.op_type == OP_NOP) {
      clearopbeep(cap.oap);
    } else {
      adjust_for_sel(cap);
    }
  }

  /*
   * In exclusive Visual mode, may include the last character.
   */
  static private void adjust_for_sel(CMDARG cap) {
      if (G.VIsual_active
          && cap.oap.inclusive
          && G.p_sel.charAt(0) == 'e'
          && Misc.gchar_cursor() != '\n')
       {
          //++curwin->w_cursor.col;
          G.curwin.setCaretPosition(G.curwin.getCaretPosition()+1);
          cap.oap.inclusive = false;
       }
  }

  /**
   * ESC in Normal mode: beep, but don't flush buffers.
   * Don't even beep if we are canceling a command.
   */
  static private void nv_esc(CMDARG cap, int opnum) {
    if (G.VIsual_active) {
      end_visual_mode();	// stop Visual
      Misc.check_cursor_col();	// make sure cursor is not beyond EOL
      G.curwin.setWSetCurswant(true);
      update_curbuf(NOT_VALID);
    } else if (cap.oap.op_type == OP_NOP && opnum == 0
	     && cap.count0 == 0 && cap.oap.regname == 0 && p_im == 0) {
      Util.vim_beep();
    }
    clearop(cap.oap);
    if (p_im != 0 && G.restart_edit == 0) {
      G.restart_edit = 'a';
    }
  }
  
  /** force exit from visual mode.
   * Used by ViManager when switching out and EditorPane
   * This is taken from nv_esc above, except that end_visual_mode
   * has a comment that says it checks EOL so took that out.
   */
  static void abortVisualMode() {
    if (G.VIsual_active) {
      end_visual_mode();	// stop Visual
      G.curwin.setWSetCurswant(true);
      update_curbuf(NOT_VALID);
      Misc.showmode();
    }
  }

  /**
   * Handle "A", "a", "I", "i" and <Insert> commands.
   */
  static private void nv_edit (CMDARG cap)
  {
    do_xop("nv_edit");
    /* in Visual mode "A" and "I" are an operator */
    if ((cap.cmdchar == 'A' || cap.cmdchar == 'I')
	&& G.VIsual_active)
    {
      v_visop(cap);
    }
    /* in Visual mode and after an operator "a" and "i" are for text objects */
    else if ((cap.cmdchar == 'a' || cap.cmdchar == 'i')
	     && (cap.oap.op_type != OP_NOP || G.VIsual_active))
    {
        nv_object(cap);
      //clearopbeep(cap.oap);
    } else if (!checkclearopq(cap.oap) && u_save_cursor() == OK) {
      Misc.beginInsertUndo();
      nv_edit_dispatch(cap);
      return;
    }
    return;
  }

  /**
   * Enter regualar edit mode, moved here so could be invoked
   * as an atomic operation.
   */
  // can't be private, if want to dispatch
  static private void nv_edit_dispatch (CMDARG cap)
  {
    switch (cap.cmdchar) {
      case 'A':	/* "A"ppend after the line */
        G.curwin.setWSetCurswant(true);
        Util.endLine();
        // G.op.xop(ViOp.THIS_END_LINE, this);
        break;

      case 'I':	/* "I"nsert before the first non-blank */
        Edit.beginline(BL_WHITE);
        break;

      case 'a':	/* "a"ppend is like "i"nsert on the next character. */
        int offset = G.curwin.getWCursor().getOffset();
        int c = Util.getCharAt(offset);
        if(c != '\n') {
          G.curwin.setCaretPosition(offset+1);
        }
        break;
    }
    Edit.edit(cap.cmdchar, false, cap.count1);
    return;
  }

  static private  void	nv_object (CMDARG cap) {
      do_op("nv_object");
      int flag;
      boolean include;
      //char_u *mps_save;
      String mps_save;

      if (cap.cmdchar == 'i')
          include = false;    /* "ix" = inner object: exclude white space */
      else
          include = true;    /* "ax" = an object: include white space */

      /* Make sure (), [], {} and <> are in 'matchpairs' */
      mps_save = G.curbuf.b_p_mps;
      G.curbuf.b_p_mps = "(:),{:},[:],<:>";

      switch (cap.nchar)
      {
//         case 'w': /* "aw" = a word */
//             flag = current_word(cap.oap, cap.count1, include, false);
//             break;
//         case 'W': /* "aW" = a WORD */
//             flag = current_word(cap.oap, cap.count1, include, true);
//             break;
//         case 'b': /* "ab" = a braces block */
//         case '(':
//         case ')':
//             flag = current_block(cap.oap, cap.count1, include, '(', ')');
//             break;
//         case 'B': /* "aB" = a Brackets block */
//         case '{':
//         case '}':
//             flag = current_block(cap.oap, cap.count1, include, '{', '}');
//             break;
//         case '[': /* "a[" = a [] block */
//         case ']':
//             flag = current_block(cap.oap, cap.count1, include, '[', ']');
//             break;
//         case '<': /* "a<" = a <> block */
//         case '>':
//             flag = current_block(cap.oap, cap.count1, include, '<', '>');
//             break;
         case 'p': /* "ap" = a paragraph */
             flag = Search.current_par(cap.oap, cap.count1, include, 'p');
             break;
//         case 's': /* "as" = a sentence */
//             flag = current_sent(cap.oap, cap.count1, include);
//             break;
//             //#if 0­  /* TODO */
//             //­       case 'S': /* "aS" = a section */
//             //­       case 'f': /* "af" = a filename */
//             //­       case 'u': /* "au" = a URL */
//             //#endif
          default:
              flag = FAIL;
              break;
      }

      G.curbuf.b_p_mps = mps_save;
      if (flag == FAIL)
          clearopbeep(cap.oap);
      //adjust_cursor_col();
      G.curwin.setWSetCurswant(true);

  }

  /**
   * Handle the "q" key.
   */
  static private void nv_q(CMDARG cap) throws NotSupportedException {
    do_xop("nv_q");
    if (cap.oap.op_type == OP_FORMAT) {
      notSup("gqq");
      /* "gqq" is the same as "gqgq": format line */
      cap.cmdchar = 'g';
      cap.nchar = 'q';
      nv_operator(cap);
    }
    else if (!checkclearop(cap.oap))
    {
      /* (stop) recording into a named register */
      /* command is ignored while executing a register */
      if (!G.Exec_reg && Misc.do_record(cap.nchar) == FAIL)
	clearopbeep(cap.oap);
    }
  }

  /**
   * Handle the "@r" command.
   */
  static private void nv_at(CMDARG cap) {
    do_xop("nv_at");
    if (checkclearop(cap.oap))
      return;
    while (cap.count1-- > 0)
    {
      if (Misc.do_execreg(cap.nchar, false, false) == FAIL)
      {
	clearopbeep(cap.oap);
	break;
      }
    }
  }

  /**
   * Handle the CTRL-U and CTRL-D commands.
   */
  static private void nv_halfpage(CMDARG cap) {
    ViFPOS fpos = G.curwin.getWCursor();
    if ((cap.cmdchar == Util.ctrl('U') && fpos.getLine() == 1)
	|| (cap.cmdchar == Util.ctrl('D')
	    && fpos.getLine() == G.curwin.getLineCount()))
      clearopbeep(cap.oap);
    else if (!checkclearop(cap.oap))
      Misc.halfpage(cap.cmdchar == Util.ctrl('D'), cap.count0);
  }

  /**
   * Handle "J" or "gJ" command.
   */
  static private void nv_join(CMDARG cap) {
    if (G.VIsual_active)	/* join the visual lines */
      nv_operator(cap);
    else if (!checkclearop(cap.oap)) {
      if (cap.count0 <= 1)
	cap.count0 = 2;	    /* default for join is two lines! */
      if (G.curwin.getWCursor().getLine() + cap.count0 - 1 >
	  		G.curwin.getLineCount())
	clearopbeep(cap.oap);  /* beyond last line */
      else
      {
	prep_redo(cap.oap.regname, cap.count0,
		  NUL, cap.cmdchar, NUL, cap.nchar);
        Misc.beginUndo();
        try {
          Misc.do_do_join(cap.count0, cap.nchar == NUL, true);
        } finally {
          Misc.endUndo();
        }
      }
    }
  }

  /**
   * "P", "gP", "p" and "gp" commands.
   */
  static private void nv_put(CMDARG cap) {
    if (cap.oap.op_type != OP_NOP || G.VIsual_active) {
      clearopbeep(cap.oap);
    } else {
      prep_redo_cmd(cap);
      Misc.beginUndo();
      try {
        Misc.do_put(cap.oap.regname,
                    (cap.cmdchar == 'P'
                     || (cap.cmdchar == 'g' && cap.nchar == 'P'))
                    ? BACKWARD : FORWARD,
                    cap.count1, cap.cmdchar == 'g' ? PUT_CURSEND : 0);
      } finally {
        Misc.endUndo();
      }
    }
  }

  //
  //
  //
  //
  //
  //

  static int u_save_cursor() {/*do_op("u_save_cursor");*/return OK; }

  static void start_selection() {do_op("start_selection");}
  // static void onepage(int dir, int count) {do_op("onepage");}
  static void do_help(String s) {do_op("do_help");}
  static boolean buflist_getfile(int n, int lnum, int options, boolean forceit) {
    do_op("buflist_getfile");return true;
  }
  static void update_curbuf(int type) {do_op("update_curbuf");}
  static void redraw_curbuf_later(int type) {do_op("redraw_curbuf_later");}
  static void do_tag(String tag, int type, int count,
	      boolean forceit, boolean verbose) {do_op("do_tag");}
  // static int start_redo(int count, boolean type) { do_op("start_redo");return OK; }
  // static void u_undo(int count) {do_op("u_undo");}
  // static void u_redo(int count) {do_op("u_redo");}
  static int do_addsub(int command, int Prenum1) { do_op("do_addsub");return OK; }
  //static void stuffReadbuff(String s) {do_op("stuffReadbuff");}
  //static void do_window(int nchar, int Prenum) {do_op("do_window");}
  // void do_pending_operator(CMDARG cap, CharBuf searchbuf,
  // 			   MutableBoolean mb, int old_col,
  // 			   boolean gui_yank, boolean dont_adjust_op_end)
  // 			   	{do_op("do_pending_operator");}
  static boolean typebuf_typed() { do_op("typebuf_typed");return true; }
  static boolean msg_attr(String s, int attr) { do_op("msg_attr");return true; }
  static void setcursor() {do_op("setcursor");}
  static void cursor_on() {do_op("curson_on");}
  static void ui_delay(int msec, boolean ignoreinput) {do_op("ui_delay");}
  static void update_other_win() {do_op("update_other_win");}
  // void checkpcmark() {do_op("checkpcmark");}
  // boolean edit(int cmdchar, boolean startln, int count) { do_op("edit");return true; }
  // static void stuffcharReadbuff(int c) {do_op("stuffcharReadbuff");}

  //
  //
  //
  //
  //
  //

  //static boolean Recording;
  //static boolean Exec_reg;
  static int p_im;
  static boolean msg_didout;
  static int msg_col;
  static boolean arrow_used;
  static boolean redraw_cmdline;
  static boolean msg_didany;
  static boolean msg_nowait;
  static boolean KeyTyped;
  static boolean msg_scroll;
  static boolean emsg_on_display;
  static boolean must_redraw;
  static String keep_msg;
  static int keep_msg_attr;
  static boolean modified;

  static boolean bangredo;



  /**
   * The Visual area is remembered for reselection.
   */
  static int	resel_VIsual_mode = NUL;	/* 'v', 'V', or Ctrl-V */
  static int	resel_VIsual_line_count;/* number of lines */
  static int	resel_VIsual_col;	/* nr of cols or end col */

  // static private  void	op_colon (OPARG oap) {do_op("op_colon");}
  // private  void	prep_redo_cmd (CMDARG cap) {do_op("prep_redo_cmd");}
  //static private  void	prep_redo (int regname, long l,
		  //int p0, int p1, int p2, int p3) {do_op("prep_redo");}
  // private  boolean checkclearop (OPARG oap) { do_op("checkclearop");return true; }
  // private  boolean checkclearopq (OPARG oap) { do_op("checkclearopq");return true; }
  // private  void	clearop (OPARG oap) {do_op("clearop");}
  // private  void	clearopbeep (OPARG oap) {do_op("clearopbeep");}
  // private  void	del_from_showcmd (int arg) {do_op("del_from_showcmd");}

/*
 * nv_*(): functions called to handle Normal and Visual mode commands.
 * n_*(): functions called to handle Normal mode commands.
 * v_*(): functions called to handle Visual mode commands.
 */
  static private  void	nv_gd (OPARG oap, int nchar) {do_op("nv_gd");}
  static private  int	nv_screengo (OPARG oap, int dir, long dist) { do_op("nv_screengo");return 0; }
  // static private  void	nv_scroll_line (CMDARG cap, boolean is_ctrl_e) {do_op("nv_scroll_line");}
  // static private  void	nv_zet (CMDARG cap) {do_op("nv_zet");}
  // static private  void	nv_colon (CMDARG cap) {do_op("nv_colon");}
  // static private  void	nv_ctrlg (CMDARG cap) {do_op("nv_ctrlg");}
  static private  void	nv_zzet (CMDARG cap) {do_op("nv_zzet");}
  // static private  void	nv_ident (CMDARG cap, CharBuf searchp) {do_op("nv_ident");}
  // static private  void	nv_scroll (CMDARG cap) {do_op("nv_scroll");}
  // private  void	nv_right (CMDARG cap) {do_op("nv_right");}
  // private  boolean nv_left (CMDARG cap) { do_op("nv_left");return true; }
  static private  void	nv_gotofile (CMDARG cap) {do_op("nv_gotofile");}
  // private  void	nv_dollar (CMDARG cap) {do_op("nv_dollar");}
  //static private  void	nv_search (CMDARG cap, CharBuf searchp,
  //			   boolean dont_set_mark) {do_op("nv_search");}
  // static private  void	nv_next (CMDARG cap, int flag) {do_op("nv_next");}
  // private  void	nv_csearch (CMDARG cap, int dir, boolean type) {do_op("nv_csearch");}
  static private  void	nv_brackets (CMDARG cap, int dir) {do_op("nv_brackets");}
  // static private  void	nv_percent (CMDARG cap) {do_op("nv_percent");}
  // static private  void	nv_brace (CMDARG cap, int dir) {do_op("nv_brace");}
  //  static private  void	nv_findpar (CMDARG cap, int dir) {do_op("nv_findpar");}
  // private  boolean nv_Replace (CMDARG cap) { do_op("nv_Replace");return true; }
  static private  int	nv_VReplace (CMDARG cap) { do_op("nv_VReplace");return 0; }
  static private  int	nv_vreplace (CMDARG cap) { do_op("nv_vreplace");return 0; }
  //static private  void	v_swap_corners (CMDARG cap) {
  // static private  boolean nv_replace (CMDARG cap) { do_op("nv_replace");return true; }
  // static private  void	n_swapchar (CMDARG cap) {do_op("n_swapchar");}
  // private  void	nv_cursormark (CMDARG cap, boolean flag, ViFPOS pos) {do_op("nv_cursormark");}
  // static private  void	v_visop (CMDARG cap) {
  // private  void	nv_optrans (CMDARG cap) {do_op("nv_optrans");}
  // private  void	nv_gomark (CMDARG cap, boolean flag) {do_op("nv_gomark");}
  // private  void	nv_pcmark (CMDARG cap) {do_op("nv_pcmark");}
  // private  void	nv_regname (CMDARG cap, MutableInt opnump) {do_op("nv_regname");}
  // static private  void	nv_visual(CMDARG cap, boolean selectmode) {
  //  static private  boolean nv_g_cmd (CMDARG cap, CharBuf searchp) { do_op("nv_g_cmd");return true; }
  // private  boolean n_opencmd (CMDARG cap) { do_op("n_opencmd");return true; }
  // static private  void	nv_Undo (CMDARG cap)
  // private  void	nv_operator (CMDARG cap) {do_op("nv_operator");}
  // private  void	nv_lineop (CMDARG cap) {do_op("nv_lineop");}
  // static private  void	nv_pipe (CMDARG cap) {do_op("nv_pipe");}
  // static private  void	nv_bck_word (CMDARG cap, boolean type) {do_op("nv_bck_word");}
  // static private  void	nv_wordcmd (CMDARG cap, boolean type) {do_op("nv_wordcmd");}
  // static private  void	adjust_for_sel (CMDARG cap) {do_op("adjust_for_sel");}
  static private  void	unadjust_for_sel () {do_op("unadjust_for_sel");}
  // private  void	nv_goto (CMDARG cap, long lnum) {do_op("nv_goto");}
  static private  void	nv_select (CMDARG cap) {do_op("nv_select");}
  static private  void	nv_normal (CMDARG cap) {do_op("nv_normal");}
  // private  void	nv_esc (CMDARG oap, int opnum) {do_op("nv_esc");}
  // private  boolean nv_edit (CMDARG cap) { do_op("nv_edit");return true; }
  // static private  void	nv_object (CMDARG cap);
  // static private  void	nv_q (CMDARG cap) {do_op("nv_q");}
  // static private  void	nv_at (CMDARG cap) {do_op("nv_at");}
  // static private  void	nv_halfpage (CMDARG cap) {do_op_clear("nv_halfpage", cap.oap);}
  // static private  void	nv_join (CMDARG cap) {do_op("nv_join");}
  // private  void	nv_put (CMDARG cap) {do_op("nv_put");}

  // static void op_shift(OPARG oap, boolean curs_top, int amount) {do_op("op_shift");}
  //static void do_do_join(int count, boolean insert_space, boolean redraw)
    //{do_op("do_do_join");}
  // int op_delete(OPARG oap) {do_op("op_delete"); return OK;}
  // int op_yank(OPARG oap, boolean deleting, boolean mess) {
    // do_op("op_yank"); return OK; }
  // static boolean op_change(OPARG oap) {do_op("op_change"); return false;}
  // static void op_tilde(OPARG oap) {do_op("op_tilde");}
  static void op_format(OPARG oap) {do_op("op_format");}


  // static int typebuf_maplen() { do_op("typebuf_maplen");return 0; }
  static void do_exmode() {do_op("do_exmode");}
  static void update_screen(boolean flag) {do_op("update_screen(bool)");}
  static void update_screen(int flag) {do_op("update_screen(int)");}
  // void AppendToRedobuff(String s) {do_op("AppendToRedobuff");}
  static boolean inindent(int extra) { do_op("inindent");return false; }
  // int coladvance(int wcol) { do_op("coladvance"); return OK; }



  static class Opchar {
    char c1, c2;
    boolean lines;

    Opchar(char c1, char c2, boolean lines) {
      this.c1 = c1;
      this.c2 = c2;
      this.lines = lines;
    }
  }

  /*
   * The names of operators.  Index must correspond with defines in vim.h!!!
   * The third field indicates whether the operator always works on lines.
   */
  static Opchar[] opchars = new Opchar[]
  {
      new Opchar('\000', '\000', false),/* OP_NOP */
      new Opchar('d', '\000', false),	/* OP_DELETE */
      new Opchar('y', '\000', false),	/* OP_YANK */
      new Opchar('c', '\000', false),	/* OP_CHANGE */
      new Opchar('<', '\000', true),	/* OP_LSHIFT */
      new Opchar('>', '\000', true),	/* OP_RSHIFT */
      new Opchar('!', '\000', true),	/* OP_FILTER */
      ////////////////new Opchar('g', '~', false),	/* OP_TILDE */
      new Opchar('~', '\000', false),	/* OP_TILDE */
      new Opchar('=', '\000', true),	/* OP_INDENT */
      new Opchar('g', 'q', true),	/* OP_FORMAT */
      new Opchar(':', '\000', true),	/* OP_COLON */
      new Opchar('g', 'U', false),	/* OP_UPPER */
      new Opchar('g', 'u', false),	/* OP_LOWER */
      new Opchar('J', '\000', true),	/* DO_JOIN */
      new Opchar('g', 'J', true),	/* DO_JOIN_NS */
      new Opchar('g', '?', false),	/* OP_ROT13 */
      new Opchar('r', '\000', false),	/* OP_REPLACE */
      new Opchar('I', '\000', false),	/* OP_INSERT */
      new Opchar('A', '\000', false)	/* OP_APPEND */
  };

  /**
   * Translate a command name into an operator type.
   * Must only be called with a valid operator name!
   */
  static int get_op_type(int char1, int char2) {
      int		i;

      if (char1 == 'r')		/* ignore second character */
	  return OP_REPLACE;
      if (char1 == '~')		/* when tilde is an operator */
	  return OP_TILDE;
      for (i = 0; ; ++i)
	  if (opchars[i].c1 == char1 && opchars[i].c2 == char2)
	      break;
      return i;
  }

  /**
   * Return TRUE if operator "op" always works on whole lines.
   */
  static boolean op_on_lines(int op) {
      return opchars[op].lines;
  }

  /**
   * Get first operator command character.
   * Returns 'g' if there is another command character.
   */
  static int get_op_char(int optype) {
      return opchars[optype].c1;
  }

  /**
   * Get second operator command character.
   */
  static int get_extra_op_char(int optype) {
      return opchars[optype].c2;
  }


  /**
   * An operation that is not yet implemented has been selected.
   * Output the op and the characters that got us here.
   */
  static void notImp(String op) throws NotSupportedException {
    // setGeneralStatus
    if(KeyBinding.notImpDebug) System.err.println("Not Implemented: "
                      + op + ": " + "\"" + getCmdChars() + "\"");
    throw new NotSupportedException(op, getCmdChars());
  }

  /**
   * An operation that is not planned support has been selected.
   * Output the op and the characters that got us here.
   */
  static void notSup(String op) throws NotSupportedException {
    // setGeneralStatus
    if(KeyBinding.notImpDebug) System.err.println("Not supported: "
                       + op + ": " + "\"" + getCmdChars() + "\"");
    throw new NotSupportedException(op, getCmdChars());
  }

  static void do_op(String op) {
    if(G.debugOpPrint) System.err.println("Exec: " + op);
  }

  static void do_xop(String op) {
    if(G.debugPrint) System.err.println("Exec: ** " + op);
  }

  static void do_op_clear(String op, OPARG oap) {
    if(G.debugPrint) System.err.println("Exec: " + op);
    clearop(oap);
  }

  /**
   * @return the command characters for the current command.
   */
  static String getCmdChars() {
    return showcmd_buf.toString();
  }
}
