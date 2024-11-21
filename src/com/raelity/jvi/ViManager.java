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
import java.io.PrintStream;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.ArrayList;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import com.raelity.jvi.swing.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

/**
 * This class coordinates things.
 * <b>NEEDSWORK:</b><ul>
 * </ul>
 */
public class ViManager {
    
  public static final String PREFS_ROOT = "com/raelity/jvi";
  public static final String PREFS_KEYS = "KeyBindings";

  static private RSyntaxTextArea currentEditorPane;
  static private ViFactory factory;

  static private Keymap editModeKeymap;
  static private Keymap normalModeKeymap;

  // HACK: to workaround JDK bug dealing with focus and JWindows
  public static ViCmdEntry activeCommandEntry;

  private static final int majorVersion = 0;
  private static final int minorVersion = 9;
  private static final int microVersion = 0;
  private static final String releaseTag = "";
  private static final String release = "jVi "
                    + ViManager.majorVersion
		    + "." + ViManager.minorVersion
		    + "." + ViManager.microVersion
                    + ViManager.releaseTag;
  
  private static boolean enabled;

  public static void setViFactory(ViFactory factory) {
    if(ViManager.factory != null) {
      throw new RuntimeException("ViFactory already set");
    }
    
    enabled = true;
    ViManager.factory = factory;

    Options.init();
    KeyBinding.init();
  }

  public static String getReleaseString() {
    return release;
  }

  public static int getMajorVersion() {
    return majorVersion;
  }

  public static int getMinorVersion() {
    return minorVersion;
  }

  public static int getMicroVersion() {
    return microVersion;
  }

  public static String getReleaseTag() {
    return releaseTag;
  }

  public static ViFactory getViFactory() {
    return factory;
  }

  public static ViFS getFS() {
    return factory.getFS();
  }

  public static RSyntaxTextArea getCurrentEditorPaneXXX() {
    return currentEditorPane;
  }

  public static ViTextView getViTextView(RSyntaxTextArea editorPane) {
    return factory.getViTextView(editorPane);
  }

  public static Buffer getBuffer(RSyntaxTextArea editorPane) {
    return factory.getBuffer(editorPane);
  }
  
  public static ViOutputStream createOutputStream(ViTextView tv,
                                           Object type, Object info) {
    return factory.createOutputStream(tv, type, info);
  }

  static public void installKeymap(RSyntaxTextArea editorPane) {
    editorPane.setKeymap(KeyBinding.getKeymap());
  }

  /**
   * Pass control to indicated ViCmdEntry widget. If there are
   * readahead or typeahead characters available, then collect
   * them up to a &lt;CR&gt; and append them to initialString.
   * If there was a CR, then signal the widget to immeadiately
   * fire its actionPerformed without displaying any UI element.
   */
  public static void startCommandEntry(ViCmdEntry commandEntry,
                                       String mode,
                                       ViTextView tv,
                                       StringBuffer initialString)
  {
    Msg.clearMsg();
    if(initialString == null) {
      initialString = new StringBuffer();
    }
    if(activeCommandEntry != null) {
      throw new RuntimeException("activeCommandEntry not null");
    }

    activeCommandEntry = commandEntry;
    boolean passThru = GetChar.getRecordedLine(initialString);
    commandEntry.activate(mode, tv, new String(initialString), passThru);
  }

  public static void stopCommandEntry() {
    activeCommandEntry = null;
  }
  
  //
  // jVi maintains two lists of opened files: the order they opened, and a
  // MostRecentlyUsed list.
  //
  // Even if jVi is disabled, these methods can be used. They only maintain
  // the lists.
  //

  // NEEDSWORK: textMRU: use a weak reference to fileObject?
  private static List textBuffers = new ArrayList();
  private static LinkedList textMRU = new LinkedList();
  private static Object currentlyActive;
  private static Object ignoreActivation;

  /**
   * Fetch the text buffer indicated by the argument. If the argument is
   * positive, then fetch the Nth buffer, numbered 1 to N, according to
   * the order they were activated. If the argument is negative then use
   * the MRU list to get the buffer, where -1
   * means the previous buffer. An argument of 0 will return null.
   * Usage for n < 0 is deprecated, consider -0 is not the top of the
   * MRU list, see {@link #getMruBuffer}.
   */
  public static Object getTextBuffer(int i) {
    if(i == 0) {
      return null;
    }
    if(i < 0)
      return getMruBuffer(-i);
      
    i = i - 1;
    if(i >= textBuffers.size()) {
      return null;
    }
    return textBuffers.get(i);
  }
  
