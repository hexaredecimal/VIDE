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

class Cursor implements ViCursor {
  int shape;
  int percentage;
  int blinkwait;
  int blinkon;
  int blinkoff;
  int highlightId;

  Cursor(int shape, int percentage) {
    // this(shape, percentage, 700, 400, 250, 0);
    this(shape, percentage, 700, 500, 500, 0);
  }

  Cursor(int shape, int percentage,
	 int blinkwait, int blinkon, int blinkoff, int highlightId)
  {
    this.shape = shape;
    this.percentage = percentage;
    this.blinkwait = blinkwait;
    this.blinkon = blinkon;
    this.blinkoff = blinkoff;
    this.highlightId = highlightId;
  }

  /** one of the SHAPE_ defined */
  public int getShape() {
    return shape;
  }

  /** percentage of cell for bar */
  public int getPercentage() {
    return percentage;
  }

  /** blinking, wait time before blinking starts */
  public int getBlinkwait() {
    return blinkwait;
  }

  /** blinking, on time */
  public int getBlinkon() {
    return blinkon;
  }

  /** blinking, off time */
  public int getBlinkoff() {
    return blinkoff;
  }

  /** highlight group ID */
  public int getHighlightId() {
    return highlightId;
  }

  public boolean equals(Object o) {
    if( ! (o instanceof Cursor)) {
      return false;
    }
    Cursor c = (Cursor)o;
    return       shape == c.shape
	      && percentage == c.percentage
	      && blinkwait == c.blinkwait
	      && blinkon == c.blinkon
	      && blinkoff == c.blinkoff
	      && highlightId == c.highlightId;
  }
}
