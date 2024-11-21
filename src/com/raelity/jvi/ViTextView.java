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

import javax.swing.JEditorPane;
import javax.swing.text.Segment;
import javax.swing.text.Element;
import javax.swing.text.BadLocationException;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

/**
 * The information needed by vim when running on
 * a GUI. We abstract this, rather than using
 * native swing, since different environments, e.g. JBuilder,
 * have their own abstractions. Some abstraction that presents
 * a line oriented interface is useful for vim.
 * <p>This has had a few "window" methods added to it, and it is
 * now the primary class referenced by most of the vi code. This
 * allows it to be G.curwin.
 * </p>
 * <p>
 * NEEDSWORK: get rid of JEditorPane reference, should not refer to swing.
 * </p>
 */

public interface ViTextView extends ViOptionBag {
  // text fold operations
  public static final int FOLDOP_CLOSE = 'c';
  public static final int FOLDOP_OPEN = 'o';
  public static final int FOLDOP_CLOSE_ALL = 'M';
  public static final int FOLDOP_OPEN_ALL = 'R';
  
  /** jump list operations */
  public enum JLOP { NEXT_JUMP, PREV_JUMP, NEXT_CHANGE, PREV_CHANGE }
  
  /** annonymous mark operations */
  public enum MARKOP { TOGGLE, NEXT, PREV }
  
  /** tags and tagstack operations */
  public enum TAGOP { OLDER, NEWER }
  
  //
  // First the methods that make this look like a window
  // (maybe implement at some point)
  //
  /** @return the associated Window */
  public Window getWindow();
  public void setWindow(Window window);

  /** @return the current location of the cursor in the window */
  public FPOS getWCursor();

  public int getWCurswant();
  public void setWCurswant(int c);
  public boolean getWSetCurswant();
  public void setWSetCurswant(boolean f);

  public ViMark getPCMark();
  public ViMark getPrevPCMark();
  public void pushPCMark();
  public ViMark getMark(int i);

  public int getWPScroll();
  public void setWPScroll(int n);
  public boolean getWPList();
  public void setWPList(boolean f);

  //
  // and now the text view proper
  //

  /** @return the underlying text component */
  public RSyntaxTextArea getEditorComponent();

  /** @return opaque FileObject backing this EditorPane */
  public Object getFileObject();

  /** Insert new line at current position */
  public void insertNewLine();

  /** Insert tab at current position */
  public void insertTab();

  /** Replace character at current cursor location with argument character */
  public void replaceChar(int c, boolean advanceCursor);

  /** Replace indicated region with string */
  public void replaceString(int start, int end, String s);

  /** Delete a bunch of characters */
  public void deleteChar(int start, int end);

  /** Delete previous character (backspace). */
  public void deletePreviousChar();

  /** insert text at specified location */
  public void insertText(int offset, String s);

  /** insert text at current cursor location.
   *  For some characters special actions may be taken
   */
  public void insertChar(int c);

  /** insert the char verbatim, no special actions */
  public void insertTypedChar(char c);
  
  /** Can the editor text be changed */
  public boolean isEditable();

  /** undo a change */
  public void undo();

  /** redo a change */
  public void redo();

  /** get some text from the document */
  public String getText(int offset, int length) throws BadLocationException;

  /** @return the offset of the text insertion caret */
  public int getCaretPosition();

  /** Determine cursor position, all args get set (call by reference). */
  public void computeCursorPosition(MutableInt offset,
				    MutableInt line,
				    MutableInt column);

  /** set the caret to the indicated position. */
  public void setCaretPosition(int offset);

  /** set the caret to the indicated position. */
  public void setCaretPosition(int lnum, int col);

  /** select a region of the screen */
  public void setSelect(int dot, int mark);

  /** find matching character for character under the cursor.
   * This is the '%' command. It is here to take advantage of
   * existing functionality in target environments.
   */
  public void findMatch();
  
  /** Jump to the definition of the identifier under the cursor. */
  public void jumpDefinition(String ident);
  
  /** Jump list handling */
  public void jumpList(JLOP op, int count);
  
  /** Anonymous mark handling.
   * Count is the Nth mark forward, back. It is ignored by TOGGLE.
   */
  public void anonymousMark(MARKOP op, int count);
  
  /** Perform the fold operation.  */
  public void foldOperation(int op);

  /** @return the line number, 1 based, corresponding to the offset */
  public int getLineNumber(int offset);

  /** @return the column number, 1 based, corresponding to the offset */
  public int getColumnNumber(int offset);

  /** @return the starting offset of the line */
  public int getLineStartOffset(int line);

  /** @return the starting offset of the line */
  public int getLineEndOffset(int line);

  /** @return the starting offset of the line */
  public int getLineStartOffsetFromOffset(int offset);

  /** @return the end offset of the line, char past newline */
  public int getLineEndOffsetFromOffset(int offset);

  /** @return the number of lines in the associated file */
  public int getLineCount();

  /** @return the segment for the line */
  public Segment getLineSegment(int line);

  /** @return a segment for the requested text */
  public void getSegment(int offset, int length, Segment segment);

  /** @return the element for the line */
  public Element getLineElement(int line);

  /** @return the line number of first visible line in window */
  public int getViewTopLine();

  /** cause the idndicated line to be displayed as top line in view. */
  public void setViewTopLine(int line);

  /** @return the line number of line *after* end of window */
  public int getViewBottomLine();

  /** @return the number of unused lines on the display */
  public int getViewBlankLines();

  /** @return the number of lines in window */
  public int getViewLines();

  /** Scroll down (n_lines positive) or up (n_lines negative) the
   * specified number of lines.
   */
  public void scroll(int n_lines);

  /** establish all the listeners */
  public void attach();

  /** tear down all the listeners */
  public void detach();
  
  /** going away, do any remaining cleanup */
  public void shutdown();

  /** Change the cursor shape */
  public void updateCursor(ViCursor cursor);

  /** start an undo group, must be paired */
  public void beginUndo();

  /** end an undo group, must be paired */
  public void endUndo();

  /** between a begin and an end undo? */
  public boolean isInUndo();
  
  /** start a insert (user typing) ungo group, must be paired */
  public void beginInsertUndo();
  
  /** end a insert (user typing) ungo group, must be paired */
  public void endInsertUndo();
  
  /** between a insert begin and end undo? */
  public boolean isInInsertUndo();

  /** associate the indicated mark with a particular offset */
  public void setMarkOffset(ViMark mark, int offset, boolean global_mark);

  /** @return an array of marks */
  public ViMark[] createMarks(int n_mark);

  /** Quit editing window. Can close last view.
   */
  public void win_quit();

  /** Split this window.
   * @param n the size of the new window.
   */
  public void win_split(int n);

  /** Close this window. Does not close last view.
   * @param freeBuf true if the related buffer may be freed
   */
  public void win_close(boolean freeBuf);

  /** Close other windows
   * @param forceit true if always hide all other windows
   */
  public void win_close_others(boolean forceit);

  /** Goto the indicated buffer.
   * @param n the index of the window to make current
   */
  public void win_goto(int n);

  /** Cycle to the indicated buffer.
   * @param n the positive/negative number of windows to cycle.
   */
  public void win_cycle(int n);

  /** Handle displayable editor state changes */
  public ViStatusDisplay getStatusDisplay();

  /** Display file info */
  public void displayFileInfo();

  /** Display file info */
  public String getDisplayFileName();

  public String getDisplayFileNameAndSize();

  /**
   * Update the visual state (selection) of the window.
   */
  public void updateVisualState();
}
