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

import java.awt.event.ActionListener;
import java.util.Set;
import java.util.prefs.Preferences;
import javax.swing.JEditorPane;
import javax.swing.Action;
import com.raelity.jvi.ViTextView.TAGOP;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

/**
 * This provides Vi the items it needs to interface with
 * swing UI environment.
 */
public interface ViFactory {

  /** Return a TextView, create one if it doesn't already exist */
  public ViTextView getViTextView(RSyntaxTextArea editorPane);

  /** Return a Buffer, create one if it doesn't already exist */
  public Buffer getBuffer(RSyntaxTextArea editorPane);

  /** @return null if TextView does not exist */
  public ViTextView getExistingViTextView(Object editorPane);
  
  /** @return Set of active ViTextView, some may have retired */
  public Set<ViTextView> getViTextViewSet();
  
  /** @return Set of active Buffer, some may have retired */
  public Set<Buffer> getBufferSet();

  /** For an environmental object, used for debug output */
  public String getDisplayFilename(Object o);
  
  /**
   * This editor pane is going away, forget about it.
   */
  public void shutdown(RSyntaxTextArea editorPane);

  /*
   * Register editor pane for use with vi.
   * This is a nop if already registered.
   */
  public void registerEditorPane(RSyntaxTextArea editorPane);

  /**
   * File manipulation handled through this object.
   */
  public ViFS getFS();

  /** create an output stream for some kind of results.
   *  @param type Should be a constant from ViOutputStream,
   *          e.g. ViOutputStream.SEARCH.
   *  @param info qualifier for the output stream, e.g. search pattern.
   */
  public ViOutputStream createOutputStream(ViTextView tv,
                                           Object type, Object info);

  /**
   * The factory keeps track of which Window is active when there
   * is a switch. The argument may or may not be used depending on
   * the implementation.
   * @return the Window for the specified editor
   */
  public Window lookupWindow(RSyntaxTextArea editorPane);
  
  /**
   * Something has changed in jVi's key mapping, update editor stuff as needed.
   */
  public void updateKeymap();

  /**
   * @return action suitable for default key action
   */
  public Action createCharAction(String name);

  /**
   * @return action for picking up specified key
   */
  public Action createKeyAction(String name, int key);
  
  /**
   * fetch the keymap for insert mode operations
   */
  //public Keymap getInsertModeKeymap();
  
  /**
   * fetch the keymap for normal mode operations
   */
  //public Keymap getNormalModeKeymap();
  
  /**
   * @return edit mode action for specific operation
   */
  public Action createInsertModeKeyAction(String name, int vkey, String desc);
  
  /**
   * @return normal mode action for specific operation
   */
  public Action createNormalModeKeyAction(String name, int vkey, String desc);
  
  /**
   * The actions used for keymap translation may be munged by the environment
   * so we need a way to get back the original action.
   */
  public ActionListener xlateKeymapAction(ActionListener act);

  /**
   * A command entry object will be created if needed.
   * @return a CmdEntry object to handle the specified type of user input.
   */
  public ViCmdEntry createCmdEntry(int type);

  public Preferences getPreferences();
  
  //
  // Just stuff all the tag stuff here for now
  //

  public void startTagPush(ViTextView tv, String ident);

  public void finishTagPush(ViTextView tv);
  
  public void tagStack(TAGOP op, int count);
  
  public void displayTags();
  
  public void tagDialog(ColonCommands.ColonEvent e);
}
