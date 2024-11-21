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
import com.raelity.jvi.*;
import javax.swing.border.*;

/**
 * This class presents a editable combo box UI for picking up command entry
 * data. A mode, usually a single character like "?" or ":", can be
 * set for display. This UI supports the maintenance of a history of commands.
 * This component builds a keymap through which the action events are
 * delivered. When an event is delivered, the history list is updated.
 * The command line has a label that can be set with {@link #setMode}.
 * <p>Take steps to prevent this component from taking focus
 * throught the focus manager. But ultimately need to subclass the
 * editor component to handle it.
 */
public class CommandLine extends JPanel
{
  static public final int DEFAULT_HISTORY_SIZE = 50;
  JLabel modeLabel = new JLabel();
  JComboBox combo = new JComboBox();
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  private String mode;
  private java.util.List list;
  private int historySize;
  Border border1;

  /** This is used to initialize the text of the combo box,
   * needed so that characters entered before the combo box gets focus
   * are not lost.
   */
  public void init(String s) {
    if(s.length() == 0) {
      JTextComponent tc = getTextField();
      int len = tc.getText().length();
      if(len > 0) {
        tc.setCaretPosition(0);
        tc.moveCaretPosition(len);
        //System.err.println("Selection length = " + len);
      }
      return;
    }
    try {
      Document doc = getTextField().getDocument();
      doc.remove(0,doc.getLength());
      doc.insertString(0, s, null);
    }
    catch (BadLocationException ex) { }
  }

  /** This is used to append characters to the the combo box. It is
   * needed so that characters entered before the combo box gets focus
   * are not lost.
   */
  public void append(String s) {
    int offset = getTextField().getDocument().getLength();
    try {
      getTextField().getDocument().insertString(offset, s, null);
    }
    catch (BadLocationException ex) { }
  }

  public CommandLine() {
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
    font = combo.getFont();
    combo.setFont(new Font("Monospaced",
				      font.getStyle(),
				      font.getSize()));
    setHistorySize(DEFAULT_HISTORY_SIZE);
    setMode(" ");
    setKeymap();

    //getTextField().setNextFocusableComponent(null);
    //takeFocus(false);
  }

  void jbInit() throws Exception {
    modeLabel.setText("");
    this.setLayout(gridBagLayout1);
    combo.setEditable(true);
    this.add(modeLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    this.add(combo, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
  }
  
  public void setupBorder() {
    if(border1 == null) {
      /*
      border1 = BorderFactory.createCompoundBorder(
		  BorderFactory.createCompoundBorder(
		    BorderFactory.createLineBorder(Color.gray, 5),
		    BorderFactory.createRaisedBevelBorder()),
		  BorderFactory.createCompoundBorder(
		    BorderFactory.createLineBorder(Color.gray, 5),
		    BorderFactory.createLoweredBevelBorder()));
      border1 = BorderFactory.createBevelBorder(BevelBorder.RAISED,
						Color.white,Color.white,
						new Color(134, 134, 134),
						new Color(93, 93, 93));
      border1 = BorderFactory.createCompoundBorder(
		    BorderFactory.createRaisedBevelBorder(),
		  BorderFactory.createCompoundBorder(
		    BorderFactory.createLineBorder(Color.black, 2),
		    BorderFactory.createLoweredBevelBorder()));
      */
      border1 = BorderFactory.createLineBorder(Color.black, 2);
    }
    this.setBorder(border1);
  }

  /** This is used when the combo box is going to be displayed.
   * A blank line is put at the head of the list. This blank is
   * automatically removed when the user completes the action.
   */
  public void clear() {
    if(historySize == 0 || list == null) {
      getTextField().setText("");
      return;
    }
    list.add(0, "");
    combo.insertItemAt("", 0);
    combo.setSelectedIndex(0);
    getTextField().setText(""); // ??? prevent re-execute last command on <CR>
  }

  public void takeFocus(boolean flag) {
    if(flag) {
      combo.setEnabled(true);
      // NEEDSWORK: FOCUS: use requestFocusInWindow()
      getTextField().requestFocus();
    } else {
      combo.setEnabled(false);
    }
  }

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
    Component c = combo.getEditor().getEditorComponent();
    return (JTextField)c;
  }

  /** This installs the history list. This allows multiple history
   * lists to share the same component.
   */
  public void setList(java.util.List newList) {
    list = newList;
    combo.removeAllItems();
    if(list == null) {
      return;
    }
    Iterator iter = newList.iterator();
    while(iter.hasNext()) {
      combo.addItem(iter.next());
    }
  }

  /** retrieve the history list. */
  public java.util.List getList() {
    return list;
  }

  /**
   * Make the argument command the top of the list.
   * If is already in the list then first remove it.
   */
  void makeTop(String command) {
    if(historySize == 0 || list == null) {
      return;
    }
    // remove the empty-blank element
    int i = list.indexOf("");
    if(i >= 0) {  // it really must be in the list
      list.remove(i);
      combo.removeItemAt(i);
    }
    // if the empty-blank string was selected we're done
    if(command.equals("")) {
      return;
    }
    // now move the selected string to the top of the list
    i = list.indexOf(command);
    if(i == 0) {
      return;  // already on the top
    }
    if(i > 0) {  // if its already in the list, remove it.
      list.remove(i);
      combo.removeItemAt(i);
    }
    list.add(0, command);
    combo.insertItemAt(command, 0);
    combo.setSelectedIndex(0);
    trimList();
  }

  /** Use this to limit the size of the history list */
  public void setHistorySize(int newHistorySize) {
    if(newHistorySize < 0) {
      throw new IllegalArgumentException();
    }
    historySize = newHistorySize;
  }

  /** the max size of the history list */
  public int getHistorySize() {
    return historySize;
  }

  private void trimList() {
    if(list == null) {
      return;
    }
    while(list.size() > historySize) {
      combo.removeItemAt(list.size() - 1);
      list.remove(list.size() - 1);
    }
  }

  /** Bounce the event, modified, to this class's user. */
  private class SimpleEvent extends TextAction {
    SimpleEvent(String name) {
      super(name);
    }
    public void actionPerformed(ActionEvent e) {
      ActionEvent e01 = new ActionEvent(CommandLine.this, e.getID(),
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
      ViFactory factory = ViManager.getViFactory();
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
   * Do some maintenance on the LRU history.
   */
  protected void fireActionPerformed(ActionEvent e) {
    String command = getTextField().getText();
    
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();

    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length-2; i>=0; i-=2) {
      if (listeners[i]==ActionListener.class) {
        ((ActionListener)listeners[i+1]).actionPerformed(e);
      }
    }
    
    // Maintain the LRU history, do this after the notifying completion
    // to avoid document events relating to the following actions
    makeTop(command);
    combo.hidePopup();
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
