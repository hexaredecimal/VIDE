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

import java.awt.Point;
import java.util.Arrays;

import javax.swing.text.*;
import javax.swing.JEditorPane;

import com.raelity.jvi.FPOS;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.ViMark;
import com.raelity.jvi.ViStatusDisplay;
import com.raelity.jvi.ViCursor;
import com.raelity.jvi.Util;
import com.raelity.jvi.MutableInt;
import com.raelity.jvi.Window;
import com.raelity.jvi.*;
import java.awt.Color;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

/**
 * Presents a swing editor interface for use with vi. There is
 * one of these for each JEditorPane;
 * <p>Notice the listeners for caret changes. If the caret changes
 * to a location that is unexpected, i.e. it came from some external
 * source, then an externalChange message is sent to vi.
 * </p><p>
 * The getEditorComponent method should not be used by the primary
 * vi software. The primary vi software should only access, or make
 * changes to, the underlying JEditorPane through other methods in
 * this class.
 * </p>
 */
public class TextView implements ViTextView {

  private static MutableAttributeSet HIGHLIGHT = new SimpleAttributeSet();
  private static MutableAttributeSet UNHIGHLIGHT = new SimpleAttributeSet();
  static {
      StyleConstants.setBackground(HIGHLIGHT, Color.LIGHT_GRAY);
      StyleConstants.setBackground(UNHIGHLIGHT, Color.WHITE);
  }

  protected RSyntaxTextArea editorPane;
  protected TextOps ops;
  protected static char[] oneCharArray = new char[1];
  protected TextViewCache cache;
  protected Window window;

  protected ViStatusDisplay statusDisplay;

  protected int expectedCaretPosition = -1;

  public TextView(final RSyntaxTextArea editorPane) {
    this.editorPane = editorPane;
    cache = createTextViewCache();
    statusDisplay = createStatusDisplay();
  }
  
  //
  // Declare the variables referenced as part of a ViOptionBag
  //

  /** jVi doesn't support this flag. Keep it as a per window flag like vim.
   * The platform may do something with it.
   */
  public boolean w_p_nu;
  
  public void viOptionSet(ViTextView tv, String name) {
  }

  public void activateOptions(ViTextView tv) {
  }
  
  //
  //
  //

  public void setWindow(Window window) {
    this.window = window;
  }

  public Window getWindow() {
    return window;
  }

  //
  // Pretend a little to be a window
  //

  /** @return the current location of the cursor in the window,
      note that this cursor is dynamic as caret moves this gets
      updated.
   */
  public FPOS getWCursor() { return cache.getCursor(); }
  //public FPOS getWCursor() { return window.getWCursor(); }

  public int getWCurswant() { return window.getWCurswant(); }
  public void setWCurswant(int c) { window.setWCurswant(c); }
  public boolean getWSetCurswant() { return window.getWSetCurswant(); }
  public void setWSetCurswant(boolean f) { window.setWSetCurswant(f); }

  public ViMark getPCMark() { return window.getPCMark(); }
  public ViMark getPrevPCMark() { return window.getPrevPCMark(); }
  public void pushPCMark() { window.pushPCMark(); }
  public ViMark getMark(int i) { return window.getMark(i); }

  public int getWPScroll() { return window.getWPScroll(); }
  public void setWPScroll(int n) { window.setWPScroll(n); }
  public boolean getWPList() { return window.getWPList(); }
  public void setWPList(boolean f) { window.setWPList(f); }

  /**
   * Override this class to provide a different implementations
   * of status display.
   */
  protected ViStatusDisplay createStatusDisplay() {
    return new StatusDisplay();
  }

  /** Override this method to provide different cache implementation */
  protected TextViewCache createTextViewCache() {
    return new TextViewCache(this);
  }

  public void attach() {
    if(ops == null) {
      createOps(editorPane);
    }
    if(G.dbgEditorActivation.getBoolean()) {
      System.err.println("TV.attach: " + editorPane.hashCode());
    }
    expectedCaretPosition = -1;
    cache.attach(editorPane);
  }

  public void detach() {
    cache.detach(editorPane);
    
    ViManager.detached(editorPane); // NEEDSWORK: what's this for?
  }
  
  public void shutdown() {
    cache.shutdown(editorPane);
    ViManager.detached(editorPane);
    editorPane = null;
  }
  
  /**
   * Create methods to invoke and interact with editor pane actions.
   * May override for custom editor panes.
   */
  protected void createOps(RSyntaxTextArea editorPane) {
    ops = new Ops(this);
    ops.init(editorPane);
  }

