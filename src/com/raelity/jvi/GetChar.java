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

import com.raelity.jvi.swing.KeyBinding;
import javax.swing.text.AbstractDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

public class GetChar implements Constants, KeyDefs {
  private static boolean block_redo = false;
  private static boolean handle_redo = false;

  /** An input char from the user has been recieved.
   * Implement the key into 16 bit mapping defined in KeyDefs.
   * <p>
   * There is still a hole. This doesn't handle stuff where screwy
   * modifiers are applied to normal keys, e.g. Ctrl-Shft-Alt-A.
   * There is room in the reseved
   * area to map the regular keys, probably want to put them in the
   * E000-E07f range, and put the K_UP type keys in the E080 range.
   * </p>
   */
  static void gotc(int key, int modifier) {
    if((key & 0xF000) == VIRT) {
      if((modifier & KeyBinding.MOD_MASK) == SHFT
                && key >= VIRT && key <= VIRT + 0x0f) {
        // only the shift key is pressed and its one of "those".
        key += SHIFTED_VIRT_OFFSET;
      }
    }
    G.setModMask(modifier);

    last_recorded_len = 0;      // for the one in vgetc()
    userInput(key);
    
    assert(!handle_redo);
    handle_redo = false;
    
    // Lock document while handling a single user character
    // EXPERIMENTAL
    AbstractDocument doc = null;
    if(true)
        doc = null; // DISABLE
    else {
        RSyntaxTextArea ep = G.curwin.getEditorComponent();
        if(ep.getDocument() instanceof AbstractDocument)
            doc = (AbstractDocument)ep.getDocument();
    }
    
    try {
        if(doc != null)
            doc.readLock();
        
        Normal.processInputChar(key, true);

        if(!handle_redo)
            pumpVi();
        else {
            //
            // Handle some type of redo command: start_redo or start_redo_ins.
            // Execute these commands atomically, with the file locked if
            // supported by the platform. We need these special brackets since
            // while executing the command, other begin/endUndo's may occur.
            // 
            // During the above processInputChar, a redo command was setup by
            // stuffing the buffer. pumpVi() delivers stuffbuf and typebuf
            // to processInputChar. It's tempting to always bracket pumpVi with
            // begin/endRedoUndo, but macro execution might end in input mode.
            //
            try {
                Misc.beginRedoUndo();
                pumpVi();
            } finally {
                Misc.endRedoUndo();
                handle_redo = false;
            }
        }
    } finally {
        if(doc != null)
            doc.readUnlock();
    }
    Misc.out_flush();   // returning from event
                        // only do this if no pending characters
                        // but can't test for pending characters, so ....
  }
  
  /** This is a special case for the two part search */
  static void fakeGotc(int key) {
    G.setModMask(0);
    Normal.processInputChar(key, true);
    Misc.out_flush();   // returning from event
  }

  /**
   * Pass queued up characters to vi for processing.
   * First from stuffbuf, then typebuf.
   */
  private static void pumpVi() {
    // NEEDSWORK: pumpVi: check for interupt?
      
    while(true) {
      while(stuffbuff.hasNext()) {
        pumpChar(stuffbuff.getNext());
      }
      if(typebuf.hasNext()) {
        pumpChar(typebuf.getNext());
      } else {
        break;
      }
    }
    G.Exec_reg = false;
  }

  /**
   * @return a queued character from input stream.
   * @exception RuntimeException if no characters are available
   */
  private static char getOneChar() {
    if(stuffbuff.hasNext()) {
      return (char)stuffbuff.getNext();
    }
    if(typebuf.hasNext()) {
      return (char)typebuf.getNext();
    }
    throw new RuntimeException("No character available");
  }

  private static void pumpChar(int c) {
    int modifiers = 0;
    if((c & 0xF000) == VIRT) {
      modifiers = (c >> MODIFIER_POSITION_SHIFT) & 0x0f;
      c &= ~(0x0f << MODIFIER_POSITION_SHIFT);
    }
    G.setModMask(modifiers);
    Normal.processInputChar(c, true);
  }