  /**
   * Fetch the Nth buffer, 0 to N-1, from the Mru list.
   * @return the buffer, else null if i is out of bounds.
   */
  public static Object getMruBuffer(int i) {
    if(i < 0 || i >= textMRU.size())
        return null;
    return textMRU.get(i);
  }
  
  /**
   * Return the Ith next/previous fileObject relative to the argument 
   * fileObject. If i < 0 then look in previously used direction.
   */
  public static Object relativeMruBuffer(Object fileObject, int i) {
      if(factory != null && G.dbgEditorActivation.getBoolean()) {
        System.err.println("Activation: ViManager.relativeMruBuffer: "
                + factory.getDisplayFilename(fileObject));
      }
      if(textMRU.size() == 0)
          return null;
      int idx = textMRU.indexOf(fileObject);
      if(idx < 0)
          return null;
      // the most recent is at index 0, so bigger numbers are backwwards in time
      idx += -i;
      if(idx < 0)
          idx = 0;
      else if(idx >= textMRU.size())
          idx = textMRU.size() -1;
      return textMRU.get(idx);
  }
  
  public static Object relativeMruBuffer(int i) {
      return relativeMruBuffer(currentlyActive, i);
  }
  
  /**
   * Request that the next activation does not re-order the mru list if the
   * activated object is the argment.
   */
  public static void ignoreActivation(Object fileObject) {
      if(!textBuffers.contains(fileObject)) {
          return; // can't ignore if its not in the list
      }
      ignoreActivation = fileObject;
  }

  /**
   * The application invokes this whenever a file becomes selected
   * in the specified container. This also serves as an open.
   * @param ep May be null, otherwise the associated editor pane
   * @param parent Usually, but not necessarily, a container that hold the
   *               editor.
   */
  public static void activateFile(RSyntaxTextArea ep, Object fileObject, String tag) {
    if(factory != null && G.dbgEditorActivation.getBoolean()) {
      System.err.println("Activation: ViManager.activateFile: "
              + tag + ": " + factory.getDisplayFilename(fileObject));
    }
    if(ep != null && enabled)
        registerEditorPane(ep);
    assert(fileObject != null);
    if(fileObject == null)
        return;
    
    Object ign = ignoreActivation;
    ignoreActivation = null;
    currentlyActive = fileObject;
    if(textBuffers.contains(ign) && fileObject == ign) {
        return;
    }
    
    textMRU.remove(fileObject);
    textMRU.add(0, fileObject);
    if( ! textBuffers.contains(fileObject)) {
      textBuffers.add(fileObject);
    }
  }
  
  public static void deactivateCurrentFile(Object parent) {
    if(factory != null && G.dbgEditorActivation.getBoolean()) {
      System.err.println("Activation: ViManager.deactivateCurentFile: "
                         + factory.getDisplayFilename(parent));
    }
    // For several reasons, eg. don't want to hold begin/endUndo
    if(enabled)
        exitInputMode();
    
    currentlyActive = null;
    // assert(parent == currentlyActive || parent == null || currentlyActive == null);
  }
  
  public static boolean isBuffer(Object fileObject) {
      return textBuffers.contains(fileObject);
  }

  /**
   * The applications invokes this method when a file is completely
   * removed from a container or should be forgotten by jVi.
   */
  public static void closeFile(RSyntaxTextArea ep, Object fileObject) {
    if(factory != null && G.dbgEditorActivation.getBoolean()) {
      String fname = factory.getDisplayFilename(fileObject);
      System.err.println("Activation: ViManager.closeFile: "
              + (ep == null ? "(no shutdown) " : "") + fname);
    }
    
    assert(factory != null);
    if(factory != null && ep != null && enabled) {
        factory.shutdown(ep);
    }
    if(fileObject == currentlyActive)
        currentlyActive = null;
    textMRU.remove(fileObject);
    textBuffers.remove(fileObject);
  }
  
  //
  // END of OpenEditors list handling
  //