  public RSyntaxTextArea getEditorComponent() {
    return editorPane;
  }

  /**
   * Use the document in default implementation.
   * @return opaque FileObject backing this EditorPane */
  public Object getFileObject() {
    return getEditorComponent().getDocument();
  }
  
  /**
   * @return true if the text can be changed.
   */
  public boolean isEditable() {
    return editorPane.isEditable();
  }

  ////////////////////////////////////////////////////////////////////////
  //
  // Text modification methods.
  //
  
  protected void processTextException(BadLocationException ex) {
    Util.vim_beep();
  }
  
  public void insertNewLine() {
    if( ! isEditable()) {
      Util.vim_beep();
      return;
    }
    ops.xop(TextOps.INSERT_NEW_LINE); // NEEDSWORK: xop throws no exception
  }

  public void insertTab() {
    if( ! isEditable()) {
      Util.vim_beep();
      return;
    }
    expectedCaretPosition++;
    ops.xop(TextOps.INSERT_TAB); // NEEDSWORK: xop throws no exception
  }

  public void replaceChar(int c, boolean advanceCursor) {
    if( ! isEditable()) {
      Util.vim_beep();
      return;
    }
    oneCharArray[0] = (char)c;
    String s = new String(oneCharArray);
    int offset = editorPane.getCaretPosition();

    Document doc = getDoc();
    try {
      doc.remove(offset, 1);
      expectedCaretPosition++;
      doc.insertString(offset, s, null);
      if(advanceCursor) {
	offset++;
      }
      setCaretPosition(offset);// also clears the selection
    } catch(BadLocationException ex) {
      processTextException(ex);
    }
  }

  public void replaceString(int start, int end, String s) {
    if( ! isEditable()) {
      Util.vim_beep();
      return;
    }
    Document doc = getDoc();
    try {
      if(start != end) {
	doc.remove(start, end - start);
      }
      doc.insertString(start, s, null);
    } catch(BadLocationException ex) {
      processTextException(ex);
    }
  }

  public void deleteChar(int start, int end) {
    if( ! isEditable()) {
      Util.vim_beep();
      return;
    }
    Document doc = getDoc();
    try {
      doc.remove(start, end - start);
    } catch(BadLocationException ex) {
      processTextException(ex);
    }
  }

  public void deletePreviousChar() {
    if( ! isEditable()) {
      Util.vim_beep();
      return;
    }
    ops.xop(TextOps.DELETE_PREVIOUS_CHAR); // NEEDSWORK: xop throws no exception
  }

  public void insertText(int offset, String s) {
    if( ! isEditable()) {
      Util.vim_beep();
      return;
    }
    /* ******************************************
    setCaretPosition(offset);
    ops.xop(TextOps.INSERT_TEXT, s);
    ******************************************/
    expectedCaretPosition += s.length();
    Document doc = getDoc();
    if(offset > doc.getLength()) {
      // Damn, trying to insert after the final magic newline.
      // 	(the one that gets counted in elem.getEndOffset() but
      // 	not in getLength() )
      // Take the new line from the end of the string and put it
      // at the beging, e.g. change "foo\n" to "\nfoo". Then set
      // offset to length. The adjusted string is inserted before
      // the magic newline, this gives the correct result.
      // If there is no newline at the end of the string being inserted,
      // then there will end up being a newline added to the file magically,
      // but this shouldn't really matter.
      StringBuffer new_s = new StringBuffer();
      new_s.append('\n');
      if(s.endsWith("\n")) {
	if(s.length() > 1) {
	  new_s.append(s.substring(0,s.length()-1));
	}
      } else {
	new_s.append(s);
      }
      offset = doc.getLength();
      s = new_s.toString();
    }
    try {
      doc.insertString(offset, s, null);
    } catch(BadLocationException ex) {
      processTextException(ex);
    }
  }

  /**
   * insert character at cursor position. For some characters
   * special actions may be taken.
   */
  public void insertChar(int c) {
    if( ! isEditable()) {
      Util.vim_beep();
      return;
    }
    if(c == '\t') {
      insertTab();
      return;
    }
    insertTypedChar((char)c);
  }

  /**
   * Add a character verbatim to the window.
   */
  public void insertTypedChar(char c) {
    if( ! isEditable()) {
      Util.vim_beep();
      return;
    }
    oneCharArray[0] = (char)c;
    expectedCaretPosition++;
    ops.xop(TextOps.KEY_TYPED, new String(oneCharArray)); // NEEDSWORK: xop throws no exception
  }
  
  
///////////////////////////////////////////////////////////////////////
//
  
