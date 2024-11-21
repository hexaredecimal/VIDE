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

import javax.swing.text.*;
import com.raelity.jvi.ViTextView;
import com.raelity.jvi.ViMark;
import com.raelity.jvi.ViFPOS;
import com.raelity.jvi.MarkException;
import com.raelity.jvi.MarkOrphanException;
import com.raelity.jvi.*;

/**
 * <b>NEEDSWORK:</b><ul>
 * <li>A Mark should really only have the document, not the TextView.
 * Can do this when there is handling of doc to/from view.
 * <li>Consider garbage collection issues, this is holding ref to
 * both doc and editor.
 * <li>Add file name as class member?
 * <li>Before throwing OrphanMark, search for document in known editors.
 * <ul>
 */
class Mark implements ViMark {
  private Position pos;
  private Document doc;
  private TextView editor;	// This is a ViPane, NEEDSWORK: remove

  /**
   * If the mark offset is not valid then this mark is converted into
   * a null mark.
   */
  void setOffset(int offset, ViTextView editor) {
    Position aPos;

    setEditor((TextView)editor);
    try {
      aPos = doc.createPosition(offset);
      setPos(aPos);
    } catch(BadLocationException ex) {
      pos = null;
      doc = null;
      editor = null;
      return;
    }
  }

  private void setEditor(TextView editor) {
    this.editor = editor;
    doc = editor.getEditorComponent().getDocument();
  }

  Document getDoc() {
    checkMark();
    return doc;
  }

  private void setPos(Position pos) {
    checkMark();
    this.pos = pos;
  }

  private Position getPos() {
    checkMark();
    return pos;
  }

  public int getLine() {
    // NEEDSWORK: should use doc, not editor
    return editor.getLineNumber(getOffset());
  }

  public int getColumn() {
    // NEEDSWORK: should use doc, not editor
    return editor.getColumnNumber(getOffset());
  }

  public int getOffset() {
    checkMark();
    return pos.getOffset();
  }
  public void setData(ViMark mark_arg) {
    Mark mark = (Mark)mark_arg;
    this.pos = mark.pos;
    this.doc = mark.doc;
    this.editor = mark.editor;
  }

  public void invalidate() {
    doc = null;
  }

  public ViFPOS copy() {
    Mark m = new Mark();
    m.setData(this);
    return m;
  }

  final void checkMark() {
    if(doc == null) throw new MarkException("Uninitialized Mark");
    if(doc != editor.getEditorComponent().getDocument()) {
      throw new MarkOrphanException("Mark Document Change");
    }
  }

  final public int compareTo(Object o) {
    ViFPOS p = (ViFPOS)o;
    if(this.getOffset() < p.getOffset()) {
      return -1;
    } else if(this.getOffset() > p.getOffset()) {
      return 1;
    } else {
      return 0;
    }
  }

  public boolean equals(Object o) {
    if(o instanceof ViFPOS) {
      ViFPOS fpos = (ViFPOS)o;
      return getOffset() == fpos.getOffset();
    }
    return false;
  }

  /** This is optional, may throw an UnsupportedOperationException */
  public void setPosition(int line, int col) {
    throw new UnsupportedOperationException();
  }
}
