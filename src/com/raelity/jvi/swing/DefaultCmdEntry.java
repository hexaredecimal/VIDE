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

import javax.swing.JEditorPane;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.LinkedList;
import java.util.TooManyListenersException;
import com.raelity.jvi.ViCmdEntry;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.GetChar;
import com.raelity.jvi.*;
import javax.swing.text.JTextComponent;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

/**
 * This class provides shared access to a CommandLine entry widget.
 */
public class DefaultCmdEntry implements ViCmdEntry, ActionListener {
  CommandLine cmdLine;
  String lastCommand;
  ActionListener listener;
  List list = new LinkedList();
  RSyntaxTextArea editorPane;
  String initialText = "";

  public DefaultCmdEntry(CommandLine cmdLine) {
    this.cmdLine = cmdLine;
  }

  /** ViCmdEntry interface */
  public String getCommand() {
    return lastCommand;
  }
  /** ViCmdEntry interface */
  public void append(String s) {
    cmdLine.append(s);
  }

  /** ViCmdEntry interface */
  public void activate(String mode, ViTextView parent) {
    activate(mode, parent, "", false);
  }

  /** ViCmdEntry interface */
  public void activate(String mode, ViTextView parent,
                       String initialText, boolean passThru) {
    this.initialText = initialText;
    if(passThru) {
      lastCommand = initialText;
      fireEvent(new ActionEvent(parent.getEditorComponent(),
                                ActionEvent.ACTION_PERFORMED,
                                "\n"));
      return;
    }
    cmdLine.addActionListener(this);
    cmdLine.takeFocus(true);
    cmdLine.setMode(mode);
    cmdLine.setList(list);
    editorPane = parent.getEditorComponent();
    // listen to editor pane focus
    cmdLine.clear();
    cmdLine.append(initialText);
  }

  /**
   * This is invoked when the command line entry is stopped by the user.
   * pass the event on.
   * ActionListener interface.
   */
  public void actionPerformed(ActionEvent e) {
    lastCommand = cmdLine.getCommand();
    list = cmdLine.getList();
    shutdownEntry();
    fireEvent(e);
  }

  /** ViCmdEntry interface */
  public void cancel() {
    lastCommand = "";
    shutdownEntry();
  }

  /** Send the event, if it is a successful entry, a CR, then
   * record the input. Note that initial Text is not part of
   * the recorded input.
   */
  void fireEvent(ActionEvent e) {
    if(e.getActionCommand().charAt(0) == '\n') {
      StringBuffer sb = new StringBuffer();
      if( ! initialText.equals("")
          && lastCommand.startsWith(initialText)) {
        sb.append(lastCommand.substring(initialText.length()));
      } else {
        sb.append(lastCommand);
      }
      sb.append('\n');
      GetChar.userInput(new String(sb));
    }
    listener.actionPerformed(e);
  }

  void shutdownEntry() {
    cmdLine.removeActionListener(this);
    cmdLine.takeFocus(false);
    editorPane.requestFocus();
    editorPane = null;
  }

  /** ViCmdEntry interface */
  public void addActionListener(ActionListener l)
                          throws TooManyListenersException {
    if(listener != null) {
      throw new TooManyListenersException();
    }
    listener = l;
  }

  /** ViCmdEntry interface */
  public void removeActionListener(ActionListener l) {
    if(listener == l) {
      listener = null;
    }
  }

    public JTextComponent getTextComponent() {
        return cmdLine.getTextField();
    }
}
