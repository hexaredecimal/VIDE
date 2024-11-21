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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import java.util.*;
//import com.raelity.jvi.*;

/**
 * This class presents an editable textfield UI for picking up command entry
 * data. A mode, usually a single character like "?" or ":", can be
 * set for display. This UI supports the maintenance of a history of commands.
 * This component builds a keymap through which the action events are
 * delivered. When an event is delivered, the history list is updated.
 * The command line has a label that can be set with {@link #setMode}.
 */
public class NewCommandLine extends JPanel
{
  JLabel modeLabel = new JLabel();
  JTextField text = new JTextField();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  private String mode;

  /** This is used to append characters to the the textfield. It is
   * needed so that characters entered before the textfield gets focus
   * are not lost.
   */
  public void append(String s) {
    int offset = getTextField().getDocument().getLength();
    try {
      getTextField().getDocument().insertString(offset, s, null);
    }
    catch (BadLocationException ex) { }
  }

  public NewCommandLine() {
    try {
      jbInit();
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
    Font font = modeLabel.getFont();
    modeLabel.setFont(new Font("Monospaced",
				      font.BOLD,
				      font.getSize()));
    font = text.getFont();
    text.setFont(new Font("Monospaced",
				      font.getStyle(),
				      font.getSize()));
    setMode(" ");
    setKeymap();
  }

  void jbInit() throws Exception {
    modeLabel.setText("");
    this.setLayout(gridBagLayout1);
    text.setEditable(true);
    this.add(modeLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    this.add(text, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
  }

  /** This is used when the textfield is going to be displayed.
   * A blank line is put at the head of the list. This blank is
   * automatically removed when the user completes the action.
   */
  public void clear() {
    getTextField().setText("");
  }

  /*
  public void takeFocus(boolean flag) {
  }
  */

  /** Retrieve the contents of the command line. This is typically
   * done in response to a action event.
   */
  public String getCommand() {
    return getTextField().getText();
  }

  /** This sets the mode of the command line, e.g. ":" or "?". */
  public void setMode(String newMode) {
    mode = newMode;
    modeLabel.setText(" " + mode + " ");
  }

  public String getMode() {
    return mode;
  }

  javax.swing.JTextField getTextField() {
    return text;
  }

  /**
   * Make the argument command the top of the list.
   * If is already in the list then first remove it.
   */
  void makeTop(String command) {
  }

  /** Bounce the event, modified, to this class's user. */
  private class SimpleEvent extends TextAction {
    SimpleEvent(String name) {
      super(name);
    }
    public void actionPerformed(ActionEvent e) {
      ActionEvent e01 = new ActionEvent(NewCommandLine.this, e.getID(),
                              e.getActionCommand(), e.getModifiers());
      fireActionPerformed(e01);
    }
  };

  protected Action createSimpleEvent(String name) {
    return new SimpleEvent(name);
  }

  /**
   * Return and <ESC> fire events and update the history list.
   */
  private void setKeymap() {
    Keymap keymap = JTextComponent.addKeymap("CommandLine",
                                            getTextField().getKeymap());
    JTextComponent.loadKeymap(keymap, getBindings(), getActions());
    getTextField().setKeymap(keymap);
  }

  protected JTextComponent.KeyBinding[] getBindings() {
    JTextComponent.KeyBinding[] bindings = {
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_ENTER, 0),
		       "enter"),
	new JTextComponent.KeyBinding(KeyStroke.getKeyStroke(
		       KeyEvent.VK_ESCAPE, 0),
		       "escape")
    };
    return bindings;
  }

  protected Action[] getActions() {
    Action[] localActions = null;
    try {
      //ViFactory factory = ViManager.getViFactory();
      localActions = new Action[] {
	  createSimpleEvent("enter"),
	  createSimpleEvent("escape")
      };
    } catch(Throwable e) {
      e.printStackTrace();
    }
    return localActions;
  }

  /**
   * Take the argument event and create an action event copy with
   * this as its source. Then deliver it as needed.
   */
  protected void fireActionPerformed(ActionEvent e) {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();

    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length-2; i>=0; i-=2) {
      if (listeners[i]==ActionListener.class) {
        ((ActionListener)listeners[i+1]).actionPerformed(e);
      }
    }
  }

  /**
   * Adds the specified action listener to receive
   * action events from this textfield.
   *
   * @param l the action listener
   */
  public synchronized void addActionListener(ActionListener l) {
    listenerList.add(ActionListener.class, l);
  }

  /**
   * Removes the specified action listener so that it no longer
   * receives action events from this textfield.
   *
   * @param l the action listener
   */
  public synchronized void removeActionListener(ActionListener l) {
    listenerList.remove(ActionListener.class, l);
  }
}
