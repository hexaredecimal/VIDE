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

import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import java.awt.Rectangle;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.JEditorPane;
import javax.swing.JViewport;
import javax.swing.text.Document;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.CaretEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

import com.raelity.jvi.ViTextView;
import com.raelity.jvi.FPOS;
import com.raelity.jvi.WCursor;
import com.raelity.jvi.Util;
import com.raelity.jvi.BooleanOption;
import com.raelity.jvi.Options;
import com.raelity.jvi.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Position;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;


/*
  ViFPOS getCursor()
  PositionSegment getLineSegment(int line)
  PositionSegment getCurrentLineSegment()
  Element getLineElement(int line)
  Document getDoc()
  int getTopLineOffset()
  int getBottomLineOffset()
  int getScreenLines()
*/

/**
 * This class caches information about the various components that make
 * up the VI model. This class listens to these components as needed to
 * keep the cache up to date. All info is derived through a JEditorPane.
 */
public class TextViewCache implements PropertyChangeListener,
                                      CaretListener,
				      DocumentListener,
				      ChangeListener
{
  final static int DIR_TOP = -1;
  final static int DIR_BOT = 1;

  public TextViewCache(ViTextView textView) {
    this.textView = (TextView)textView;
  }

  // NEEDSWORK: can do some minor optimization by keeping some info
  // when cursor is moved within the same line.
  //
  // for methods like "getLineXXX(int line)" only refill cache if
  // line is the same line as the cursor, otherwise just fetch it
  //

  private TextView textView;

  final static private boolean cacheDisabled = false;

  public static BooleanOption cacheTrace
                = (BooleanOption)Options.getOption(Options.dbgCache);

  //
  // When caret moves, update cursor, segment and elemement
  // NEEDSWORK: would be nice to optimize this.....
  //

  private int dot = -1;
  private FPOS cursor = new WCursor() {
    public int getLine() {
      if(invalidCursor) {
        fillCursor();
      }
      return super.getLine();
    }
    public int getColumn() {
      if(invalidCursor) {
        fillCursor();
      }
      return super.getColumn();
    }
    public int getOffset() {
      if(invalidCursor) {
        fillCursor();
      }
      return super.getOffset();
    }
  };
  private boolean invalidCursor = true;
  // NEEDSWORK: keep caret offset from event to help performance
  // re-assign to FPOS, dont keep creating a new one
  final public FPOS getCursor() {
    if(cacheDisabled || cursor == null || invalidCursor) {
      if(cacheTrace.getBoolean())System.err.println("Miss cursor:");
      fillCursor();
    }
    else {
      if(cacheTrace.getBoolean())System.err.println("Hit cursor:");
    }
    return cursor;
  }

  protected void fillCursor() {
    // NEEDSWORK: use dot?
    if(cursor == null) {
      cursor = new WCursor();
      System.err.println("CURSOR should never be null");
    }
    cursor.setCursor(textView);
    invalidCursor = false;
  }

  final private void invalidateCursor(int dot) {
    if(cacheTrace.getBoolean())System.err.println("Inval cursor:");
    // cursor = null;
    this.dot = dot;
    invalidCursor = true;
  }

  /** the segment cache */
  private PositionSegment segment = new PositionSegment();
  // private Segment tempSegment = new Segment();

  /** @return the positionsegment for the indicated line */
  final public PositionSegment getLineSegment(int line) {
    if(cacheDisabled || segment.count == 0 || segment.line != line) {
      if(cacheTrace.getBoolean())System.err.println("Miss seg: " + line);
      try {
	Element elem = getLineElement(line);
	segment.position = elem.getStartOffset();
	getDoc().getText(elem.getStartOffset(),
		    elem.getEndOffset() - elem.getStartOffset(),
		    segment);
	segment.line = line;
        /* **************************************************
        int len = Math.max(80, tempSegment.count + 10);
        if(segment.array == null || segment.array.length < len) {
          segment.array = new char[len];
        }
        System.arraycopy(tempSegment.array, tempSegment.offset,
                         segment.array, 0, tempSegment.count);
        segment.count = tempSegment.count;
        **************************************************/
        // segment.offset is always zero
      } catch(BadLocationException ex) {
	segment.count = 0;
	// NEEDSWORK: how to report exception?
	ex.printStackTrace();
      }
    }
    else {
      if(cacheTrace.getBoolean())System.err.println("Hit seg: " + line);
    }
    return segment;
  }

  final public PositionSegment getCurrentLineSegment() {
    return segment;
  }

  final private void invalidateLineSegment() {
    if(cacheTrace.getBoolean())System.err.println("Inval seg:");
    segment.count = 0;
  }

  protected void fillLineSegment(int line) {
  }

  /** the element cache */
  private Element element;
  /** the line number corresponding to the element cache (0 based). */
  private int elementLine;
  /** @return the element for the indicated line */
  final public Element getLineElement(int line) {
    line--;
    if(cacheDisabled || element == null || elementLine != line) {
      if(cacheTrace.getBoolean())System.err.println("Miss elem: " + (line+1));
      element = getDoc().getDefaultRootElement().getElement(line);
      elementLine = line;
    }
    else {
      if(cacheTrace.getBoolean())System.err.println("Hit elem: " + (line+1));
    }
    return element;
  }

  final public Element getCurrentLineElement() {
    return element;
  }

  final private void invalidateElement() {
    if(cacheTrace.getBoolean())System.err.println("Inval elem:");
    element = null;
  }

  protected void fillLineElement(int line) {
  }

  /** the document */
  private Document doc;
  final public Document getDoc() {
    return doc;
  }

  final public JViewport getViewport() {
    return viewport;
  }

  final public int getFheight() {
    return fheight;
  }

  /** current font information. */
  private FontMetrics fm;
  /** height of the font */
  private int fheight;

  // The visible part of the document, negative means not valid.
  // These values are updated whenever the viewport changes.
  // NEEDSWORK: update view sizes with certain document changes.
  // 		in particular bottomline, blankline

  private JViewport viewport;
  private Point viewportPosition;
  private Dimension viewportExtent;
  private int topLineOffset;
  private int bottomLineOffset;

  private int viewTopLine;
  private int viewBottomLine;
  private int viewLines;

  /** @return the top line number */
  public int getViewTopLine() {
    return viewTopLine;
  }

  public void setViewTopLine(int line) {
    if(line == getViewTopLine()) {
      return; // nothing to change
    }
    int offset = textView.getLineStartOffset(line);
    Rectangle r;
    try {
      r = textView.getEditorComponent().modelToView(offset);
    } catch(BadLocationException e) {
      Util.vim_beep();
      return;
    }
    Point p = r.getLocation();
    p.translate(-p.x, 0); // leave a few pixels to left
    viewport.setViewPosition(p);
  }

  public int getViewBottomLine() {
    return viewBottomLine + 1;	// NEEDSWORK:
  }

  public int getViewBlankLines() {
    int blank = viewLines - (viewBottomLine - viewTopLine + 1);
    return blank;
  }

  /** @return number of lines on viewport */
  public int getViewLines() {
    return viewLines;
  }
  
  protected void fillLinePositions() {
    /*
    SwingUtilities.invokeLater(
      new Runnable() { public void run() { fillLinePositionsFinally(); }});
    */
    fillLinePositionsFinally();
  }

  /** determine document indicators visible in the viewport */
  protected void fillLinePositionsFinally() {
    Point newViewportPosition;
    Dimension newViewportExtent;
    Rectangle r;
    int newViewLines = -1;
    boolean topLineChange = false;
    
    if(viewport == null) {
      newViewportPosition = null;
      newViewportExtent = null;
    } else {
      newViewportPosition = viewport.getViewPosition();
      newViewportExtent = viewport.getExtentSize();
    }
    if(newViewportPosition == null || newViewportExtent == null) {
      viewTopLine = -1;
      viewBottomLine = -1;
      newViewLines = -1;
    } else {
      int newViewTopLine = findFullLine(newViewportPosition, DIR_TOP);
      if(viewTopLine != newViewTopLine) {
	topLineChange = true;
      }
      viewTopLine = newViewTopLine;
      Point pt = new Point(newViewportPosition); // top-left
      pt.translate(0, newViewportExtent.height-1); // bottom-left
      viewBottomLine = findFullLine(pt, DIR_BOT);
      //
      // Calculate number of lines on screen, some may be blank
      //
      newViewLines = newViewportExtent.height / fheight;
    }

    boolean sizeChange = false;
    if( newViewportExtent == null
			|| ! newViewportExtent.equals(viewportExtent)
                        || newViewLines != viewLines) {
      sizeChange = true;
    }
    viewLines = newViewLines;
    viewportPosition = newViewportPosition;
    viewportExtent = newViewportExtent;

    if(sizeChange) {
      ViManager.viewSizeChange(textView);
    }
    if(sizeChange || topLineChange) {
      ViManager.viewMoveChange(textView);
    }
  }

  /**
   * Determine the line number of the text that is fully displayed
   * (top or bottom not chopped off).
   * @return line number of text at point
   */
  private int findFullLine(Point pt, int dir) {
    Rectangle vrect = viewport.getViewRect();
    RSyntaxTextArea editor = textView.getEditorComponent();
    
    int offset = editor.viewToModel(pt);
    int line = textView.getLineNumber(offset);
    Rectangle lrect;
    try {
      lrect = editor.modelToView(offset);
    }
    catch (BadLocationException ex) {
      System.err.println("findFullLine: exeption 1st " + line);
      return line; // can't happen
    }
    
    if(vrect.contains(lrect)) {
      return line;
    }
    int oline = line;
    line -= dir; // move line away from top/bottom
    if(line < 1 || line > textView.getLineCount()) {
      //System.err.println("findFullLine: line out of bounds " + line);
      return oline;
    }
    offset = textView.getLineStartOffset(line);
    try {
      lrect = editor.modelToView(offset);
    }
    catch (BadLocationException ex) {
      System.err.println("findFullLine: exeption 2nd " + line);
      return line; // can't happen
    }
    if( ! vrect.contains(lrect)) {
      //System.err.println("findFullLine: adjusted line still out " + line);
    }
    return line;
  }
  
  private void invalidateData() {
    invalidateCursor(-1);
    invalidateLineSegment();
    invalidateElement();
  }

  //
  // Track changes of interest.
  //
  // There are two types of cached things we're interested in,
  //	- data from the document, especially around the cursor
  //	  Caret movement and document changes affect this
  //
  //	- visible screen: top line, bottom line
  //	  resize, viewport affects this
  //

  private void changeDocument(Document doc) {
    if(cacheTrace.getBoolean())System.err.println("doc switch: ");
    if(this.doc != null) {
      this.doc.removeDocumentListener(this);
    }
    this.doc = doc;
    if(doc != null) {
      doc.addDocumentListener(this);
    }
    invalidateData();
  }

  private void changeFont(Font f) {
    int h;
    if(f == null) {
      fm = null;
    } else {
      fm = textView.getEditorComponent().getFontMetrics(f);
      fheight = fm.getHeight();
      fillLinePositions();
    }
  }

  /** The container for the editor has changed. */
  private void changeViewport(Object component) {
    if(viewport != null) {
      viewport.removeChangeListener(this);
    }
    if (component instanceof JViewport) {
      viewport = (JViewport)component;
      viewport.addChangeListener(this);
    } else {
      viewport = null;
    }
    changeView(true);
  }

  /** The defining rectangle of the viewport has changed
   *  @param init true indicates that the position should be
   *  checked immeadiately, not potentially defered with an invoke later.
   */
  private void changeView(boolean init) {
    if(init) {
      fillLinePositionsFinally();
    } else {
      fillLinePositions();
    }
  }

  private void changeCaretPosition(int dot, int mark) {
    if(textView.expectedCaretPosition != dot) {
      //System.err.println("changeCaretPosition: "
			 //+ textView.expectedCaretPosition + " " + dot);
      ViManager.unexpectedCaretChange(dot);
    }
    invalidateCursor(dot);
    //cursor = null;
    // NEEDSWORK: update the FPOS right now!
  }

  /** This is called from the managing textview,
   * listen to things that affect the cache.
   */
  public void attach(RSyntaxTextArea editor) {
    if(G.dbgEditorActivation.getBoolean()) {
      System.err.println("TVCache: attach: "
              + (editor == null ? 0 : editor.hashCode()));
    }
    if(freezer != null) {
        freezer.stop();
        freezer = null;
    }
    
    if(hasListeners)
        return;
    
    hasListeners = true;
    editor.addPropertyChangeListener("font", this);
    changeDocument(editor.getDocument());
    editor.addPropertyChangeListener("document", this);
    editor.addPropertyChangeListener("ancestor", this);
    editor.addCaretListener(this);
    changeFont(editor.getFont());
    changeViewport(editor.getParent());
  }
  
  boolean hasListeners = false;

  /** Dissassociate from the observed components. */
  public void detach(RSyntaxTextArea ep) {
    if(G.dbgEditorActivation.getBoolean()) {
      System.err.println("TVCache: detach: "
              + (ep == null ? "" : ep.hashCode()));
    }
    if(ep == null) {
      return;
    }
    freezer = new FreezeViewport(ep);
    
    //removeListeners();
  }
  
  public void shutdown(RSyntaxTextArea ep) {
    if(freezer != null) {
        freezer.stop();
        freezer = null;
    }
    removeListeners();
  }
  
  private void removeListeners() {
    hasListeners = false;
    RSyntaxTextArea editor = textView.getEditorComponent();
    editor.removePropertyChangeListener("font", this);
    editor.removePropertyChangeListener("document", this);
    editor.removePropertyChangeListener("ancestor", this);
    changeDocument(null);
    editor.removeCaretListener(this);
    changeViewport(null);
  }
  
  private FreezeViewport freezer;

  //
  // Listener events
  //

  // -- property change events --

  public void propertyChange(PropertyChangeEvent e) {
    String p = e.getPropertyName();
    Object o = e.getNewValue();
    if("font".equals(p)) {
      changeFont((Font)o);
    } else if("document".equals(p)) {
      changeDocument((Document)o);
    } else if("ancestor".equals(p)) {
      changeViewport(o);
    }
  }

  // -- caret event --

  public void caretUpdate(CaretEvent e) {
    changeCaretPosition(e.getDot(), e.getMark());
  }

  // -- document events --

  public void changedUpdate(DocumentEvent e) {
    if(cacheTrace.getBoolean()) {
      System.err.println("doc changed: " +e.getOffset() + ":" + e.getLength() + " " + e);
      // System.err.println("element" + e.getChange());
    }
    // insertUpdate/removeUpdate fire as well so skip this one
    // invalidateData();
  }
  
  // These variables track last insert/remove to document.
  // They are usually used for undo/redo.
  private int undoOffset;
  private int undoLength;
  private boolean undoChange;

  public void insertUpdate(DocumentEvent e) {
    if(cacheTrace.getBoolean())
        System.err.println("doc insert: " +e.getOffset() + ":" + e.getLength() + " " + e);
    invalidateData();
    undoOffset = e.getOffset();
    undoLength = e.getLength();
    undoChange = true;
  }

  public void removeUpdate(DocumentEvent e) {
    if(cacheTrace.getBoolean())
        System.err.println("doc remove: " +e.getOffset() + ":" + e.getLength() + " " + e);
    invalidateData();
    undoOffset = e.getOffset();
    undoLength = e.getLength();
    undoChange = true;
  }

  public int getUndoOffset() {
    return undoOffset;
  }

  public int getUndoLength() {
    return undoLength;
  }

  /**
   * This method can be used to determine if some action(s)
   * cause a change. The method itself has nothing to do with undo. It
   * is called from an optimized undo.
   * 
   * @return true if there has be a change to the document since this method
   * was last called.
   */
  public boolean isUndoChange() {
    boolean rval;
    rval = undoChange;
    undoChange = false;
    return rval;
  }

  // -- viewport event --

  public void stateChanged(ChangeEvent e) {
    changeView(false);
  }
    
    /**
     * Stabilize (do not allow scrolling) the JViewport displaying
     * the indicated RSyntaxTextArea.
     * This is typically used when the underlying document may change while
     * being edited in another view. The {@link #stop} method is used to release
     * the listeners and so unfreeze the viewport.
     * <p>This is a one shot class. The editor is expected to be good to go.
     * Only document changes are listened to. The first char of the top line is
     * pinned to the upper left corner. If needed, this could be extended
     * to pin the horizontal position as well.
     */
    public static class FreezeViewport implements DocumentListener {
        private RSyntaxTextArea ep;
        private JViewport vp;
        private AbstractDocument doc;
        private Position pos;
        private int topLine;
        private int nLine;
        
        public FreezeViewport(RSyntaxTextArea ep) {
            this.ep = ep;
            Object o = ep.getDocument();
            if(!(o instanceof AbstractDocument))
                return;
            doc = (AbstractDocument)ep.getDocument();
            try {
                doc.readLock();
                vp = (JViewport)ep.getParent(); // may throw class cast, its ok
                Element root = doc.getDefaultRootElement();
                nLine = root.getElementCount();
                
                // Get the offset of the first displayed char in the top line
                Point pt = vp.getViewPosition();
                int offset = ep.viewToModel(pt);
                
                // Determine the line number of the top displayed line
                topLine = root.getElementIndex(offset);
                
                // Note. offset may not be first char, due to horiz scroll
                // make offset the first char of the line
                offset = root.getElement(topLine).getStartOffset();
                
                // Get marker to offset in the document
                pos = doc.createPosition(offset);
                doc.addDocumentListener(this);
            } catch (Exception ex) {
                // Note: did not start listener
            } finally {
                doc.readUnlock();
            }
        }
    
        public void stop() {
            doc.removeDocumentListener(this);
        }
    
        private void adjustViewport(int offset) {
            // Might be able to use info from DocumentEvent to optimize
            try {
                Point pt = ep.modelToView(offset).getLocation();
                pt.translate(-pt.x, 0); // x <-- 0, leave a few pixels to left
                vp.setViewPosition(pt);
            } catch(Exception ex) {
                stop();
            }
            return;
        }
        
        private void handleChange(DocumentEvent e) {
            // Note while in listener document can't change, no read lock
            Element root = doc.getDefaultRootElement();
            int newNumLine = root.getElementCount();
            // return if line count unchanged or changed after our mark
            if(nLine == newNumLine || e.getOffset() > pos.getOffset())
                return;
            nLine = newNumLine;
            
            int newTopLine = root.getElementIndex(pos.getOffset());
            if(topLine == newTopLine)
                return;
            topLine = newTopLine;
            
            // make a move
            final int offset = root.getElement(topLine).getStartOffset();
            if(EventQueue.isDispatchThread()) {
                adjustViewport(offset);
            } else {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        adjustViewport(offset);
                    }
                });
            }
        }

        public void insertUpdate(DocumentEvent e) { handleChange(e); }

        public void removeUpdate(DocumentEvent e) { handleChange(e); }

        public void changedUpdate(DocumentEvent e) { }
    }
}
