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

import java.util.Stack;
import javax.swing.text.Segment;
import javax.swing.text.Element;
import javax.swing.SwingUtilities;

import com.raelity.jvi.ViManager;

import static com.raelity.jvi.KeyDefs.*;
import static com.raelity.jvi.Constants.*;

public class Edit {
  
  public static final String VI_MODE_COMMAND = "";
  public static final String VI_MODE_NORMAL= "NORMAL";
  public static final String VI_MODE_INSERT = "INSERT";
  public static final String VI_MODE_REPLACE = "REPLACE";
  public static final String VI_MODE_VREPLACE = "VREPLACE";
  public static final String VI_MODE_RESTART_I = "(insert)";
  public static final String VI_MODE_RESTART_R = "(replace)";
  public static final String VI_MODE_RESTART_V = "(vreplace)";
  
  public static final String VI_MODE_SELECT = "SELECT";
  public static final String VI_MODE_VISUAL = "VISUAL";
  public static final String VI_MODE_BLOCK = "BLOCK";
  public static final String VI_MODE_LINE = "LINE";
  
  static ViFPOS Insstart;
  static int Insstart_textlen;	// length of line when insert started
  
  static final int BACKSPACE_CHAR		= 1;
  static final int BACKSPACE_WORD		= 2;
  static final int BACKSPACE_WORD_NOT_SPACE	= 3;
  static final int BACKSPACE_LINE		= 4;
  
  //
  // Edit state information
  //
  
  static boolean need_redraw; // GET RID OF
  static int did_restart_edit;
  static int i;
  static int c;
  static int lastc;
  static boolean did_backspace;
  static MutableInt count;
  static MutableBoolean inserted_space;
  