  public void undo(){
    assertUndoState(!inUndo && !inInsertUndo, "undo");
  }

  public void redo() {
    assertUndoState(!inUndo && !inInsertUndo, "undo");
  }

  public String getText(int offset, int length) throws BadLocationException {
    return cache.getDoc().getText(offset, length);
  }

  public int getCaretPosition() {
    return editorPane.getCaretPosition();
  }

  public void setCaretPosition(int offset) {
    expectedCaretPosition = offset;
    editorPane.setCaretPosition(offset);
    if (G.VIsual_active) {
        updateVisualState();
  }
  }

  public void setCaretPosition(int lnum, int col) {
    Element elem = cache.getLineElement(lnum);
    setCaretPosition(elem.getStartOffset() + col);
  }

  public void setSelect(int dot, int mark) {
    Caret c = editorPane.getCaret();
    c.setDot(mark);
    c.moveDot(dot);
  }

  public void findMatch() {
    Util.vim_beep();
  }

  public void jumpDefinition(String ident) {
    Util.vim_beep();
  }

  public void jumpList(JLOP op, int count) {
    Util.vim_beep();
  }
  
  public void anonymousMark(MARKOP op, int count) {
      Util.vim_beep();
  }

  public void foldOperation(int op) {
    Util.vim_beep();
  }

  public void computeCursorPosition(MutableInt offset,
			       MutableInt line,
			       MutableInt column)
  {
    int o = getCaretPosition();
    computeCursorPosition(o, line, column);
    offset.setValue(o);
  }

  public void computeCursorPosition(int offset,
			       MutableInt line,
			       MutableInt column)
  {
    // NEEDSWORK: computeCursorPosition use the cache

    Document doc = getDoc();
    Element root = doc.getDefaultRootElement();
    int idx =  root.getElementIndex(offset);
    Element elem =  root.getElement(idx);

    line.setValue(idx + 1);
    column.setValue(offset - elem.getStartOffset());
  }

  public int getLineNumber(int offset) {
    return getElemIndex(offset) + 1;
  }

  public int getColumnNumber(int offset) {
    Element elem = getElem(offset);
    return offset - elem.getStartOffset();
  }

  /** @return the starting offset of the line */
  public int getLineStartOffset(int line) {
    return getLineElement(line).getStartOffset();
  }

  /** @return the starting offset of the line */
  public int getLineEndOffset(int line) {
    return getLineElement(line).getEndOffset();
  }

  public int getLineStartOffsetFromOffset(int offset) {
    Element elem = getElem(offset);
    return elem.getStartOffset();
  }

  public int getLineEndOffsetFromOffset(int offset) {
    Element elem = getElem(offset);
    return elem.getEndOffset();
  }

  public int getLineCount() {
    return getDoc().getDefaultRootElement().getElementCount();
  }

  public int getViewTopLine() {
    return cache.getViewTopLine();
  }

  public int getViewBottomLine() {
    return cache.getViewBottomLine();
  }

  public void setViewTopLine(int line) {
    cache.setViewTopLine(line);
  }

  public int getViewBlankLines() {
    return cache.getViewBlankLines();
  }

  public int getViewLines() {
    return cache.getViewLines();
  }

  /** Scroll down (n_lines positive) or up (n_lines negative) the
   * specified number of lines.
   */
  public void scroll(int n_lines) {
    Point pt = cache.getViewport().getViewPosition();
    pt.translate(0, n_lines * cache.getFheight());
    cache.getViewport().setViewPosition(pt);
  }

  final public Segment getLineSegment(int lnum) {
    return cache.getLineSegment(lnum);
  }

  final public Element getLineElement(int lnum) {
    return cache.getLineElement(lnum);
  }
  
  final public void getSegment(int offset, int length, Segment seg) {
    try {
      getDoc().getText(offset, length, seg);
    } catch (BadLocationException ex) {
      seg.count = 0;
      // NEEDSWORK: how to report exception?
      ex.printStackTrace();
    }
  }

  /**
   * Set a mark at the specified offset.
   * @param global_mark if false then it is a mark within a file, otherwise
   * it is a file mark and is valid between files.
   */
  public void setMarkOffset(ViMark mark_arg, int offset, boolean global_mark) {
    Mark mark = (Mark) mark_arg;
    mark.setOffset(offset, this);
  }

  public ViMark[] createMarks(int n_mark) {
    ViMark[] mark = new ViMark[n_mark];
    for(int i = 0; i < n_mark; i++) {
      mark[i] = new Mark();
    }
    return mark;
  }