  /**
   * read from the typebuf until a CR, return what was read, discarding
   * the CR. If the input stream empties, just return what was available.
   * @return true when a CR was encountered, else false
   */
  public static boolean getRecordedLine(StringBuffer sb) {
    boolean hasCR = false;
    while(true) {
      if( ! char_avail()) {
        break;
      }
      char c = getOneChar();
      if(c == '\n') {
        hasCR = true;
        break;
      }
      sb.append(c);
    }
    return hasCR;
  }

  /**
   * Call vpeekc() without causing anything to be mapped.
   * @return true if a character is available, false otherwise.
   */
  static boolean char_avail() {
    // NOTE: Look at description for vgetorpeek; there's more to add

    if(stuffbuff.hasNext() || typebuf.hasNext()) {
      return true;
    }

    return false; // would really like to check...

    // return keyInput.hasChar();

  }

  /**
   * Write typed characters to script file.
   * If recording is on put the character in the recordbuffer.
   * Put the current modifies into the character if needed,
   * {@see KeyDefs} for info on character layout with modifiers.
   */
  private static void userInput(int c /*, int len*/) {

    /* remember how many chars were last recorded */
    if (G.Recording) {
      last_recorded_len += 1;
      if((c & 0xF000) == VIRT) {
        c |= (G.getModMask() << MODIFIER_POSITION_SHIFT);
      }
      recordbuff.append((char)c);
    }

    /* ******************************************************************
    buf[1] = NUL;
    while (len--)
    {
      c = *s++;
      updatescript(c);

      if (Recording)
      {
        buf[0] = c;
        add_buff(&recordbuff, buf);
      }
    }
    may_sync_undo();
    *******************************************************************/
  }

  /**
   * Write a stuff to the script and/or record buffer.
   * Used to stash command entry input.
   */
  public static void userInput(String s) {
    /* remember how many chars were last recorded */
    if (G.Recording) {
      last_recorded_len += s.length();
      recordbuff.append(s);
    }
  }

  /**
   * Return the contents of the record buffer as a single string
   *  and clear the record buffer.
   */
  static String get_recorded() {
    //
    // Remove the characters that were added the last time, these must be the
    // (possibly mapped) characters that stopped recording.
    //
    int len = recordbuff.length();
    if (len >= last_recorded_len) {
      len -= last_recorded_len;
      recordbuff.setLength(len);
    }

    /* *******************************************************************
    //
    // When stopping recording from Insert mode with CTRL-O q, also remove the
    // CTRL-O.
    //
    if (len > 0 && restart_edit && p[len - 1] == Ctrl('O'))
      p[len - 1] = NUL;
    *************************************************************/

    String s = recordbuff.toString();
    recordbuff = new BufferQueue();

    return s;
  }

  /**
   * get a character: 1. from a previously ungotten character
   *		    2. from the stuffbuffer
   *		    3. from the typeahead buffer
   *		    4. from the user
   * <br>
   * if "advance" is TRUE (vgetc()):
   *	really get the character.
   *	KeyTyped is set to TRUE in the case the user typed the key.
   *	KeyStuffed is TRUE if the character comes from the stuff buffer.
   * <br>
   * if "advance" is FALSE (vpeekc()):
   *	just look whether there is a character available.
   */

  static boolean vgetorpeek(boolean advance) {
    // Just a place holder for the above comment for now
    return false;
  }

  /**
   * prepare stuff buffer for reading (if it contains something).
   */
  private static void start_stuff() {
    // NEEDSWORK: start_stuff: should this this is a nop
    // stuffbuff = new BufferQueue();
  }

  static boolean stuff_empty() {
    Normal.do_xop("stuff_empty");
    return ! stuffbuff.hasNext();
  }

  /**
   * The previous contents of the redo buffer is kept in old_redobuffer.
   * This is used for the CTRL-O <.> command in insert mode.
   */
  static void ResetRedobuff() {
    if (!block_redo) {
      // old_redobuff = redobuff;
      redobuff = new BufferQueue();
    }
  }

  static void AppendToRedobuff(String s) {
    if (!block_redo) {
      redobuff.append(s);
    }
  }

  static void AppendCharToRedobuff(int c) {
    if (!block_redo) {
      redobuff.append((char)c);
    }
  }

  static void AppendNumberToRedobuff(int n) {
    if (!block_redo) {
      redobuff.append(n);
    }
  }