  /**
   * edit(): Start inserting text.
   *<ul>
   *<li> "cmdchar" can be:
   *<li> 'i'	normal insert command
   *<li> 'a'	normal append command
   *<li> 'R'	replace command
   *<li> 'r'	"r<CR>" command: insert one <CR>.
   *		Note: count can be > 1, for redo,
   *		but still only one <CR> is inserted.
   *		The <Esc> is not used for redo.
   *<li> 'g'	"gI" command.
   *<li> 'V'	"gR" command for Virtual Replace mode.
   *<li> 'v'	"gr" command for single character Virtual Replace mode.
   *</ul>
   *
   * This function is not called recursively.  For CTRL-O commands, it returns
   * and lets the caller handle the Normal-mode command.
   *
   * @return true if a CTRL-O command caused the return (insert mode pending).
   */
  static void edit(int cmdchar, boolean startln, int count_arg) {
    if( ! Misc.isInInsertUndo()) {
      ViManager.dumpStack("In edit with no undo pending");
    }
    if( ! Normal.editBusy) {
      Normal.editBusy = true;
      Normal.do_xop("edit");
      count = new MutableInt(count_arg);
      
      // clear any selection so a character insert doesn't do more than wanted
      G.curwin.setCaretPosition(G.curwin.getCaretPosition());
      
      need_redraw = false; // GET RID OF
      did_restart_edit = G.restart_edit;
      c = 0;
      did_backspace = false;
      inserted_space = new MutableBoolean(false);
      
      Insstart = G.curwin.getWCursor().copy();
      Insstart_textlen = Misc.linetabsize(Util.ml_get_curline());
      
      if (cmdchar != NUL && G.restart_edit == 0) {
        GetChar.ResetRedobuff();
        GetChar.AppendNumberToRedobuff(count.getValue());
        if (cmdchar == 'V' || cmdchar == 'v') {
          /* "gR" or "gr" command */
          GetChar.AppendCharToRedobuff('g');
          GetChar.AppendCharToRedobuff((cmdchar == 'v') ? 'r' : 'R');
        } else {
          GetChar.AppendCharToRedobuff(cmdchar);
          if (cmdchar == 'g')		    /* "gI" command */
            GetChar.AppendCharToRedobuff('I');
          else if (cmdchar == 'r')	    /* "r<CR>" command */
            count.setValue(1);	    /* insert only one <CR> */
        }
      }
      
      if(cmdchar == 'R') {
        G.State = REPLACE;
      } else {
        G.State = INSERT;
      }
      
      Normal.clear_showcmd();
      
      //
      // Handle restarting Insert mode.
      // Don't do this for CTRL-O . (repeat an insert): we get here with
      // restart_edit non-zero, and something in the stuff buffer.
      //
      if (G.restart_edit != 0 && GetChar.stuff_empty()) {
        // stuff...
        throw new RuntimeException("jVi: restart_edit non-zero");
      } else {
        arrow_used = false;
        // o_eol = FALSE;
      }
      
      i = 0;
      if(G.p_smd.getBoolean()) {
        i = Misc.showmode();
      }
      
      if (did_restart_edit == 0) {
        Misc.change_warning(i + 1);
      }
      
      Misc.ui_cursor_shape();
      return;
    }
    
    c = cmdchar;        // when actually doing editting cmdchar is the input.
    
    // whole lot of stuff deleted
    //	if (c == Ctrl('V') || c == Ctrl('Q'))
    
    normal_char:	// do "break normal_char" to insert a character
            while(true) {
      try {
        
        G.curwin.setWSetCurswant(true);
        
        // if (need_redraw && curwin.w_p_wrap && !did_backspace
        // && curwin.w_topline == old_topline)
        
        // ....
        Misc.update_curswant();
        // .....
        
        lastc = c; // NEEDSWORK: use of lastc not supported
        // c = GetChar.safe_vgetc();
        
        // skip ctrl-\ ctrlN to normal mode
        // NEEDSWORK: ctrl-V ctrl-Q
        // skip some indent stuff
        // skip if(has_startsel)
        
        int dir = FORWARD;
        c = Misc.xlateViKey(ViManager.getInsertModeKeymap(), c, G.getModMask());
        switch(c) {
          case IM_INS_REP:
            //case K_INS:	    // toggle insert/replace mode
            if (G.State == REPLACE) { G.State = INSERT; }
            else { G.State = REPLACE; }
            GetChar.AppendCharToRedobuff(c);
            Misc.showmode();
            Misc.ui_cursor_shape();	// may show different cursor shape
            break;
            
            // case Ctrl('X'):	    // Enter ctrl-x mode
            // case K_SELECT:	    // end of Select mode mapping - ignore
            // case Ctrl('Z'):	    // suspend when 'insertmode' set
            // case Ctrl('O'):	    // execute one command
            // case K_HELP:
            // case K_F1:
            // case K_XF1:
            
            //
            // Insert the previously inserted text.
            // For ^@ the trailing ESC will end the insert, unless there
            // is an error.
            //
            // case K_ZERO:
            // case NUL:
            // case Ctrl('A'):
            
          /*
            // insert the contents of a register
          case 0x1f & (int)('R'):	// Ctrl
            ins_reg();
            break;
           */
            
            // Make indent one shiftwidth smaller.
          case IM_SHIFT_LEFT:
            //case 0x1f & (int)('D'):	// Ctrl
            dir = BACKWARD;
            // FALLTHROUGH
            
            // Make indent one shiftwidth greater.
          case IM_SHIFT_RIGHT:
            //case 0x1f & (int)('T'):	// Ctrl
            ins_shift(c, lastc, dir);
            // need_redraw = TRUE;
            break;
            
            // shift line to be under char after next
            // parenthesis in previous line
          case IM_SHIFT_RIGHT_TO_PAREN:
            //case K_X_PERIOD:
            if( c == IM_SHIFT_RIGHT_TO_PAREN
                    || c == K_X_PERIOD && G.getModMask() == CTRL) {
              ins_shift_paren(c, FORWARD);
            }
            break;
            
            // shift line to be under char after previous
            // parenthesis in previous line
          case IM_SHIFT_LEFT_TO_PAREN:
            //case K_X_COMMA:
            if( c == IM_SHIFT_LEFT_TO_PAREN
                    || c == K_X_COMMA && G.getModMask() == CTRL) {
              ins_shift_paren(c, BACKWARD);
            }
            break;
            
          /*
            // delete character under the cursor
          case K_DEL:
            ins_del();
            // need_redraw = TRUE;
            break;
           */
            
            // delete character before the cursor
            //case K_S_BS:
            //c = K_BS;
            // FALLTHROUGH
            
          case K_BS:
          case 0x1f & (int)('H'):	// Ctrl
            did_backspace = ins_bs(c, BACKSPACE_CHAR, inserted_space);
            // need_redraw = TRUE;
            break;
            
          /*
            // delete word before the cursor
          case 0x1f & (int)('W'):	// Ctrl
            did_backspace = ins_bs(c, BACKSPACE_WORD, inserted_space);
            Normal.notImp("backspace_word");
            // need_redraw = TRUE;
            break;
           
            // delete all inserted text in current line
          case 0x1f & (int)('U'):	// Ctrl
            did_backspace = ins_bs(c, BACKSPACE_LINE, inserted_space);
            Normal.notImp("backspace_line");
            // need_redraw = TRUE;
            break;
           
          case K_HOME:
            // case K_KHOME:
            // case K_XHOME:
          case K_S_HOME:
            ins_home();
            break;
           
          case K_END:
            // case K_KEND:
            // case K_XEND:
          case K_S_END:
            ins_end();
            break;
           
          case K_LEFT:
            if ((G.mod_mask & MOD_MASK_CTRL) != 0)
              ins_s_left();
            else
              ins_left();
            break;
           
          case K_S_LEFT:
            ins_s_left();
            break;
            
          case K_RIGHT:
            if ((G.mod_mask & MOD_MASK_CTRL) != 0)
              ins_s_right();
            else
              ins_right();
            break;
            
          case K_S_RIGHT:
            ins_s_right();
            break;
            
          case K_UP:
            ins_up();
            break;
             
          case K_S_UP:
          case K_PAGEUP:
            // case K_KPAGEUP:
            ins_pageup();
            break;
             
          case K_DOWN:
            ins_down();
            break;
             
          case K_S_DOWN:
          case K_PAGEDOWN:
            // case K_KPAGEDOWN:
            ins_pagedown();
            break;
             */
            
            // keypad keys: When not mapped they produce a normal char
          case K_KPLUS:
            c = '+'; break normal_char;
          case K_KMINUS:
            c = '-'; break normal_char;
          case K_KDIVIDE:
            c = '/'; break normal_char;
          case K_KMULTIPLY:
            c = '*'; break normal_char;
            
            // When <S-Tab> isn't mapped, use it like a normal TAB
          case K_S_TAB:
            // FALLTHROUGH
            
          case K_TAB:
            c = TAB;
            
            // TAB or Complete patterns along path
          case TAB:
            // NEEDSWORK: no ins_tab, so noexpandtab doesn't work.
            // Just handle it as a normal character
            break normal_char;
            
          case K_KENTER:
            c = CR;
            // FALLTHROUGH
          case CR:
          case NL:
            //  p_im! wow. Out of memory should be a throw...
            // if (ins_eol(c) && !p_im) goto doESCkey;    // out of memory
            ins_eol(c);
            break;
            
            // case Ctrl('K'): // Enter digraph
            // case Ctrl(']'): // Tag name completion after ^X
            // case Ctrl('F'): // File name completion after ^X
            // case Ctrl('L'): // Whole line completion after ^X
            // case Ctrl('P'): // Do previous pattern completion
            // case Ctrl('N'): // Do next pattern completion
            // case Ctrl('Y'): // copy from previous line or scroll down
            // case Ctrl('E'): // copy from next line	   or scroll up
            
            //case K_S_SPACE:
            //c = ' ';
            // FALTHROUGH TO "default" ignore the ESC case....
            
          case ESC:	    // an escape ends input mode
            //
            // NOTE: FALLTHROUGH TO "default", check for escape there
            //
            // if (echeck_abbr(ESC + ABBR_OFF))
            // break;
            //FALLTHROUGH
            
            // case 0x1f & (int)('C'):	// Ctrl
            // when 'insertmode' set, and not halfway a mapping, don't leave
            // Insert mode.
            // REMOVE insertmode stuff
            
            //
            // This is the ONLY return from edit() (WHERE editBusy SET false)
            //
            /* **** this is the code moved to default case ****
            if (ins_esc(count, need_redraw, cmdchar)) {
              Normal.editBusy = false;
              // return (c == (0x1f & (int)('O')));
            }
            return;
             */
            //continue;
            
          default:
            // Virtual keys should not be put into the file.
            // Exit input mode if they are seen
            if(cmdchar == ESC
                    || (cmdchar & 0xF000) == VIRT
                    || cmdchar > 0xFFFF) {
              // This is the identical code as <ESC>
              if (ins_esc(count, need_redraw, cmdchar)) {
                Normal.editBusy = false;
                // return (c == (0x1f & (int)('O')));
              }
              return;
            }
            break normal_char;
        } // switch
        // break out of switch, process another character
        // continue edit_loop;
        return;
        
      } catch(NotSupportedException e) {
        // brutal exit from input mode
        ins_esc(count, need_redraw, cmdchar);
        Normal.editBusy = false;
        return; // ignore the character
      }
      
            } //  normal_char: while(true), so a "break normal_char" goes to next line
    
    // just do something simple for now
    // if(vim_iswordc(c) || !echeck_abbr(c))
    
    // NEEDSWORK: better filter to prevent funny chars from input
    // Oh hell, caveat programmer
    // if(c != '\t' && (c < ' ' || c == 7f)) {// WAS: c > 0x7e)) {
    if(c == 0) {
      // filter out most control chars and del
      // continue edit_loop;
      return;
    }
    insert_special(c, false, false);
    //continue edit_loop;
    return;
  }
  
