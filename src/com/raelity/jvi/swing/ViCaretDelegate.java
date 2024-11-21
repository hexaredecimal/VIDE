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
package com.raelity.jvi.swing;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.FontMetrics;
import java.awt.event.MouseEvent;
import java.awt.Point;
import javax.swing.text.Position;
import javax.swing.text.JTextComponent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.plaf.TextUI;

import com.raelity.jvi.ViCursor;
import com.raelity.jvi.*;

/**
 * This handles the VI behavior of a caret, drawing the caret is
 * the big deal.
 * // NEEDSWORK: cache the current font metric, listen to font property changes
 */
public class ViCaretDelegate
{
  ViCaret caret;        // the actual caret
  ViCursor cursor;
  /** width of a block cursor */
  int blockWidth = 8;

  public ViCaretDelegate(ViCaret caret) {
    this.caret = caret;
  }

  public void setCursor(ViCursor cursor) {
    if(cursor == null || cursor.equals(this.cursor)) {
      return;
    }
    this.cursor = cursor;
    // caret only has one value, compromise on blink rate
    caret.setBlinkRate((cursor.getBlinkon() + cursor.getBlinkoff()) / 2);
    caret.setVisible(true);
  }

  public ViCursor getCursor() {
    return this.cursor;
  }

  public boolean damage(Rectangle cleanupArea, Rectangle cursorArea) {
    if(cursorArea != null) {
      cleanupArea.x = cursorArea.x > 0 ? cursorArea.x -1 : cursorArea.x;
      cleanupArea.y = cursorArea.y;
      cleanupArea.width = blockWidth +2;
      cleanupArea.height = cursorArea.height;
      return true;
    }
    return false;
  }

  /**
   * Render the caret as specified by the cursor.
   */
  public void paint(Graphics g, JTextComponent component) {
    if(caret.isVisible()) {
      int dot = caret.getDot();
      FontMetrics fm = g.getFontMetrics();	// NEEDSWORK: should be cached
      blockWidth = fm.charWidth('.');		// assume all the same width
      try {
	Rectangle r = component.getUI().modelToView(
                component, dot);
	g.setColor(component.getCaretColor());
	g.setXORMode(component.getBackground());

	int h02;
	int cursorShape = ViCursor.SHAPE_BLOCK;
	if(cursor != null) { cursorShape = cursor.getShape(); }
	switch(cursorShape) {
	case ViCursor.SHAPE_BLOCK:
	  r.width = blockWidth;
	  break;
	case ViCursor.SHAPE_HOR:
	  r.width = blockWidth;
	  h02 = percent(r.height, cursor.getPercentage());
	  r.y += r.height - h02;
	  r.height = h02;
	  break;
	case ViCursor.SHAPE_VER:
	  /* back up vertical cursors by one pixel */
            /* NEEDSWORK: following was in 0.7.x
	  if(r.x > 0) {
	    r.x -= 1;
	  }*/
	  r.width = percent(blockWidth + 1, cursor.getPercentage());
	  break;
	}
	g.fillRect(r.x, r.y, r.width, r.height);
	g.setPaintMode();

      } catch (BadLocationException e) {
	//System.err.println("Can't render cursor");
      }
    }
  }

  private int percent(int v, int percentage) {
    // NEEDSWORK: percent use int only
    v = (int) (((v * (float)percentage) / 100) + .5);
    return v;
  }

  /**
   * Tries to set the position of the caret from
   * the coordinates of a mouse event, using viewToModel().
   * Notifies vi that the most has been clicked in window
   * and give vi a chance to adjust the position.
   * <br>
   * Note: this implementation can not call setDot(pos, biasRet[0]),
   * so other stuff can probably be eliminated.
   *
   * @param e the mouse event
   */
  public void positionCaret(MouseEvent e) {
    Point pt = new Point(e.getX(), e.getY());
    Position.Bias[] biasRet = new Position.Bias[1];
    int pos = caret.getTextComponent().getUI()
                       .viewToModel(caret.getTextComponent(), pt, biasRet);
    if(biasRet[0] == null)
      biasRet[0] = Position.Bias.Forward;
    pos = ViManager.mouseSetDot(pos, caret.getTextComponent());
    if (pos >= 0) {
      // setDot(pos, biasRet[0]);
      caret.setDot(pos);

      // clear the prefferred caret position
      // see: JCaret's UpAction/DownAction
      caret.setMagicCaretPosition(null);
    }
  }
}