  public void updateCursor(ViCursor cursor) {
    if(editorPane == null) {
      return; // NEEDSWORK: was getting some null pointer stuff here
    }
    Caret caret = editorPane.getCaret();
    if(caret instanceof ViCaret) {
      ((ViCaret)caret).setCursor(cursor);
    }
  }

  private static boolean inUndo;

  private static boolean inInsertUndo;
  private void assertUndoState(boolean condition, String fn) {
      if(!(condition)) {
	  ViManager.dumpStack(fn + ": inUndo " + inUndo
		  + ", inInsertUndo " + inInsertUndo);
      }
  }
  
  public void beginUndo() {
    assertUndoState(!inUndo && !inInsertUndo, "beginUndo");
    inUndo = true;
  }

  public void endUndo() {
    assertUndoState(inUndo && !inInsertUndo, "endUndo");
    inUndo = false;
  }

  public boolean isInUndo() {
    return inUndo;
  }

  public void beginInsertUndo() {
    assertUndoState(!inUndo && !inInsertUndo, "beginInsertUndo");
    inInsertUndo = true;
  }

  public void endInsertUndo() {
    assertUndoState(inInsertUndo && !inUndo, "endInsertUndo");
    inInsertUndo = false;
  }

  public boolean isInInsertUndo() {
    return inInsertUndo;
  }

  /** Quit editing window. Can close last view.
   */
  public void win_quit() {
    Msg.emsg("win_quit not implemented");
  }

  /** Split this window.
   * @param n the size of the new window.
   */
  public void win_split(int n) {
    Msg.emsg("win_split not implemented");
  }

  /** Close this window
   * @param freeBuf true if the related buffer may be freed
   */
  public void win_close(boolean freeBuf) {
    Msg.emsg("win_close not implemented");
  }

  /** Close other windows
   * @param forceit true if always hide all other windows
   */
  public void win_close_others(boolean forceit) {
    Msg.emsg("win_close_others not implemented");
  }

  /** Goto the indicated buffer.
   * @param n the index of the window to make current
   */
  public void win_goto(int n) {
    Msg.emsg("win_goto not implemented");
  }

  /** Cycle to the indicated buffer.
   * @param n the positive/negative number of windows to cycle.
   */
  public void win_cycle(int n) {
    Msg.emsg("win_cycle not implemented");
  }

  public ViStatusDisplay getStatusDisplay() {
    return statusDisplay;
  }

  public void displayFileInfo() {
    StringBuffer sb = new StringBuffer();
    sb.append("\"" + getDisplayFileName() + "\"");
    int l = getLineCount();
    //sb.append(" " + l + " line" + Misc.plural(l));
    sb.append(" line " + cache.getCursor().getLine());
    sb.append(" of " + getLineCount());
    sb.append(" --" + (int)((cache.getCursor().getLine() * 100)
			      / getLineCount()) + "%--");
    sb.append(" col " + cache.getCursor().getColumn());
    getStatusDisplay().displayStatusMessage(sb.toString());
  }

  public String getDisplayFileNameAndSize() {
    StringBuffer sb = new StringBuffer();
    sb.append("\"" + getDisplayFileName() + "\"");
    int l = getLineCount();
    sb.append(" " + getLineCount() + "L, ");
    sb.append(" " + getDoc().getLength() + "C");
    return sb.toString();
  }

  public String getDisplayFileName() {
    return "xxx";
  }

  public TextOps getOp() {
    return ops;
  }

  protected final Document getDoc() {
    // return editorPane.getDocument();
    return cache.getDoc();
  }

  /** @return the element index from root which contains the offset */
  protected int getElemIndex(int offset) {
    Document doc = getDoc();
    Element root = doc.getDefaultRootElement();
    return root.getElementIndex(offset);
  }

  /** @return the element which contains the offset */
  protected Element getElem(int offset) {
    Element elem = cache.getCurrentLineElement();
    if(elem != null
       	&& elem.getStartOffset() <= offset && offset < elem.getEndOffset()) {
      return elem;
    }
    Element root = getDoc().getDefaultRootElement();
    return root.getElement(root.getElementIndex(offset));
  }
 
  /**
   * Update the selection highlight.
   */
  protected int[] previousHighlight = null;

  public void updateVisualState() {
      if (!G.VIsual_active) {
          try {
            unhighlight(new int[]{getMark('<').getOffset(), getMark('>').getOffset()});
          } catch(Exception e) {}
      }
      highlight(getVisualSelectBlocks(0, Integer.MAX_VALUE));
  }