  /**
   * Call this function before moving the cursor from the normal insert position
   * in insert mode.
   */
  static void undisplay_dollar() {
    /*if (dollar_vcol)
    {
      dollar_vcol = 0;
      update_screenline();
    }*/
  }
  
  /**
   * Insert an indent (for <Tab> or CTRL-T) or delete an indent (for CTRL-D).
   * Keep the cursor on the same character.
   * type == INDENT_INC	increase indent (for CTRL-T or <Tab>)
   * type == INDENT_DEC	decrease indent (for CTRL-D)
   * type == INDENT_SET	set indent to "amount"
   * if round is TRUE, round indent to 'shiftwidth' (only with _INC and _Dec).
   * @param replaced replaced character, put on replace stack
   */
  static void change_indent(int type, int amount, boolean round, int replaced) {
    /*
       int		vcol;
       int		last_vcol;
       int		i;
       char_u	*ptr;
       int		save_p_list;
       colnr_t	vc;
       colnr_t	orig_col = 0;		// init for GCC
       char_u	*new_line, *orig_line = NULL;	// init for GCC
     */
    int		new_cursor_col;
    int		start_col;
    int		insstart_less;		// reduction for Insstart.col
    boolean	save_p_list;
    
    /*
    // VREPLACE mode needs to know what the line was like before changing
    if (State == VREPLACE)
    {
      orig_line = vim_strsave(ml_get_curline());  // Deal with NULL below
      orig_col = curwin->w_cursor.col;
    }
     
    // for the following tricks we don't want list mode
    save_p_list = G.curwin.getWPList();
    G.curwin.setWPList(false);
    getvcol(curwin, &curwin->w_cursor, NULL, &vc, NULL);
    vcol = vc;
     */
    
    //
    // For Replace mode we need to fix the replace stack later, which is only
    // possible when the cursor is in the indent.  Remember the number of
    // characters before the cursor if it's possible.
    //
    start_col = G.curwin.getWCursor().getColumn();
    
    // determine offset from first non-blank
    new_cursor_col = G.curwin.getWCursor().getColumn();
    beginline(BL_WHITE);
    new_cursor_col -= G.curwin.getWCursor().getColumn();
    
    insstart_less = G.curwin.getWCursor().getColumn();
    
    /*
    //
    // If the cursor is in the indent, compute how many screen columns the
    // cursor is to the left of the first non-blank.
    //
    if (new_cursor_col < 0)
      vcol = get_indent() - vcol;
     
    if (new_cursor_col > 0)	    // can't fix replace stack
     */          // following == -1 means can't fix replace stack
    start_col = -1;
    
    //
    // Set the new indent.  The cursor will be put on the first non-blank.
    //
    if (type == INDENT_SET)
      Misc.set_indent(amount, true);
    else
      Misc.shift_line(type == INDENT_DEC, round, 1);
    insstart_less -= G.curwin.getWCursor().getColumn();
    
    //
    // Try to put cursor on same character.
    // If the cursor is at or after the first non-blank in the line,
    // compute the cursor column relative to the column of the first
    // non-blank character.
    // If we are not in insert mode, leave the cursor on the first non-blank.
    // If the cursor is before the first non-blank, position it relative
    // to the first non-blank, counted in screen columns.
    //
    // NEEDSWORK: change_indent: if cursor before non-blank, leave on first
    if (new_cursor_col >= 0) {
      //
      // When changing the indent while the cursor is touching it, reset
      // Insstart_col to 0.
      //
      if (new_cursor_col == 0)
        insstart_less = MAXCOL;
      new_cursor_col += G.curwin.getWCursor().getColumn();
    } else
      new_cursor_col = G.curwin.getWCursor().getColumn();
    /* ***************************************************************
    else if (!(State & INSERT))
      new_cursor_col = G.curwin.getWCursor().getColumn();
    else
    {
      //
      // Compute the screen column where the cursor should be.
      //
      vcol = get_indent() - vcol;
      curwin->w_virtcol = (vcol < 0) ? 0 : vcol;
     
      //
      // Advance the cursor until we reach the right screen column.
      //
      vcol = last_vcol = 0;
      new_cursor_col = -1;
      ptr = ml_get_curline();
      while (vcol <= (int)curwin->w_virtcol)
      {
        last_vcol = vcol;
        ++new_cursor_col;
        vcol += lbr_chartabsize(ptr + new_cursor_col, (colnr_t)vcol);
      }
      vcol = last_vcol;
     
      //
      // May need to insert spaces to be able to position the cursor on
      // the right screen column.
      //
      if (vcol != (int)curwin->w_virtcol)
      {
        curwin->w_cursor.col = new_cursor_col;
        i = (int)curwin->w_virtcol - vcol;
        ptr = alloc(i + 1);
        if (ptr != NULL)
        {
          new_cursor_col += i;
          ptr[i] = NUL;
          while (--i >= 0)
            ptr[i] = ' ';
          ins_str(ptr);
          vim_free(ptr);
        }
      }
     
      //
      // When changing the indent while the cursor is in it, reset
      // Insstart_col to 0.
      //
      insstart_less = MAXCOL;
    }
     
    G.curwin.setWPList(save_p_list);
     ********************************************************************/
    
    int col;
    if (new_cursor_col <= 0)
      col = 0;
    else
      col = new_cursor_col;
    G.curwin.setCaretPosition(G.curwin.getWCursor().getLine(), col);
    G.curwin.setWSetCurswant(true);
    
    // changed_cline_bef_curs();
    
    /* ********************************************************************
    //
    // May have to adjust the start of the insert.
    //
    if (State & INSERT)
    {
      if (curwin->w_cursor.lnum == Insstart.lnum && Insstart.col != 0)
      {
        if ((int)Insstart.col <= insstart_less)
          Insstart.col = 0;
        else
          Insstart.col -= insstart_less;
      }
      if ((int)ai_col <= insstart_less)
        ai_col = 0;
      else
        ai_col -= insstart_less;
    }
     
    //
    // For REPLACE mode, may have to fix the replace stack, if it's possible.
    // If the number of characters before the cursor decreased, need to pop a
    // few characters from the replace stack.
    // If the number of characters before the cursor increased, need to push a
    // few NULs onto the replace stack.
    //
    if (State == REPLACE && start_col >= 0)
    {
      while (start_col > (int)curwin->w_cursor.col)
      {
        replace_join(0);	    // remove a NUL from the replace stack
        --start_col;
      }
      while (start_col < (int)curwin->w_cursor.col || replaced)
      {
        replace_push(NUL);
        if (replaced)
        {
          replace_push(replaced);
          replaced = NUL;
        }
        ++start_col;
      }
    }
     
    //
    // For VREPLACE mode, we also have to fix the replace stack.  In this case
    // it is always possible because we backspace over the whole line and then
    // put it back again the way we wanted it.
    //
    if (State == VREPLACE)
    {
      // If orig_line didn't allocate, just return.  At least we did the job,
      // even if you can't backspace.
      //
      if (orig_line == NULL)
        return;
     
      // Save new line
      new_line = vim_strsave(ml_get_curline());
      if (new_line == NULL)
        return;
     
      // We only put back the new line up to the cursor
      new_line[curwin->w_cursor.col] = NUL;
     
      // Put back original line
      ml_replace(curwin->w_cursor.lnum, orig_line, FALSE);
      curwin->w_cursor.col = orig_col;
     
      // Backspace from cursor to start of line
      backspace_until_column(0);
     
      // Insert new stuff into line again
      vr_virtcol = MAXCOL;
      ptr = new_line;
      while (*ptr != NUL)
        ins_char(*ptr++);
     
      vim_free(new_line);
    }
     ************************************************************/
  }
  
