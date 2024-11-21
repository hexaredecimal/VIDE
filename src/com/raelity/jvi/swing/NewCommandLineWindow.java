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

import javax.swing.JFrame;
import javax.swing.JWindow;
import javax.swing.JDialog;
import java.awt.*;

/**
 * A JWindow that holds a CommandLine.
 * Wanted to use a window, but see bugparade: Bug Id 4199374
 */
public class NewCommandLineWindow extends JDialog {
  public NewCommandLine commandLine = new NewCommandLine();

  public NewCommandLineWindow(Frame frame, String title) {
    super(frame, title, true);
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }
  }

  private void jbInit() throws Exception {
    this.getContentPane().add(commandLine, BorderLayout.NORTH);
    this.pack();
  }

  public NewCommandLine getCommandLine() {
    return commandLine;
  }
}