  /**
   * Remove the contents of the stuff buffer and the mapped characters in the
   * typeahead buffer (used in case of an error). If 'typeahead' is true,
   * flush all typeahead characters (used when interrupted by a CTRL-C).
   */
  public static void flush_buffers(boolean typeahead) {
    init_typebuf();
    start_stuff();
    stuffbuff.setLength(0); // while(read_stuff(true) != NUL);

    /* *******************************************************************
    // NEEDSWORK: finish flush_buffer
    if (typeahead) {	    // remove all typeahead
      //
      // We have to get all characters, because we may delete the first part
      // of an escape sequence.
      // In an xterm we get one char at a time and we have to get them all.
      //
      while (inchar(typebuf, typebuflen - 1, 10L))
	;
      typeoff = MAXMAPLEN;
      typelen = 0;
    } else {		    // remove mapped characters only
      typeoff += typemaplen;
      typelen -= typemaplen;
    }
    typemaplen = 0;
    no_abbr_cnt = 0;
    *********************************************************************/
  }
  
  //
  // The stuff buff used from externally by the "r" and "*" (nv_ident) commands.
  //

  static void stuffcharReadbuff(int n) {
    stuffbuff.append((char)n);
  }

  static void stuffnumReadbuff(int n) {
    stuffbuff.append(n);
  }

  static void stuffReadbuff(String s) {
    stuffbuff.append(s);
  }

  /**
   * Read a character from the redo buffer.
   * The redo buffer is left as it is.
   * <p>NOTE: old_redo ignored</p>
   * @param init if TRUE, prepare for redo, return FAIL if nothing to redo, OK
   * otherwise
   * @param old_redo if TRUE, use old_redobuff instead of redobuff
   */
  static private int read_redo(boolean init, boolean old_redo) {
    if(init) {
      if(redobuff.hasNext() == false) {
	return FAIL;
      } else {
	redobuff_idx = 0;
	return OK;
      }
    }
    int c = NUL; // assume none left
    if(redobuff.hasCharAt(redobuff_idx)) {
      c = redobuff.getCharAt(redobuff_idx);
      redobuff_idx++;
    }
    return c;
  }

  /**
   * Copy the rest of the redo buffer into the stuff buffer (could do faster).
   * if old_redo is TRUE, use old_redobuff instead of redobuff
   * <p>NOTE: old_redo ignored</p>
   */
  static private void copy_redo(boolean old_redo) {
    stuffReadbuff(redobuff.substring(redobuff_idx));
  }

  /**
   * Stuff the redo buffer into the stuffbuff.
   * Insert the redo count into the command.
   * If 'old_redo' is TRUE, the last but one command is repeated
   * instead of the last command (inserting text). This is used for
   * CTRL-O <.> in insert mode
   * <p>NOTE: old_redo true causes and exception</p>
   *
   * @return FAIL for failure, OK otherwise
   */
  static int start_redo(int count, boolean old_redo) {
    if(old_redo) {
      throw new RuntimeException("old_redo not false");
    }
    if(read_redo(true, old_redo) == FAIL) {
      return FAIL;
    }

    int c = read_redo(false, old_redo);

    // copy the buffer name if present
    if (c == '"') {
      stuffbuff.append((char)c);
      c = read_redo(false, old_redo);

      // if a numbered buffer is used, increment the number
      if (c >= '1' && c < '9')
	++c;
      stuffbuff.append((char)c);
      c = read_redo(false, old_redo);
    }

    /* ********************************************************/
    if (c == 'v') {   // redo Visual	// VISUAL
      G.VIsual = (FPOS) G.curwin.getWCursor().copy();
      G.VIsual_active = true;
      G.VIsual_reselect = true;
      G.redo_VIsual_busy = true;
      c = read_redo(false, old_redo);
    }
    /*******************************************************/

    // try to enter the count (in place of a previous count)
    if (count != 0) {
      while (Util.isdigit(c)) {	// skip "old" count
	c = read_redo(false, old_redo);
      }
      stuffbuff.append(count);
    }

    // copy from the redo buffer into the stuff buffer
    stuffbuff.append((char)c);
    copy_redo(old_redo);
    handle_redo = true;
    return OK;
  }