  /**
   * Insert character, taking care of special keys and mod_mask.
   */
  
  private static void insert_special(int c,
                                     boolean allow_modmask,
                                     boolean ctrlv) {
    Normal.do_xop("insert_special: " + (char)c);
    
    // NEEDSWORK: edit: insert_special
    // Special function key, translate into "<Key>". Up to the last '>' is
    // inserted with ins_str(), so as not to replace characters in replace
    // mode.
    // Only use mod_mask for special keys, to avoid things like <S-Space>,
    // unless 'allow_modmask' is TRUE.
    //
    /* *********************************************************************
    if (IS_SPECIAL(c) || (mod_mask && allow_modmask)) {
      p = get_special_key_name(c, mod_mask);
      len = STRLEN(p);
      c = p[len - 1];
      if (len > 2) {
        p[len - 1] = NUL;
        ins_str(p);
        AppendToRedobuff(p);
        ctrlv = FALSE;
      }
    }
     ***********************************************************************/
    insertchar(c, false, -1, ctrlv);
  }
  
  static void insertchar(int c, boolean force_formatting,
          int second_indent, boolean ctrlv) {
    
    // NEEDSWORK: edit: insertchar
    // If there's any pending input, grab up to INPUT_BUFLEN at once.
    // This speeds up normal text input considerably.
    // Don't do this when 'cindent' is set, because we might need to re-indent
    // at a ':', or any other character.
    //
    
    stop_arrow();
    Misc.ins_char(c);
    GetChar.AppendCharToRedobuff(c);
  }
  
