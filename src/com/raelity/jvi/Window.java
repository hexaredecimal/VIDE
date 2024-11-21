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

import javax.swing.text.Segment;
import com.raelity.jvi.swing.*;

import static com.raelity.jvi.Constants.*;

/**
 * Vim references values in a structure, but we need to present a method
 * interface, so the vim code is not preserved as we'd like.
 *
 * <br><b>NEEDSWORK:</b><ul>
 * <li>Make an interface for this. Then can have the jbuilder version and
 * the standalone version.
 * </ul>
 */

public final class Window {
  private ViTextView editor;
  private FPOS w_cursor = new FPOS();

  // private ViMark[] marks;

  public Window(ViTextView editor) {
    this.editor = editor;
    // marks = editor.createMarks(26);	// only lowercase for now.
    w_pcmark = editor.createMarks(1)[0];
    w_prev_pcmark = editor.createMarks(1)[0];
    viewSizeChange();
  }

  /**
   * The current location of the cursor in this window.
   */
  public FPOS XXXgetWCursor() {
    // return editor.getCursor()
    // w_cursor.setCursor(editor);	// make sure cursor is current
    // return w_cursor;
    return getEditor().getWCursor();
  }

  /**
   * Set the current location of the cursor in this window.
   */
  public final void setWCursor(ViFPOS p) {
    getEditor().setCaretPosition(p.getOffset());
  }

  /**
   * The column we'd like to be at.
   */
  private int w_curswant;

  /**
   * Get the column we'd like to be at. This is used to try to stay in the
   * same column through up/down cursor motions.
   */

  public int getWCurswant() { return w_curswant; }
  /**
   * Set the column we'd like to be at.
   */
  public void setWCurswant(int c) { w_curswant = c; }

  private boolean w_set_curswant;

  /**
   * If set, then update w_curswant the next time through cursupdate()
   * to the current virtual column.
   */
  public boolean getWSetCurswant() { return w_set_curswant; }
  public void setWSetCurswant(boolean f) { w_set_curswant = f; }

  private boolean w_p_list;

  /**
   * list mode
   */
  public boolean getWPList() { return w_p_list; }
  public void setWPList(boolean f) { w_p_list = f; }

  private int w_p_scroll;
  // NEEDSWORK: this should be comming from the cache
  /**
   * scroll
   */
  public int getWPScroll() { return w_p_scroll; }
  public void setWPScroll(int n) { w_p_scroll = n; }

   //
   // Mark related stuff
   //
  private ViMark w_pcmark;
  private ViMark w_prev_pcmark;

  public final ViMark getPCMark() {
    return w_pcmark;
  }

  public final ViMark getPrevPCMark() {
    return w_prev_pcmark;
  }

  public void pushPCMark() {
    w_prev_pcmark.setData(w_pcmark);
  }

  public final ViMark getMark(int i) {
    return MarkOps.getMark(getEditor(), i);
  }

  public void previousContextHack(ViMark mark) {
    pushPCMark();
    w_pcmark.setData(mark);
  }

  /**
   * Like win_new_height....
   */
  public void viewSizeChange() {
    // from win_comp_scroll
    int i = (getEditor().getViewLines() >> 1);
    if(i <= 0) {
      i = 1;
    }
    setWPScroll(i);
  }

  /**
   * A mouse click has just occured in this window. Check the
   * position so it is not on a newline (unless in input mode)
   * <br>
   * NEEDSWORK: "signal" a change in cursor position
   */
  public int mouseClickedPosition(int offset) {
    setWSetCurswant(true);
    if (Util.getCharAt(offset) == '\n' && (G.State & INSERT) == 0) {
      // Sitting on a newline and not in insert mode
      // back the cursor up (unless previous char is a newline)
      if(offset > 0 && Util.getCharAt(offset - 1) != '\n') {
	--offset;
      }
    }
    return offset;
  }

  /**
   * @return the editor associated with this window
   */
  ViTextView getEditor() {
    return editor;
  }
}
