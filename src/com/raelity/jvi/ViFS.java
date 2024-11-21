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

import java.io.File;
import javax.swing.text.Document;

import com.raelity.jvi.ViTextView;

/**
 * jVi's interactions with files are funnelled through this interface.
 */
public interface ViFS {

  /**
   * Write the specified "file object".
   */
  public void write(ViTextView tv, boolean force);

  /**
   * Write all open stuff.
   */
  public void writeAll(boolean force);

  /**
   * Write the specified text to the specified file.
   * If file is null, use a dialog to get the file name.
   */
  public void write(ViTextView tv, File file, boolean force);

  /**
   * Edit the nth file. If n &lt; 0 then n is MRU; n == -1 is most recent.
   */
  public void edit(ViTextView tv, int n, boolean force);
}