  static boolean arrow_used;
  /**
   * start_arrow() is called when an arrow key is used in insert mode.
   * It resembles hitting the <ESC> key.
   */
  static void start_arrow(ViFPOS end_insert_pos) {
    if (!arrow_used)	    /* something has been inserted */
    {
      GetChar.AppendToRedobuff(ESC_STR);
      arrow_used = true;	// this means we stopped the current insert
      stop_insert(end_insert_pos);
    }
  }
  
  /**
   * stop_arrow() is called before a change is made in insert mode.
   * If an arrow key has been used, start a new insertion.
   */
  static void stop_arrow() {
    if (arrow_used) {
      try {
        Misc.endInsertUndo();
        Normal.u_save_cursor();    // errors are ignored!
        Insstart = G.curwin.getWCursor().copy();    // new insertion starts here
        Insstart_textlen = Misc.linetabsize(Util.ml_get_curline());
        //ai_col = 0;
        assert(G.State != VREPLACE);
        if (G.State == VREPLACE) {
          // orig_line_count = curbuf->b_ml.ml_line_count;
          // vr_lines_changed = 1;
          // vr_virtcol = MAXCOL;
        }
        GetChar.ResetRedobuff();
        GetChar.AppendToRedobuff("1i");   // pretend we start an insertion
        arrow_used = false;
      } finally {
        Misc.beginInsertUndo();
      }
    }
  }
  
  private static void stop_insert(ViFPOS end_insert_pos) {
    GetChar.stop_redo_ins();
    replace_flush();		// abandon replace stack
  }
  
  /**
   * Move cursor to start of line.
   *<ul>
   *<li> if flags & BL_WHITE	move to first non-white
   *<li> if flags & BL_SOL	move to first non-white if startofline is set,
   *			        otherwise keep "curswant" column
   *<li> if flags & BL_FIX	don't leave the cursor on a NUL.
   *</ul>
   * <br><b>NEEDSWORK:</b><ul>
   * <li>This belongs as part of the document since the Content and
   * hence the segment can not be accessed publicly, only protected.
   * </ul>
   */
  public static final int beginlineColumnIndex(int flags, Segment txt) {
    int index;
    if ((flags & BL_SOL) != 0 && G.p_notsol.getBoolean()) {
      index = Misc.coladvanceColumnIndex(G.curwin.getWCurswant(), txt);
    } else {
      index = 0;
      if((flags & (BL_WHITE | BL_SOL)) != 0) {
        for(int ptr = txt.offset
                ; Misc.vim_iswhite(txt.array[ptr])
                && !(((flags & BL_FIX) != 0) && txt.array[ptr+1] == '\n')
                ; ++ptr) {
          ++index;
        }
      }
      G.curwin.setWSetCurswant(true);
    }
    return index;
  }
  
  /**
   * <b>NEEDSWORK:</b><ul>
   * <li>This often follows an operation that get the segment,
   * and here we are getting it again in BEGIN_LINE.
   * </ul>
   */
  public static final void beginline(int flags) {
    Normal.do_xop("beginline");
    int line = G.curwin.getWCursor().getLine();
    Element elem = G.curwin.getLineElement(line);
    Segment seg = G.curwin.getLineSegment(line);
    int offset = elem.getStartOffset() + beginlineColumnIndex(flags, seg);
    G.curwin.setCaretPosition(offset);
  }
  
  
  /**
   * oneright oneleft cursor_down cursor_up
   *
   * Move one char {right,left,down,up}.
   * Return OK when successful, FAIL when we hit a line of file boundary.
   */
  