  /**
   * Set up an editor pane for use with vi.
   */
  public static void registerEditorPane(RSyntaxTextArea editorPane) {
    factory.registerEditorPane(editorPane);
  }
  
  public static void log(Object... a) {
      StringBuilder s = new StringBuilder();
      for (int i = 0; i < a.length; i++) {
          s.append(a[i]);
      }
      System.err.println(s);
  }

  /**
   * A key was typed. Handle the event.
   * <br>NEEDSWORK: catch all exceptions comming out of here?
   */
  static public void keyStroke(RSyntaxTextArea target, int key, int modifier) {
    switchTo(target);
    if(rerouteChar(key, modifier)) {
      return;
    }
    factory.finishTagPush(G.curwin);
    GetChar.gotc(key, modifier);
    
    if(G.curwin != null)
        G.curwin.getStatusDisplay().refresh();
  }

  /** If chars came in between the time a dialog was initiated and
   * the time the dialog starts taking the characters, we feed the
   * chars to the dialog.
   * <p>Special characters are discarded.
   * </p>
   */
  static boolean rerouteChar(int c, int modifiers) {
    if(activeCommandEntry == null) {
      return false;
    }
    if((c & 0xF000) != KeyDefs.VIRT
              && modifiers == 0) {
      if(c >= 0x20 && c < 0x7f) {
        String content = new String(new char[] {(char)c});
        activeCommandEntry.append(content);
      }
    }
    // System.err.println("rerouteChar " + (char)c);
    return true;
  }

  private static boolean started = false;
  static final void switchTo(RSyntaxTextArea editorPane) {
    if(editorPane == currentEditorPane) {
        return;
    }
    if( ! started) {
      started = true;
      startup();
    }
    
    registerEditorPane(editorPane); // make sure its registered
    exitInputMode(); // if switching, make sure prev out of input mode
    
    ViTextView textView = getViTextView(editorPane);
    textView.attach();
    if(G.dbgEditorActivation.getBoolean()) {
      System.err.println("Activation: ViManager.SWITCHTO: "
              + textView.getDisplayFileName());
    }
    
    if(currentEditorPane != null) {
      Normal.resetCommand(); // NEEDSWORK: dont think this is needed
      Normal.abortVisualMode();
      ViTextView currentTv = getViTextView(currentEditorPane);
      // Freeze and/or detach listeners from previous active view
      currentTv.detach();
    }

    currentEditorPane = editorPane;
    Buffer buf = getBuffer(editorPane);
    G.switchTo(textView, buf);
    textView.activateOptions(textView);
    buf.activateOptions(textView);
  }

  private static boolean inStartup;
  /** invoked once when vi is first used */
  private static void startup() {
    setupStartupList();
    inStartup = true;
    Iterator iter = startupList.iterator();
    while(iter.hasNext()) {
      ((ActionListener)iter.next()).actionPerformed(null);
    }
    Misc.javaKeyMap = KeyBinding.initJavaKeyMap();
    inStartup = false;
    startupList = null;
  }

  static List startupList;
  static void setupStartupList() {
    if(startupList == null) {
      startupList = new ArrayList();
    }
  }

  /**
   * Add listener to invoke when editor is starting up.
   * A null argument can be used to test if startup has
   * already occured.
   * @return true if listener add, otherwise false indicates
   * that startup has already occured.
   */
  public static boolean addStartupListener(ActionListener l) {
    if(started) {
      return false;
    }
    if(l != null) {
      setupStartupList();
      startupList.add(l);
    }
    return true;
  }

  public static void removeStartupListener(ActionListener l) {
    if(inStartup) {
      return;
    }
    startupList.remove(l);
  }

  /**
   * The arg RSyntaxTextArea is detached from its text view,
   * forget about it.
   */
  public static void detached(RSyntaxTextArea ep) {
    if(currentEditorPane == ep) {
      if(G.dbgEditorActivation.getBoolean()) {
        System.err.println("Activation: ViManager.detached");
      }
      currentEditorPane = null;
    }
  }

  public static void exitInputMode() {
    if(currentEditorPane != null) {
      Normal.resetCommand();
    }
  }

