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

import java.util.Map;
import java.util.WeakHashMap;

import static com.raelity.jvi.ViTextView.MARKOP;
import static com.raelity.jvi.ViTextView.MARKOP.NEXT;
import static com.raelity.jvi.ViTextView.MARKOP.PREV;
import static com.raelity.jvi.ViTextView.MARKOP.TOGGLE;

/**
 * Keep track of vi marks.
 */
class MarkOps implements Constants, Messages {

  static Map marks = new WeakHashMap(5);

  /** This constant indicates mark is in other file. */
  final static FPOS otherFile = new FPOS();

  /** Set the indicated mark to the current cursor position;
   * if anonymous mark characters, then handle that.
   * <b>NEEDSWORK:</b><ul>
   * <li>Only handle lower case marks.
   * </ul>
   */
  static int setmark(int c, int count) {
    Normal.do_xop("setmark");
    
    {
        // Handle anonymous mark operations here
      
        MARKOP op = null;
        switch(c) {
            case '.': op = TOGGLE; break;
            case '<': op = PREV; break; // Nothing to do with visual mode
            case '>': op = NEXT; break; // Nothing to do with visual mode
        }
        if(op != null) {
            if(op != TOGGLE)
                setpcmark();
            G.curwin.anonymousMark(op, count);
            
            return OK;
        }
    }

    if (c == '\'' || c == '`') {
      setpcmark();
      /* keep it even when the cursor doesn't move */
      G.curwin.pushPCMark();
      return OK;
    }

    if (Util.islower(c)) {
      int		i;
      i = c - 'a';
      G.curwin.setMarkOffset(getMark(G.curwin, i),
                             G.curwin.getCaretPosition(), false);
      // curbuf.b_namedm[i] = curwin.w_cursor;
      return OK;
    }

    if (false && Util.isupper(c)) {	// NEEDSWORK: upper case marks
      // i = c - 'A';
      // namedfm[i].mark = curwin.w_cursor;
      // namedfm[i].fnum = curbuf.b_fnum;
      // return OK;
    }
    return FAIL;
  }

  /*
   * getmark(c) - find mark for char 'c'
   *
   * Return pointer to FPOS if found (caller needs to check lnum!)
   *	  NULL if there is no mark called 'c'.
   *	  -1 if mark is in other file (only if changefile is TRUE)
   */
  static ViMark getmark(int c, boolean changefile) {
    ViMark m = null;
    if(c == '\'' || c == '`') {
      // make a copy since it might change soon
      m = (ViMark) G.curwin.getPCMark().copy();
    } else if(c == '<' || c == '>') {
      ViMark startp = G.curbuf.b_visual_start;
      ViMark endp = G.curbuf.b_visual_end;
      // If either mark's no good, return it and let error handling report it
      if(check_mark(startp, false) == FAIL)
        return startp;
      if(check_mark(endp, false) == FAIL)
        return endp;
      boolean isLtName = c == '<' ? true : false;
      boolean isLtValue = startp.compareTo(endp) < 0 ? true : false;
      m = isLtName == isLtValue ? startp : endp;
      /*
       * For Visual line mode, set mark at begin or end of line
       */
      if(G.curbuf.b_visual_mode == 'V') {
        // the questions is whether column 0 or last column
        int col = 0;
        if(c == '>')
          col = Misc.check_cursor_col(m.getLine(), MAXCOL);
        int lineoff = G.curwin.getLineStartOffsetFromOffset(m.getOffset());
        m = (ViMark)m.copy();
        G.curwin.setMarkOffset(m, lineoff + col, false);
      }
    } else if (Util.islower(c)) {
      int i = c - 'a';
      m = getMark(G.curwin, i);
    }
    return m;
  }

  /** Get the indicated mark (a reference). */
  static ViMark getMark(ViTextView tv, int i) {
    ViMark[] ms = getMarks(tv);
    return ms[i];
  }

  /** Get the indicated mark (a reference). */
  static ViMark XXXgetMark(int i) {
    ViMark[] ms = getMarks(G.curwin);
    return ms[i];
  }

  static private ViMark[] getMarks(ViTextView tv) {
    ViMark[] ms = (ViMark[])marks.get(tv.getFileObject());
    if(ms == null) {
      ms = tv.createMarks(26);
      marks.put(tv.getFileObject(), ms);
    }
    return ms;
  }

  /**
   * Check a if a position from a mark is valid.
   * Give and error message and return FAIL if not.
   */
  static int check_mark(ViMark mark) {
    return check_mark(mark, true);
  }

  static int check_mark(ViMark mark, boolean messageOK) {
    String msg = null;
    if(mark == null) {
      msg = e_umark;
    } else {
      try {
	if(mark.getLine() > G.curwin.getLineCount()) {
	  msg = e_markinval;
	}
      } catch(MarkOrphanException e) {
	msg = e_marknotset;
      } catch(MarkException e) {
	msg = e_marknotset;
      }
    }
    if(msg != null) {
      if(messageOK) {
	Msg.emsg(msg);
      }
      return FAIL;
    }
    return OK;
  }


  /**
   * checkpcmark() - To change context, call setpcmark(), then move the current
   *		   position to where ever, then call checkpcmark().  This
   *		   ensures that the previous context will only be changed if
   *		   the cursor moved to a different line. -- webb.
   *		   If pcmark was deleted (with "dG") the previous mark is
   *		   restored.
   */
  static void checkpcmark() {
    if(check_mark(G.curwin.getPrevPCMark(), false) == OK
       		&& (G.curwin.getPCMark().equals(G.curwin.getWCursor())
		    || check_mark(G.curwin.getPCMark(), false) == FAIL))
    {
      G.curwin.getPCMark().setData(G.curwin.getPrevPCMark());
      G.curwin.getPrevPCMark().invalidate();
    }
  }

  static void setpcmark(int offset) {
    if(G.global_busy) {
      return;
    }
    G.curwin.pushPCMark();
    G.curwin.setMarkOffset(G.curwin.getPCMark(), offset, false);
    // NEEDSWORK: pcmark and jump list stuff...
  }

  static void setpcmark() {
    setpcmark(G.curwin.getCaretPosition());
  }
}