  public static int oneright() {
    FPOS fpos = G.curwin.getWCursor();
    int lnum = fpos.getLine();
    int col = fpos.getColumn();
    Segment seg = G.curwin.getLineSegment(lnum);
    if(seg.array[seg.offset + col++] == '\n'	// not possible, maybe if input?
            || seg.array[seg.offset + col] == '\n'
            || col >= seg.count - 1) {
      return FAIL;
    }
    
    G.curwin.setWSetCurswant(true);
    G.curwin.setCaretPosition(fpos.getOffset() + 1);
    return OK;
  }
  
  public static int oneleft() {
    FPOS fpos = G.curwin.getWCursor();
    int col = fpos.getColumn();
    if(col == 0) {
      return FAIL;
    }
    
    G.curwin.setWSetCurswant(true);
    G.curwin.setCaretPosition(fpos.getOffset() - 1);
    return OK;
  }
  
  /**
   * When TRUE: update topline.
   */
  public static int cursor_up(int n, boolean upd_topline) {
    Normal.do_xop("cursor_up");
    int lnum = G.curwin.getWCursor().getLine();
    if (n != 0) {
      if (lnum <= 1)
        return FAIL;
      if (n >= lnum)
        lnum = 1;
      else
        lnum -= n;
    }
    
    Misc.gotoLine(lnum, -1);
    
    return OK;
  }
  
  
  /**
   * @param n the number of lines to move
   * @param upd_topline When TRUE: update topline.
   */
  public static int cursor_down(int n, boolean upd_topline) {
    Normal.do_xop("cursor_down");
    int lnum = G.curwin.getWCursor().getLine();
    if (n != 0) {
      int nline = G.curwin.getLineCount();
      if (lnum >= nline) { return FAIL; }
      lnum += n;
      if (lnum > nline) { lnum = nline; }
    }
    
   	Misc.gotoLine(lnum, -1);
    
    return OK;
  }
  
  /**
   * replace-stack functions.
   * <p>
   * When replacing characters, the replaced characters are remembered for each
   * new character.  This is used to re-insert the old text when backspacing.
   * </p><p>
   * There is a NUL headed list of characters for each character that is
   * currently in the file after the insertion point.  When BS is used, one NUL
   * headed list is put back for the deleted character.
   * </p><p>
   * For a newline, there are two NUL headed lists.  One contains the characters
   * that the NL replaced.  The extra one stores the characters after the cursor
   * that were deleted (always white space).
   * </p><p>
   * Replace_offset is normally 0, in which case replace_push will add a new
   * character at the end of the stack.  If replace_offset is not 0, that many
   * characters will be left on the stack above the newly inserted character.
   * </p>
   * <br><b>NEEDSWORK:</b><ul>
   * <li> May need to keep row/col indication for each char pushed.
   * </ul>
   * @parm c character that is replaced (NUL is none)
   */
  
  static Stack replace_stack = new Stack();
  static int replace_offset = 0;
  
  static void replace_push(int c) {
    if (replace_stack.size() < replace_offset)	/* nothing to do */
      return;
    // NEEDSWORK: maintain stack as an array of chars,
    // if (replace_stack_len <= replace_stack_nr).... extend stack..
    int idx = replace_stack.size() - replace_offset;
    replace_stack.add(idx, new Integer((int)c));
  }
  
  /**
   * call replace_push(c) with replace_offset set to the first NUL.
   */
  private static void replace_push_off(int c) {
    int idx = replace_stack.size();
    for (replace_offset = 1; replace_offset < replace_stack.size();
    ++replace_offset) {
      
      --idx;
      int item = ((Integer)replace_stack.get(idx)).intValue();
      if(item == NUL) {
        break;
      }
    }
    replace_push(c);
    replace_offset = 0;
  }
  
  /**
   * Pop one item from the replace stack.
   * return -1 if stack empty
   * return replaced character or NUL otherwise
   */
  private static int replace_pop() {
    // vr_virtcol = MAXCOL;
    if(replace_stack.size() == 0) {
      return -1;
    }
    return ((Integer)replace_stack.pop()).intValue();
  }
  
  /**
   * Join the top two items on the replace stack.  This removes to "off"'th NUL
   * encountered.
   * @param off offset for which NUL to remove
   */
  private static void replace_join(int off) {
    int	    i;
    
    for (i = replace_stack.size(); --i >= 0; ) {
      if (((Integer)replace_stack.get(i)).intValue() == NUL && off-- <= 0) {
        replace_stack.remove(i);
        return;
      }
    }
  }
  
  /**
   * Pop characters from the replace stack until a NUL is found, and insert them
   * before the cursor.  Can only be used in REPLACE or VREPLACE mode.
   */
  private static void replace_pop_ins() {
    int	    cc;
    int	    oldState = G.State;
    
    G.State = NORMAL;		/* don't want REPLACE here */
    while ((cc = replace_pop()) > 0) {
      Misc.ins_char(cc);
      Misc.dec_cursor();
    }
    G.State = oldState;
  }
  
  /**
   * make the replace stack empty
   * (called when exiting replace mode).
   */
  private static void replace_flush() {
    replace_stack.setSize(0);
  }
  
