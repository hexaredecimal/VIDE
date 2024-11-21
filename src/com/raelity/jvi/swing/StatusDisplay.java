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

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.raelity.jvi.ViStatusDisplay;
import com.raelity.jvi.ViCursor;
import com.raelity.jvi.G;

public class StatusDisplay implements ViStatusDisplay {
  public JLabel generalStatus;
  public JLabel strokeStatus;
  public JLabel modeStatus;

  public void displayMode(String mode) {
    String s = mode + (G.Recording ? "recording" : "");
    if(s.equals("")) {
      s = " ";
    }
    setText(modeStatus, s);
  }

  public void displayCommand(String cmd) {
    setText(strokeStatus, cmd);
  }

  public void displayStatusMessage(String msg) {
    setText(generalStatus, msg);
  }

  public void displayErrorMessage(String msg) {
    // NEEDSWORK: make error message red or something
    setText(generalStatus, msg);
  }
  
  public void clearMessage() {
    displayStatusMessage("");
  }

  synchronized void setText(JLabel l00, String s00) {
    if(l00 == generalStatus && s00.equals("")) {
      s00 = " "; // need this to keep the status bar from colapsing
    }
    if(SwingUtilities.isEventDispatchThread()) {
      l00.setText(s00);
    } else {
      final JLabel l01 = l00;
      final String s01 = s00;
      SwingUtilities.invokeLater(
	new Runnable() {
	  public void run() {
	    l01.setText(s01);
	  }});
    }
  }

    /** don't need anything special here */
    public void refresh() {
    }
}
