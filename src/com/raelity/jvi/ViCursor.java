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

/** Salient variables for doing a vi cursor. */

public interface ViCursor {
  static final int SHAPE_BLOCK	= Constants.SHAPE_BLOCK;// block cursor
  static final int SHAPE_HOR	= Constants.SHAPE_HOR;// horizontal bar cursor
  static final int SHAPE_VER	= Constants.SHAPE_VER;// vertical bar cursor
  /** one of the SHAPE_ defined */
  public int getShape();

  /** percentage of cell for bar */
  public int getPercentage();

  /** blinking, wait time before blinking starts */
  public int getBlinkwait();

  /** blinking, on time */
  public int getBlinkon();

  /** blinking, off time */
  public int getBlinkoff();

  /** highlight group ID */
  public int getHighlightId();
}