  /**
   * Handle doing a BS for one character.
   * cc &lt; 0: replace stack empty, just move cursor
   * cc == 0: character was inserted, delete it
   * cc &gt; 0: character was replace, put original back
   */
  private static void replace_do_bs() {
    int	    cc;
    
    cc = replace_pop();
    if (cc > 0) {
      Misc.pchar_cursor(cc);
      replace_pop_ins();
    } else if (cc == 0) {
      Misc.del_char(false);
    }
  }
  
  /**
   * Handle ESC in insert mode.
   * @return TRUE when leaving insert mode, FALSE when going to repeat the
   * insert.
   */
  private static boolean ins_esc(MutableInt count,
                                 boolean need_redraw,
                                 int cmdchar) {
    // .......
    
    if( ! arrow_used) {
      //
      // Don't append the ESC for "r<CR>".
      //
      if (cmdchar != 'r' && cmdchar != 'v')
        GetChar.AppendToRedobuff(ESC_STR);
      
      
      // NEEDSWORK: handle count on an insert
      
      //
      // Repeating insert may take a long time.  Check for
      // interrupt now and then.
      //
      if(count.getValue() != 0) {
        Misc.line_breakcheck();
        if (false/*got_int*/)
          count.setValue(0);
      }
      
      count.setValue(count.getValue() - 1);
      if (count.getValue() > 0) {	// repeat what was typed
        GetChar.start_redo_ins();
        // ++RedrawingDisabled;
        // disabled_redraw = TRUE;
        return false;	// repeat the insert
      }
      stop_insert(null);	// pass stop insert cursor position
      
      /* ********************************************************
      if (dollar_vcol) {
        dollar_vcol = 0;
        // may have to redraw status line if this was the
        // first change, show "[+]"
        if (curwin->w_redr_status == TRUE)
          redraw_later(NOT_VALID);
        else
          need_redraw = TRUE;
      }
       ******************************************************/
    }
    
    // ........
    
    // When an autoindent was removed, curswant stays after the indent
    // NEEDSWORK: what about autoindent in above comment?
    if (G.restart_edit == 0 /*&& (colnr_t)temp == curwin->w_cursor.col*/) {
      G.curwin.setWSetCurswant(true);
    }
    
    //
    // The cursor should end up on the last inserted character.
    //
    // NEEDSWORK: fixup cursor after insert
    ViFPOS fpos = G.curwin.getWCursor();
    if (fpos.getColumn() != 0
            && (G.restart_edit == 0 || Misc.gchar_pos(fpos) == '\n')) {
      G.curwin.setCaretPosition(fpos.getOffset() - 1);
    }
    
    G.State = NORMAL;
    Misc.ui_cursor_shape();
    //
    // When recording or for CTRL-O, need to display the new mode.
    // Otherwise remove the mode message.
    //
    // if (/*Recording ||*/ G.restart_edit != 0) {
    //   Misc.showmode();
    // } else if (true/*G.p_smd*/) {
    //   // MSG("");
    //   G.curwin.getStatusDisplay().displayMode(VI_MODE_COMMAND);
    // }
    // always use showmode, since it communicates mode to environment
    Misc.showmode();
    return true;
  }
  
  /**
   * Insert the contents of a named buf into the text.
   * <br>vim returns boolean whether to redraw or not.
   */
  private static void ins_reg() throws NotSupportedException {
    Normal.notImp("ins_reg");
  }
  
  /**
   * If the cursor is on an indent, ^T/^D insert/delete one
   * shiftwidth.	Otherwise ^T/^D behave like a "<<" or ">>".
   * Always round the indent to 'shiftwith', this is compatible
   * with vi.  But vi only supports ^T and ^D after an
   * autoindent, we support it everywhere.
   */
  private static void ins_shift(int c, int lastc, int dir) {
    stop_arrow();
    GetChar.AppendCharToRedobuff(c);
    
    // NEEDSWORK: ins_shift: lastc not supported
    lastc = 0;
    
    /* **************************************************************
    //
    // 0^D and ^^D: remove all indent.
    //
    if ((lastc == '0' || lastc == '^') && curwin->w_cursor.col)
    {
        --curwin->w_cursor.col;
        (void)del_char(FALSE);		// delete the '^' or '0'
        // In Replace mode, restore the characters that '^' or '0' replaced.
        if (State == REPLACE || State == VREPLACE)
            replace_pop_ins();
        if (lastc == '^')
            old_indent = get_indent();	// remember curr. indent
        change_indent(INDENT_SET, 0, TRUE, 0);
    }
    else
     ************************************************************/
    change_indent(dir == BACKWARD ? INDENT_DEC : INDENT_INC, 0, true, 0);
  }
  
  private static void ins_shift_paren(int c, int dir) {
    GetChar.AppendCharToRedobuff(c);
    int curindent = Misc.get_indent();
    int amount = Misc.findParen(G.curwin.getWCursor().getLine()-1,
            curindent, dir);
    if(amount > 0) {
      ++amount; // position after paren
      change_indent(INDENT_SET, amount, false, 0);
    } else {
      amount = Misc.findFirstNonBlank(G.curwin.getWCursor().getLine()-1,
              curindent, dir);
      if(dir == BACKWARD && amount > 0
              || dir != BACKWARD && amount > curindent)
        change_indent(INDENT_SET, amount, false, 0);
    }
    
  }
  