  /**
   * Repeat the last insert (R, o, O, a, A, i or I command) by stuffing
   * the redo buffer into the stuffbuff.
   * return FAIL for failure, OK otherwise
   */
  static int start_redo_ins() {
    int	    c;

    if (read_redo(true, false) == FAIL)
      return FAIL;
    start_stuff();

    /* skip the count and the command character */
    while ((c = read_redo(false, false)) != NUL) {
      if (Util.vim_strchr("AaIiRrOo", c) != null) {
	if (c == 'O' || c == 'o')
	  stuffReadbuff(NL_STR);
	break;
      }
    }

    /* copy the typed text from the redo buffer into the stuff buffer */
    copy_redo(false);
    block_redo = true;
    handle_redo = true;
    return OK;
  }

  static void stop_redo_ins() {
    block_redo = false;
  }

  //
  // typebuf stuff
  //

  static int typemaplen;

  static int typebuf_maplen() {
    return typemaplen;
  }

  static private void init_typebuf() {
    typebuf.setLength(0);
  }

  /**
   * insert a string in position 'offset' in the typeahead buffer (for "@r"
   * and ":normal" command, vgetorpeek() and check_termcode())
   * <p>
   * If noremap is 0, new string can be mapped again.
   * <br>If noremap is -1, new string cannot be mapped again.
   * <br>If noremap is >0, that many characters of the new string
   * cannot be mapped.
   * </p><p>
   * If nottyped is TRUE, the string does not return KeyTyped (don't use when
   * offset is non-zero!).
   * </p>
   *
   * @return FAIL for failure, OK otherwise
   */
  static int ins_typebuf(String str, int noremap,
                         int offset, boolean nottyped)
  {
    // NEEDSWORK: ins_typebuf: performance can be improved
    if(typebuf.length() + str.length() > MAXTYPEBUFLEN) {
      Msg.emsg(Messages.e_toocompl);     // also calls flushbuff
      //setcursor();
      return FAIL;
    }

    typebuf.insert(0, str);

    //
    // Adjust noremapbuf[] for the new characters:
    // If noremap  < 0: all the new characters are flagged not remappable
    // If noremap == 0: all the new characters are flagged mappable
    // If noremap  > 0: 'noremap' characters are flagged not remappable, the
    //			rest mappable
    //

    ////// NEEDSWORK: ins_typebuf: mapping stuff

    /* ************************************************************
		    // this is only correct for offset == 0!
    if (nottyped)			// the inserted string is not typed
      typemaplen += str.length();
    if (no_abbr_cnt && offset == 0)	// and not used for abbreviations
      no_abbr_cnt += str.length();
    *************************************************************/

    return OK;
  }

  //
  // The various character queues
  //

  private static BufferQueue stuffbuff = new BufferQueue();
  private static BufferQueue redobuff = new BufferQueue();
  private static BufferQueue recordbuff = new BufferQueue();
  private static BufferQueue typebuf = new BufferQueue();

  private static int last_recorded_len = 0;  // number of last recorded chars
  private static int redobuff_idx = 0;
  // static BufferQueue old_redobuff;
}

/**
 * Small queue of characters. Can't extend StringBuffer, so delegate.
 */
class BufferQueue {
  private StringBuilder buf = new StringBuilder();

  void setLength(int length) {
    buf.setLength(length);
  }

  int length() {
    return buf.length();
  }

  boolean hasNext() {
    return buf.length() > 0;
  }

  int getNext() {
    int c = buf.charAt(0);
    buf.deleteCharAt(0);
    return c;
  }

  BufferQueue insert(int ix, String s) {
    buf.insert(ix, s);
    return this;
  }

  boolean hasCharAt(int idx) {
    return buf.length() > idx;
  }

  int getCharAt(int idx) {
    return buf.charAt(idx);
  }

  String substring(int idx) {
    return buf.substring(idx);
  }

  BufferQueue append(char c) {
    buf.append(c);
    return this;
  }

  BufferQueue append(int n) {
    buf.append(n);
    return this;
  }

  BufferQueue append(String s) {
    buf.append(s);
    return this;
  }

  public String toString() {
    return buf.toString();
  }
}
