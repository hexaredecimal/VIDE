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

import java.awt.Toolkit;
import javax.swing.text.Segment;
import javax.swing.text.BadLocationException;

public class Util {
  // static final int TERMCAP2KEY(int a, int b) { return a + (b << 8); }
  static final int ctrl(int x) { return x & 0x1f; }
  // static final int shift(int c) { return c | (0x1 << 24); }
  // static void stuffcharReadbuff(int c) {}

  /** position to end of line. */
  static void endLine() {
    ViFPOS fpos = G.curwin.getWCursor();
    int offset = G.curwin
	      		.getLineEndOffsetFromOffset(fpos.getOffset());
    // assumes there is at least one char in line, could be a '\n'
    offset--;	// point at last char of line
    if(Util.getCharAt(offset) != '\n') {
      offset++; // unlikely
    }
    G.curwin.setCaretPosition(offset);
  }

  public static void vim_beep() {
    Toolkit.getDefaultToolkit().beep();
  }

  /** 
   * Returns the substring of c in s or null if c not part of s.
   * @param s the string to search in
   * @param c the character to search for
   * @return the substring of c in s or null if c not part of s.
   */
  public static String vim_strchr(String s, int c) {
    int index = s.indexOf(c);
    if(index < 0) {
      return null;
    }
    return s.substring(index);
  }

  public static final boolean isalnum(int regname) {
    return	regname >= '0' && regname <= '9'
    		|| regname >= 'a' && regname <= 'z'
    		|| regname >= 'A' && regname <= 'Z';
  }

  public static final boolean isalpha(int c) {
    return	   c >= 'a' && c <= 'z'
    		|| c >= 'A' && c <= 'Z';
  }

  public static boolean islower(int c) {
    return 'a' <= c && c <= 'z';
  }

 public static int tolower(int c) {
   if(isupper(c)) {
     c |= 0x20;
   }
   return c;
 }

  static boolean isupper(int c) {
    return 'A' <= c && c <= 'Z';
  }

 static int toupper(int c) {
   if(islower(c)) {
     c &= ~0x20;
   }
   return c;
 }

  public static boolean isdigit(int c) {
    return '0' <= c && c <= '9';
  }

  static boolean vim_isprintc(int c) { return false; }

  /**
   * get a pointer to a (read-only copy of a) line.
   *
   * On failure an error message is given and IObuff is returned (to avoid
   * having to check for error everywhere).
   */
  static Segment ml_get(int lnum) {
    // return ml_get_buf(curbuf, lnum, FALSE);
    return G.curwin.getLineSegment(lnum);
  }
  
  /** get pointer to positin 'pos', the returned Segment's CharacterIterator
   * is initialized to the character at pos.
   * @return Segment for the line.
   */
  static Segment ml_get_pos(ViFPOS pos) {
    //return (ml_get_buf(curbuf, pos->lnum, FALSE) + pos->col);
    Segment seg = G.curwin.getLineSegment(pos.getLine());
    seg.setIndex(pos.getOffset());
    return seg;
  }
  
  static Segment ml_get_curline() {
    //return ml_get_buf(curbuf, curwin->w_cursor.lnum, FALSE);
    return ml_get(G.curwin.getWCursor().getLine());
  }

  /**
   * Get the length of a line, not incuding the newline
   */
  static int lineLength(int line) {
    return G.curwin.getLineEndOffset(line)
                     - G.curwin.getLineStartOffset(line) - 1;
  }

  /** is the indicated line empty? */
  static boolean lineempty(int lnum) {
    Segment seg = G.curwin.getLineSegment(lnum);
    return seg.count == 0 || seg.array[seg.offset] == '\n';
  }
  
  static boolean bufempty() {
      return G.curwin.getLineCount() == 1
             && lineempty(1);
  }

  static int getChar() {
    return getCharAt(G.curwin.getCaretPosition());
  }

  static int getCharAt(int offset) {
    Segment seg = new Segment();
    G.curwin.getSegment(offset, 1, seg);
    return seg.count > 0 ? seg.array[seg.offset] : 0;
  }

  /** flush map and typeahead buffers and vige a warning for an error */
  static void beep_flush() {
    GetChar.flush_buffers(false);
    vim_beep();
  }
}
