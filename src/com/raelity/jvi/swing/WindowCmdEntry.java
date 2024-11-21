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

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Container;
import javax.swing.WindowConstants;
import java.util.List;
import java.util.LinkedList;
import java.util.TooManyListenersException;
import java.util.WeakHashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
//import javax.swing.JViewport;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.raelity.jvi.ViCmdEntry;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.GetChar;
import com.raelity.jvi.swing.CommandLineWindow;
import com.raelity.jvi.swing.CommandLine;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import com.raelity.jvi.*;
import javax.swing.text.JTextComponent;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

/**
 * This class provides a floating CommandLine entry widget.
 * A separate window must be allocated for each browser.
 * The window close button is disabled. <ESC> or <CR> must be
 * used to dismiss it.
 */
public class WindowCmdEntry
             implements ViCmdEntry, ActionListener, WindowListener
{
  /** result of last entry */
  String lastCommand;
  /** reference back to the user of this entry widget */
  ActionListener listener;
  /** the history list */
  protected List list = new LinkedList();

  /** valid while actively taking input */
  protected RSyntaxTextArea currentEditorPane;
  /** valid while active taking input */
  protected CommandLineWindow commandLineWindow;  // Should be protected
  /** maps frames to their command line window */
  protected Map windowMap = new WeakHashMap(5);
  // NEEDSWORK: WindowCmdEntry: on frame dispose, dispose of window
  private Dimension lastCalculatedSize;
  private CommandLineWindow lastUsedWindow;
  private String initialText = "";

  String title;

  public WindowCmdEntry(int type) {
    title = type == ViCmdEntry.SEARCH_ENTRY
                 ? "Search Pattern" : "Command";
  }

  /** ViCmdEntry interface */
  public String getCommand() {
    return lastCommand;
  }

  /** ViCmdEntry interface */
  public void append(String s) {
    commandLineWindow.commandLine.append(s);
  }

  /**
   * This is invoked to get the command window associated with
   * the specified text view. A new one should be created if needed.
   * Any newly created window is parented to the frame containing the
   * indicated editor.
   * This is called by {@link #placeCommandWindow}.
   * @return the CommandLineWindow
   */
  protected CommandLineWindow getCommandLineWindow(ViTextView tv) {
    CommandLineWindow commandLineWindow;

    Frame frame = (Frame)SwingUtilities.getRoot(tv.getEditorComponent());
    commandLineWindow = (CommandLineWindow)windowMap.get(frame);

    if(commandLineWindow == null) {
      commandLineWindow = new CommandLineWindow(frame, title);
      commandLineWindow.setDefaultCloseOperation(
                             WindowConstants.DO_NOTHING_ON_CLOSE);
      windowMap.put(frame, commandLineWindow);
    }
    return commandLineWindow;
  }

  /**
   * This method positions a command line for the indicated editor.
   * This invokes {@line #getCommandLineWindow}.
   * @return the CommandLineWindow that was placed.
   */
  protected CommandLineWindow placeCommandWindow(ViTextView tv) {
    CommandLineWindow commandLineWindow = getCommandLineWindow(tv);
    this.commandLineWindow = commandLineWindow;
    Container jc = SwingUtilities.getAncestorOfClass(
                javax.swing.JScrollPane.class, tv.getEditorComponent());
    if(jc == null) {
      jc = tv.getEditorComponent();
    }

    Dimension d00 = commandLineWindow.getPreferredSize();
    int x00 = jc.getWidth();
    Dimension d01 = new Dimension(Math.min(500,x00), d00.height);
    // Make the window a reasonable size
    if(lastCalculatedSize == null || ! lastCalculatedSize.equals(d01)) {
      lastCalculatedSize = d01;
      commandLineWindow.setSize(d01);
      // commandLineWindow.invalidate();
      commandLineWindow.validate();
    }
    Point p = new Point(0, jc.getHeight());
    SwingUtilities.convertPointToScreen(p, jc);
    commandLineWindow.setLocation(p.x, p.y);
    return commandLineWindow;
  }

  /** ViCmdEntry interface */
  public void activate(String mode, ViTextView parent) {
    activate(mode, parent, "", false);
  }

  /**
   * Start taking input. As a side effect of invoking this,
   * the variable {@line #commandLineWindow} is set.
   */
  public void activate(String mode, ViTextView tv,
                       String initialText, boolean passThru) {
    this.initialText = initialText;
    lastCommand = "";
    if(passThru) {
      lastCommand = initialText;
      fireEvent(new ActionEvent(tv.getEditorComponent(),
                                ActionEvent.ACTION_PERFORMED,
                                "\n"));
      return;
    }
    commandLineWindow = placeCommandWindow(tv);
    CommandLine commandLine = commandLineWindow.getCommandLine();
    if(lastUsedWindow != commandLineWindow) {
      commandLine.setList(list);
      lastUsedWindow = commandLineWindow;
    }
    commandLine.addActionListener(this);
    commandLine.setMode(mode);
    currentEditorPane = tv.getEditorComponent();
    commandLine.clear();
    commandLine.append(initialText);
    //commandLineWindow.addWindowListener(this);
    commandLineWindow.setVisible(true);
  }

  /**
   * This is invoked when the command line entry is stopped by the user.
   * pass the event on.
   * ActionListener interface.
   */
  public void actionPerformed(ActionEvent e) {
    CommandLine commandLine = commandLineWindow.getCommandLine();
    lastCommand = commandLine.getCommand();
    list = commandLine.getList();
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
    CommandLine commandLine = commandLineWindow.getCommandLine();
    commandLine.removeActionListener(this);
    // commandLine.takeFocus(false);
    commandLineWindow.setVisible(false);
    currentEditorPane.requestFocus();
    currentEditorPane = null;
    commandLineWindow = null;
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

  public void windowOpened(WindowEvent e) {
    //TODO: Implement this java.awt.event.WindowListener method
  }

  public void windowClosing(WindowEvent e) {
    //TODO: Implement this java.awt.event.WindowListener method
  }

  public void windowClosed(WindowEvent e) {
    //TODO: Implement this java.awt.event.WindowListener method
  }

  public void windowIconified(WindowEvent e) {
    //TODO: Implement this java.awt.event.WindowListener method
  }

  public void windowDeiconified(WindowEvent e) {
    //TODO: Implement this java.awt.event.WindowListener method
  }

  public void windowActivated(WindowEvent e) {
    CommandLine commandLine = commandLineWindow.getCommandLine();
    // commandLine.takeFocus(true);
  }

  public void windowDeactivated(WindowEvent e) {
    //TODO: Implement this java.awt.event.WindowListener method
  }

    public JTextComponent getTextComponent() {
        return commandLineWindow.getCommandLine().getTextField();
    }

}
