/**
 * Title:        jVi<p>
 * Description: A VI-VIM clone. Use VIM as a model where applicable.<p>
 * Copyright: Copyright (c) Ernie Rael<p>
 * Company: Raelity Engineering<p>
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
package com.application;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import com.raelity.jvi.swing.*;
import com.raelity.jvi.*;
import com.raelity.jvi.cmd.JviFrame;

public class Jvi {

  static JviFrame frame1;

  //Construct the application
  public static JviFrame makeFrame() {
    JviFrame frame = new JviFrame();
    frame.setVisible(true);
    return frame;
  }

  //Main method
  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      e.printStackTrace();
    }
    ViManager.setViFactory(new DefaultViFactory(null/*frame.commandLine1*/));

    ColonCommands.register("dumpOptions", "dumpOptions", new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          ViManager.getViFactory().getPreferences().exportSubtree(System.out);
        } catch (BackingStoreException ex) {
          ex.printStackTrace();
        } catch (IOException ex) {
          ex.printStackTrace();
        }
      }
    });
    ColonCommands.register("deleteOptions", "deleteOptions", new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          String keys[] = ViManager.getViFactory().getPreferences().keys();
          for (String key : keys) {
            ViManager.getViFactory().getPreferences().remove(key);
          }
        } catch (BackingStoreException ex) {
          ex.printStackTrace();
        }
      }
    });

    try {
      SwingUtilities.invokeAndWait(new Runnable() {
        public void run() {
          makeFrame();
        }
      });
    } catch (Exception e) {
    }

    // wait for frame to exit, so JUnitTest won't kill it
  }
}