  /**
   * A mouse click; switch to the activated editor.
   * Pass the click on to the window and give it
   * a chance to adjust the position and whatever.
   */
  public static int mouseSetDot(int pos, JTextComponent c) {
    if( ! (c instanceof RSyntaxTextArea)) {
      return pos;
    }

    RSyntaxTextArea editorPane = (RSyntaxTextArea)c;

    // NEEDSWORK: mouse click: if( ! isRegistered(editorPane)) {}

    GetChar.flush_buffers(true);
    exitInputMode();
    switchTo(editorPane);
    
    //System.err.println("mouseSetDot(" + pos + ")");
    Window window = factory.lookupWindow(editorPane);
    pos = window.mouseClickedPosition(pos);
    if (G.VIsual_active) {
        // Cancel visual mode
        G.VIsual_active = false;
        G.curwin.updateVisualState();
        Misc.showmode();
    }
    return pos;
  }
  
  public static int mouseMoveDot(int pos, JTextComponent c) {
    if(c != G.curwin.getEditorComponent()) {
      return pos;
    }
    //System.err.println("mouseMoveDot(" + pos + ")");
    G.VIsual_mode ='v';
    G.VIsual_active = true;
    G.VIsual = (FPOS) G.curwin.getWCursor().copy();
    Misc.showmode();
    return pos;
  }

  /** A mouse click may have moved the caret. */
  public static void unexpectedCaretChange(int dot) {
    // XXX verify mouse is at an acceptable location
  }

  /** The viewport has changed, so number of screen lines have changed */
  public static void viewSizeChange(ViTextView textView) {
    try {
      // Window window = factory.lookupWindow(textView.getEditorComponent());
      // window.viewSizeChange();
    }
    catch (NonExistentWindowException ex) {
    }
  }

  /** The viewport has changed or scrolled, clear messages*/
  public static void viewMoveChange(ViTextView textView) {
    if(G.curwin == null) {
      // this case is because switchto, does attach, does viewport init
      // but G.curwin is not set yet. See switchTo(RSyntaxTextArea editorPane)
      return;
    }
    Msg.clearMsg();
  }

  /** set the previous context to the indicated offset */
  /*
  public static void previousContextHack(ViTextView textView, ViMark mark) {
    Window window = factory.lookupWindow(textView.getEditorComponent());
    window.previousContextHack(mark);
  }
  */

  /**
   * Listen to carets events for newly registered RSyntaxTextAreas.
   * If an event comes in, assign the editorPane to a TextView
   * and give it the oportunity to re-adjust the cursor.
   * <br><b>NEEDSWORK:</b><ul>
   * <li> work this out with textview, where this should be......
   * <ul>
   */
  static void fixupCaret() {
  }

  static public void dumpStack(String msg) {
    try {
      throw new IllegalStateException(msg);
    } catch(IllegalStateException ex) {
      ex.printStackTrace();
    }
  }

  static public void dumpStack() {
    try {
      throw new IllegalStateException();
    } catch(IllegalStateException ex) {
      ex.printStackTrace();
    }
  }

  static public void setInsertModeKeymap(Keymap newInsertModeKeymap) {
    editModeKeymap = newInsertModeKeymap;
  }

  static public Keymap getInsertModeKeymap() {
    return editModeKeymap;
  }

  static public void setNormalModeKeymap(Keymap newNormalModeKeymap) {
    normalModeKeymap = newNormalModeKeymap;
  }

  static public Keymap getNormalModeKeymap() {
    return normalModeKeymap;
  }

  static public ActionListener xlateKeymapAction(ActionListener act) {
    return factory.xlateKeymapAction(act);
  }
  
  static public void dump(PrintStream ps) {
    ps.println("-----------------------------------");
    ps.println("currentEditorPane = " + currentEditorPane );
    ps.println("factory = " + factory );
    
    ps.println("" + textBuffers.size() + " active");
    ps.println("textBuffers = " + textBuffers );
    ps.println("textMRU = " + textMRU );
    ps.println("currentlyActive = " + currentlyActive );
    ps.println("ignoreActivation = " + ignoreActivation );
    
    int n1 = 0;
    for (ViTextView tv : factory.getViTextViewSet()) {
        n1++;
    }
    if(n1 != 0)
        ps.println("" + n1 + " ACTIVE TEXT VIEWS");
  }
}