  private static void ins_del() throws NotSupportedException {
    Normal.notImp("ins_del");
  }
  
  
  /**
   * Handle Backspace, delete-word and delete-line in Insert mode.
   * @return TRUE when backspace was actually used.
   */
  private static boolean ins_bs(int c,
                                int mode,
                                MutableBoolean inserted_space_p)
  throws NotSupportedException {
    Normal.do_xop("ins_bs");
    
    ViFPOS cursor = G.curwin.getWCursor();
    
    // can't delete anything in an empty file
    // can't backup past first character in buffer
    // can't backup past starting point unless 'backspace' > 1
    // can backup to a previous line if 'backspace' == 0
    if(   Util.bufempty()  // also ifdef RIGHTLEFT then next only if !revins_on
       || cursor.getLine() == 1 && cursor.getColumn() <= 0
       || (!Options.can_bs(BS_START)
           && (   arrow_used
               || (   cursor.getLine() == Insstart.getLine()
                   && cursor.getColumn() <=Insstart.getColumn())))
       || (!Options.can_bs(BS_INDENT) && !arrow_used && G.ai_col > 0
                                  && cursor.getColumn() <= G.ai_col)
       || (!Options.can_bs(BS_EOL) && cursor.getColumn() == 0)
    ) {
      Util.vim_beep();
      return false;
    }
    
    
    stop_arrow();
    // ....
    
    //
    // delete newline!
    //
    if(cursor.getColumn() <= 0) {
      G.curwin.deletePreviousChar();
      // NEEDSWORK: backspace over newline. bunch of logic in this branch...
    } else {
      // NEEDSWORK: backspace complications
      
      Misc.dec_cursor();
      
      // NEEDSWORK: only handle backspace char
      
      if (G.State == REPLACE || G.State == VREPLACE) {
        replace_do_bs();
      } else  { /* State != REPLACE && State != VREPLACE */
        Misc.del_char(false);
      }
      // Just a single backspace?:
      // if (mode == BACKSPACE_CHAR) break;
    }
    
    // It's a little strange to put backspaces into the redo
    // buffer, but it makes auto-indent a lot easier to deal
    // with.
    GetChar.AppendCharToRedobuff(c);
    
    
    // If deleted before the insertion point, adjust it
    if(cursor.getLine() == Insstart.getLine()
    && cursor.getColumn() < Insstart.getColumn()) {
      Insstart = cursor.copy();
    }
    
    return true;
  }
  
  /**
   * Handle CR or NL in insert mode.
   * Return TRUE when out of memory.
   */
  static void ins_eol(int c) {
    // if (echeck_abbr(c + ABBR_OFF)) return FALSE;
    
    stop_arrow();
    
    // Strange Vi behaviour: In Replace mode, typing a NL will not delete the
    // character under the cursor.  Only push a NUL on the replace stack,
    // nothing to put back when the NL is deleted.
    
    if (G.State == REPLACE)
      replace_push(NUL);
    
    // In VREPLACE mode, a NL replaces the rest of the line, and starts
    // replacing the next line, so we push all of the characters left on the
    // line onto the replace stack.  This is not done here though, it is done
    // in open_line().
    
    GetChar.AppendToRedobuff(NL_STR);
    
    Misc.open_line(FORWARD, true, false, G.old_indent);
    G.old_indent = 0;
  }
  
  private static void ins_home() throws NotSupportedException {
    Normal.notImp("ins_home");
  }
  
  private static void ins_end() throws NotSupportedException {
    Normal.notImp("ins_end");
  }
  
  private static void ins_s_left() throws NotSupportedException {
    Normal.notImp("ins_s_left");
  }
  
  private static void ins_left() throws NotSupportedException {
    Normal.notImp("ins_left");
  }
  
  private static void ins_s_right() throws NotSupportedException {
    Normal.notImp("ins_s_right");
  }
  
  private static void ins_right() throws NotSupportedException {
    //Normal.notImp("ins_right");
    System.err.println("ins_right");
    undisplay_dollar();
    if (Misc.gchar_cursor() != '\n') {
      //start_arrow(&curwin->w_cursor);
      start_arrow(G.curwin.getWCursor());
              //#ifdef MULTI_BYTE............
      //curwin->w_set_curswant = TRUE;
      //++curwin->w_cursor.col;
      G.curwin.setWSetCurswant(true);
      G.curwin.setCaretPosition(G.curwin.getWCursor().getOffset() +1);
              //#ifdef RIGHTLEFT............
    }
    /* if 'whichwrap' set for cursor in insert mode, may move the
     * cursor to the next line */
    // NEEDSWORK: implement p_ww ']': ins_right: next line ok in insert mode
    /*else if (vim_strchr(p_ww, ']') != NULL
             && curwin->w_cursor.lnum < curbuf->b_ml.ml_line_count)
    {
      start_arrow(&curwin->w_cursor);
      curwin->w_set_curswant = TRUE;
      ++curwin->w_cursor.lnum;
      curwin->w_cursor.col = 0;
    }*/
    else
      Util.vim_beep();
  }
  
  private static void ins_up() throws NotSupportedException {
    Normal.notImp("ins_up");
  }
  
  private static void ins_down() throws NotSupportedException {
    Normal.notImp("ins_down");
  }
  
  private static void ins_pageup() throws NotSupportedException {
    Normal.notImp("ins_pageup");
  }
  
  private static void ins_pagedown() throws NotSupportedException {
    Normal.notImp("ins_pagedown");
  }
}