  /**
   * Returns an array of blocks that are visual or an empty array when visual active is false.
   * the array is a an even set of points which are set like:<br>
   * {offset1start, offset1end, offset2start, offset2end, ..}
   * @return an array of blocks that are visual.
   * @throws an IllegalStateException when the visual mode is not understand
   */
  private int calcMode, calcStart, calcEnd;
  public int[] getVisualSelectBlocks(int startOffset, int endOffset) {
    if (G.drawSavedVisualBounds) {
        return previousHighlight;
    }
    // some caching so that visual blocks aren't recalculated everytime
    if (G.VIsual_active && calcMode == G.VIsual_mode && calcStart == getCaretPosition() && calcEnd == G.VIsual.getOffset()) {
        return previousHighlight;
    }
    calcMode = G.VIsual_mode;
    calcStart = getCaretPosition();
    calcEnd = G.VIsual.getOffset();
    previousHighlight = calculateVisualBlocks();
    return previousHighlight;
  }

  /**
   * Calculate the visual blocks depending on the visual mode.
   * The visual block array ends with -1,-1
   * If visual mode is not active.. it returns an array containing {-1, -1}
   * @returns te visual blocks
   */
  private int[] calculateVisualBlocks() {
    if (G.VIsual_active) { // if in selection mode
        int start, offset;
        int tmpstart = G.VIsual.getOffset();
        int tmpoffset = getCaretPosition();
        start = tmpstart < tmpoffset ? tmpstart : tmpoffset;
        offset = tmpstart > tmpoffset ? tmpstart : tmpoffset;
        int[] newHighlight;
        if (G.VIsual_mode == 'V') { // line selection mode
            // make sure the entire lines are selected
            if (offset < start) {
                start = getLineEndOffsetFromOffset(start);
                offset = getLineStartOffsetFromOffset(offset);
            } else {
                start = getLineStartOffsetFromOffset(start);
                offset = getLineEndOffsetFromOffset(offset)-1; // end of a line
            }
            newHighlight = new int[]{start, offset, -1, -1};
        } else if (G.VIsual_mode == 'v') {
            if (getCaretPosition() < G.VIsual.getOffset()) {
                offset = offset+1;
            }
            newHighlight = new int[]{start, offset, -1, -1};
        } else if (G.VIsual_mode == (0x1f & (int)('V'))) { // visual block mode
            if (offset < start) {
                start = start+1;
            }
            int startLine = offset > start ? getLineNumber(start) : getLineNumber(offset);
            int endLine = offset < start ? getLineNumber(start) : getLineNumber(offset);
            int colStart = getColumnNumber(start);
            int colEnd = getColumnNumber(offset);
            newHighlight = new int[(((endLine - startLine)+1)*2) + 2];
            for (int i = startLine; i <= endLine; i++) {
                tmpstart = getLineEndOffset(i) < getLineStartOffset(i) + colStart ? getLineEndOffset(i) : getLineStartOffset(i) + colStart;
                int tmpend = getLineEndOffset(i) < getLineStartOffset(i) + colEnd + 1 ? getLineEndOffset(i) : getLineStartOffset(i) + colEnd + 1;
                newHighlight[(i-startLine)*2] = tmpstart;
                newHighlight[((i-startLine)*2)+1] = tmpend;
            }
            newHighlight[newHighlight.length-2] = -1;
            newHighlight[newHighlight.length-1] = -1;
        } else {
            throw new IllegalStateException("Visual mode: "+ G.VIsual_mode +" is not supported");
        }
        return newHighlight;
    } else {
        return new int[] { -1, -1};
    }
  }

  private void unhighlight(int[] blocks) {
      applyBackground(blocks, UNHIGHLIGHT);
  }

  private int[] previousAppliedHighlight = null;
  private void highlight(int[] blocks) {
      if (previousAppliedHighlight != null && !Arrays.equals(previousAppliedHighlight, blocks)) {
          unhighlight(previousAppliedHighlight);
      }
      applyBackground(blocks, HIGHLIGHT);
      previousAppliedHighlight = blocks;
  }

  protected void applyBackground(int[] blocks, MutableAttributeSet mas) {
      /*StyledDocument document  = (StyledDocument) editorPane.getDocument();
      for (int i = 0; i < blocks.length; i+=2) {
          int start = blocks[i];
          int end = blocks[i+1];
          if (start == -1 && end == -1) { // break
              return;
          }
          if (start > end) {
              int tmp = start;
              start = end;
              end = tmp;
        }
        document.setCharacterAttributes(start, end - start, mas, false);
      } */
  }
}
