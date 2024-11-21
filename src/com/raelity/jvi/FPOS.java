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
 * File position, accessable as line number and column (both 1 based).
 * <p>
 * Any changes or additions here must be considered for WCursor.
 * </p>
 * <br><b>NEEDSWORK:</b><ul>
 * <li>Put lnum and col together in a class, and have them set with single
 * method call. Can also defer initializing them.
 * <li>Currently accesses jbuilder editor for line/col. Make this an
 * interface?
 * </ul>
 */

public class FPOS implements ViFPOS, Comparable, Cloneable
{
  private MutableInt offset;
  private MutableInt lnum;
  private MutableInt col;

  /** An empty cursor position */
  public FPOS() {
    this(0, 0, 0);
  }

  /** Record the current cursor position in the related document. */
  public FPOS(ViTextView editor) {
    this(0, 0, 0);
    if(editor != null) {
      setCursor(editor);
    }
  }

  /** Used to complete constructin or to make a copy. */
  private FPOS(int o, int l, int c) {
    offset = new MutableInt(o);
    lnum = new MutableInt(l);
    col = new MutableInt(c);
  }

  public void setCursor(ViTextView editor) {
    editor.computeCursorPosition(offset, lnum, col);
    // offset = editor.getCaretPosition();
    // lnum = editor.getLineNumber(offset);
    // col = editor.getColumnNumber(offset);
  }

  public int getLine() {
    return lnum.getValue();
  }

  public int getColumn() {
    return col.getValue();
  }

  public int getOffset() {
    if(offset.getValue() < 0) {
      throw new RuntimeException("negative offset");
    }
    return offset.getValue();
  }

  public void setPosition(int line, int column) {
    offset.setValue(G.curwin.getLineStartOffset(line) + column);
    lnum.setValue(line);
    col.setValue(column);
  }

//  /**
//   * Set the line number of this. Attempts to access offset
//   * after this method will throw a runtime error.
//   */
//  public void setLine(int line) {
//    lnum.setValue(line);
//    offset.setValue(-1);
//  }
//
//  /**
//   * Set the column number of this. Attempts to access offset
//   * after this method will throw a runtime error.
//   */
//  public void setColumn(int col) {
//    this.col.setValue(col);
//    offset.setValue(-1);
//  }
//
//  /**
//   * Add argument to the line number of this. Attempts to access offset
//   * after this method will throw a runtime error.
//   *
//   */
//  public void adjustLine(int line) {
//    lnum.setValue(lnum.getValue() + line);
//    offset.setValue(-1);
//  }
//
//  /**
//   * Add argument to the column number of this. Attempts to access offset
//   * after this method will throw a runtime error.
//   */
//  public void adjustCol(int i) {
//    col.setValue(col.getValue() + i);
//    offset.setValue(-1);
//  }


  final public boolean equals(Object o) {
    // NEEDSWORK: equals FPOS, should doc be checked as same?
    if(o instanceof ViFPOS) {
      ViFPOS fpos = (ViFPOS)o;
      return this.getOffset() == fpos.getOffset();
    }
    return false;
  }

  final public int compareTo(Object o) {
    ViFPOS p = (ViFPOS)o;
    if(this.getOffset() < p.getOffset()) {
      return -1;
    } else if(this.getOffset() > p.getOffset()) {
      return 1;
    } else {
      return 0;
    }
  }

  final public ViFPOS copy() {
    FPOS fpos = new FPOS(offset.getValue(), lnum.getValue(), col.getValue());
    return fpos;
  }

  public String toString() {
    return	  "offset: " + offset.getValue()
	      	+ " lnum: " + lnum.getValue()
	      	+ " col: " + col.getValue()
		;
  }
}
