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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TooManyListenersException;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import javax.swing.text.Segment;
import com.raelity.text.*;

import static com.raelity.jvi.KeyDefs.K_X_SEARCH_FINISH;
import static com.raelity.jvi.KeyDefs.K_X_INCR_SEARCH_DONE;
import static com.raelity.jvi.KeyDefs.K_X_SEARCH_CANCEL;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import static com.raelity.jvi.Constants.*;

/**
 * Searching, regexp and substitution.
 * Everything's static, can only do one thing at a time.
 */
class Search {

  ///////////////////////////////////////////////////////////////////////
  //
  // normal searching like "/" and "?"
  //
  private static ViCmdEntry searchCommandEntry;
  
  // parameters for the current search, they do not change during search
  private static int searchCount;
  private static int searchFlags;
  private static int lastDir = FORWARD;
  
  // state when incremental search started
  private static ViFPOS searchPos;
  private static int searchTopLine;
  private static boolean didIncrSearch;

  private static String lastPattern;
  private static String lastSubstitution;
  
  private static ViCmdEntry getSearchCommandEntry() {
    if(searchCommandEntry == null) {
      try {
        searchCommandEntry = ViManager.getViFactory()
                              .createCmdEntry(ViCmdEntry.SEARCH_ENTRY);
        searchCommandEntry.addActionListener(
          new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
              searchEntryComplete(ev);
            }});
      }
      catch (TooManyListenersException ex) {
        ex.printStackTrace();
        throw new RuntimeException();
      }
    }
    return searchCommandEntry;
  }
  
  private static String fetchPattern() {
      return getSearchCommandEntry().getCommand();
  }

  static private void searchEntryComplete(ActionEvent ev) {
    String cmd = ev.getActionCommand();
    boolean acceptIncr = false;
    boolean cancel = false;
    
    if(cmd.charAt(0) == '\n') {
        if(G.p_incr_search.getBoolean()
           && didIncrSearch
           && ! "".equals(fetchPattern()))
          acceptIncr = true;
    } else
      cancel = true;
            
    if(G.p_incr_search.getBoolean()) {
        stopIncrementalSearch(acceptIncr);
    }
    
    ViManager.stopCommandEntry();
    if(acceptIncr)
      GetChar.fakeGotc(K_X_INCR_SEARCH_DONE);
    else if(cancel)
      GetChar.fakeGotc(K_X_SEARCH_CANCEL);
    else
      GetChar.fakeGotc(K_X_SEARCH_FINISH);
  }
  
  /** Start the entry dialog and stash the interesting info for later use
   *  int doSearch(). */
  static void inputSearchPattern(CMDARG cap, int count, int flags) {
    String mode = "";
    int cmdchar = cap.cmdchar;
    if (cmdchar == '/') {
      mode = "/";
      lastDir = FORWARD;
    } else if (cmdchar == '?') {
      mode = "?";
      lastDir = BACKWARD;
    }
    searchCount = count;
    searchFlags = flags;
    
    ViCmdEntry ce = getSearchCommandEntry();
    if(G.p_incr_search.getBoolean())
        startIncrementalSearch();
    ViManager.startCommandEntry(ce, mode, G.curwin, null);
  }
  
  /** doSearch() should only be called after inputSearchPattern() */
  static int doSearch() {
    String pattern = fetchPattern();
    G.curwin.setWSetCurswant(true);
    if(pattern.equals("")) {
      if(lastPattern == null) {
        Msg.emsg(Messages.e_noprevre);
        return 0;
      }
      pattern = lastPattern;
    }
    lastPattern = pattern;
    // executeSearch(pattern, lastDir, G.p_ic.getBoolean());
    ViFPOS pos = G.curwin.getWCursor().copy();
    int rc = searchit(null, pos, lastDir, pattern,
                      searchCount, searchFlags, 0, G.p_ic.getBoolean());
    if(rc != FAIL) {
      Msg.smsg((lastDir == FORWARD ? "/" : "?") + pattern);
    }
    
    if(rc == FAIL) {
      return 0;
    } else {
      return 1; // NEEDSWORK: not returning 2 ever, so not line mode.
    }
    
    /* ***************************
    if(rc == 0) {
      Normal.clearopInstance();
    }
    ******************************/
  }
  
  private static DocumentListener isListener;
  private static void startIncrementalSearch() {
      Document doc = getSearchCommandEntry().getTextComponent().getDocument();
      isListener = new DocumentListener() {
          public void changedUpdate(DocumentEvent e) { }
          public void insertUpdate(DocumentEvent e) {
              doIncrementalSearch();
          }
          public void removeUpdate(DocumentEvent e) {
              doIncrementalSearch();
          }
      };
      searchPos = G.curwin.getWCursor().copy();
      searchTopLine = G.curwin.getViewTopLine();
      didIncrSearch = false;
      doc.addDocumentListener(isListener);
  }
  
  private static void stopIncrementalSearch(boolean accept) {
      Document doc = getSearchCommandEntry().getTextComponent().getDocument();
      doc.removeDocumentListener(isListener);
      
      if(accept) {
          lastPattern = fetchPattern();
      } else
        resetViewIncrementalSearch();
  }
  
  private static void resetViewIncrementalSearch() {
    G.curwin.setViewTopLine(searchTopLine);
    G.curwin.setCaretPosition(searchPos.getOffset());
  }
  
  private static void doIncrementalSearch() {
      String pattern = getSearchCommandEntry().getTextComponent().getText();
      
      if("".equals(pattern))
          return;
      ViFPOS pos = searchPos.copy();
      int rc = searchit(null, pos, lastDir, pattern,
                        searchCount, searchFlags /*& ~SEARCH_MSG*/,
                        0, G.p_ic.getBoolean());
      didIncrSearch = true;
      if(rc == FAIL)
          resetViewIncrementalSearch();
  }

  static int doNext(CMDARG cap, int count, int flag) {
    G.curwin.setWSetCurswant(true);
    int dir = ((flag & SEARCH_REV) != 0 ? - lastDir : lastDir);
    //G.curwin.repeatSearch(dir);
    int rc = FAIL;
    if(lastPattern == null) {
      Msg.emsg(Messages.e_noprevre);
    } else {
      Msg.smsg((dir == FORWARD ? "/" : "?") + lastPattern);
      // executeSearch(lastPattern, dir, G.p_ic.getBoolean());
      ViFPOS pos = G.curwin.getWCursor().copy();
      rc = searchit(null, pos, dir, lastPattern,
                        count, flag, 0, G.p_ic.getBoolean());
    }
    return rc;
  }

  ////////////////////////////////////////////////////////////////
  //
  // searchc
  //

  /** for searchc, last character searched for. */
  static int	    lastc = 0;
  /** for searchc, last direction of character search. */
  static int	    lastcdir;
  /** for searchc, last type of search ("find" or "to"). */
  static boolean    lastctypeT;

  /**
   * searchc(c, dir, typeT, count);
   *
   * Search for character 'c', in direction 'dir'. If 'typeT' is 0, move to the
   * position of the character, otherwise move to just before the char.
   * Repeat this 'count' times.
   */
  static boolean searchc(int c, int dir, boolean typeT, int count) {
    int		    col;
    int		    len;

    if (c != 0)	/* normal search: remember args for repeat */
    {
      // if (!KeyStuffed)    // don't remember when redoing
      {
	lastc = c;
	lastcdir = dir;
	lastctypeT = typeT;
      }
    } else {		// repeat previous search
      if (lastc == 0)
	return false;
      if (dir != 0)	// repeat in opposite direction
	dir = -lastcdir;
      else
	dir = lastcdir;
      typeT = lastctypeT;
      c = lastc;
    }

    ViFPOS fpos = G.curwin.getWCursor();
    Segment seg = G.curwin.getLineSegment(fpos.getLine());
    col = fpos.getColumn();
    len = seg.count - 1; // don't count the newline, MUST_HAVE_NL

    while (count-- != 0)
    {
      for (;;)
      {
	if ((col += dir) < 0 || col >= len)
	  return false;
	if (seg.array[seg.offset + col] == c)
	  break;
      }
    }
    if (typeT) {
      col -= dir;
    }
    // curwin->w_cursor.col = col;
    G.curwin.setCaretPosition(
		  col + G.curwin
			  .getLineStartOffsetFromOffset(fpos.getOffset()));
    return true;
  }

  ////////////////////////////////////////////////////////////////////
  //
  // The following routines do the word searches performed by the 'w', 'W',
  // 'b', 'B', 'e', and 'E' commands.
  //

  //
  // To perform these searches, characters are placed into one of three
  // classes, and transitions between classes determine word boundaries.
  //
  // The classes are:
  //
  // 0 - white space
  // 1 - keyword charactes (letters, digits and underscore)
  // 2 - everything else
  //

  /* type of the word motion being performed,
   * true implies capital motion (W, E) vs (w, e).
   */
  private static boolean stype;

  /**
   * cls() - returns the class of character at curwin->w_cursor
   *<br>
   * The 'type' of the current search modifies the classes of characters if a
   * 'W', 'B', or 'E' motion is being done. In this case, chars. from class 2
   * are reported as class 1 since only white space boundaries are of interest.
   */
  private static int cls() {
    int	    c;

    c = Misc.gchar_cursor();
    if (c == ' ' || c == '\t' || c == '\n')
      return 0;

    if (Misc.vim_iswordc(c))
      return 1;

    //
    // If stype is non-zero, report these as class 1.
    //
    return ( ! stype) ? 2 : 1;
  }


  /**
   * fwd_word(count, type, eol) - move forward one word
   *<br>
   * Returns FAIL if the cursor was already at the end of the file.
   * If eol is TRUE, last word stops at end of line (for operators).
   */
  static int fwd_word(int count, boolean type, boolean eol) {
    int		sclass;	    /* starting class */
    int		i;
    boolean	last_line;

    stype = type;
    while (--count >= 0) {
      sclass = cls();

      //
      // We always move at least one character, unless on the last character
      // in the buffer.
      //
      last_line = (G.curwin.getWCursor().getLine()
		   	== G.curwin.getLineCount());
      i = Misc.inc_cursor();
      if (i == -1 || (i == 1 && last_line)) // started at last char in file
	return FAIL;
      if (i == 1 && eol && count == 0)      // started at last char in line
	return OK;

      //
      // Go one char past end of current word (if any)
      //
      if (sclass != 0)
	while (cls() == sclass) {
	  i = Misc.inc_cursor();
	  if (i == -1 || (i == 1 && eol && count == 0))
	    return OK;
	}

      //
      // go to next non-white
      //
      while (cls() == 0) {
	//
	// We'll stop if we land on a blank line
	//
	if(G.curwin.getWCursor().getColumn() == 0
	   	&& Misc.gchar_cursor() == '\n') {
	  break;
	}

	i = Misc.inc_cursor();
	if (i == -1 || (i == 1 && eol && count == 0))
	  return OK;
      }
    }
    return OK;
  }

  /**
   * bck_word() - move backward 'count' words
   *<br>
   * If stop is TRUE and we are already on the start of a word, move one less.
   *
   * @return FAIL if top of the file was reached.
   */
  static int bck_word(int count, boolean type, boolean stop) {
    int		sclass;	    /* starting class */

    stype = type;
    while (--count >= 0) {
      sclass = cls();
      if (Misc.dec_cursor() == -1)     /* started at start of file */
	return FAIL;

finished_block:
      do {
	if (!stop || sclass == cls() || sclass == 0) {
	  //
	  // Skip white space before the word.
	  // Stop on an empty line.
	  //
	  while (cls() == 0) {
	    ViFPOS fpos = G.curwin.getWCursor();
	    if (fpos.getColumn() == 0 && Util.lineempty(fpos.getLine()))
	      break finished_block;
	    if (Misc.dec_cursor() == -1)   // hit start of file, stop here
	      return OK;
	  }

	  //
	  // Move backward to start of this word.
	  //
	  if (skip_chars(cls(), BACKWARD))
	    return OK;
	}

	Misc.inc_cursor();		 // overshot - forward one
      } while(false); // was a label here - finished:
      stop = false;
    }
    return OK;
  }

  /**
   * end_word() - move to the end of the word
   *<br>
   * There is an apparent bug in the 'e' motion of the real vi. At least on the
   * System V Release 3 version for the 80386. Unlike 'b' and 'w', the 'e'
   * motion crosses blank lines. When the real vi crosses a blank line in an
   * 'e' motion, the cursor is placed on the FIRST character of the next
   * non-blank line. The 'E' command, however, works correctly. Since this
   * appears to be a bug, I have not duplicated it here.
   *<br>
   * If stop is TRUE and we are already on the end of a word, move one less.
   * If empty is TRUE stop on an empty line.
   *
   * @return FAIL if end of the file was reached.
   */
  static int end_word(int count, boolean type, boolean stop, boolean empty) {
    int		sclass;	    /* starting class */

    stype = type;
    while (--count >= 0) {
      sclass = cls();
      if (Misc.inc_cursor() == -1)
	return FAIL;

finished_block:
      do {
	//
	// If we're in the middle of a word, we just have to move to the end
	// of it.
	//
	if (cls() == sclass && sclass != 0) {
	  //
	  // Move forward to end of the current word
	  //
	  if (skip_chars(sclass, FORWARD))
	    return FAIL;
	} else if (!stop || sclass == 0) {
	  //
	  // We were at the end of a word. Go to the end of the next word.
	  // First skip white space, if 'empty' is TRUE, stop at empty line.
	  //
	  while (cls() == 0) {
	    ViFPOS fpos = G.curwin.getWCursor();
	    if (empty && fpos.getColumn() == 0
		&& Util.lineempty(fpos.getLine()))
	      break finished_block;
	    if (Misc.inc_cursor() == -1)    // hit end of file, stop here
	      return FAIL;
	  }

	  //
	  // Move forward to the end of this word.
	  //
	  if (skip_chars(cls(), FORWARD))
	    return FAIL;
	}
	Misc.dec_cursor();	// overshot - one char backward
      } while(false); // was a label here - finished:
finished:
      stop = false;		// we move only one word less
    }
    return OK;
  }

  /**
   * bckend_word(count, type) - move back to the end of the word
   *<br>
   * If 'eol' is TRUE, stop at end of line.
   *
   * @return FAIL if start of the file was reached.
   */
  static int bckend_word(int count, boolean type, boolean eol) {
    int		sclass;	    // starting class
    int		i;

    stype = type;
    while (--count >= 0) {
      sclass = cls();
      if ((i = Misc.dec_cursor()) == -1)
	return FAIL;
      if (eol && i == 1)
	return OK;

      //
      // Move backward to before the start of this word.
      //
      if (sclass != 0) {
	while (cls() == sclass)
	  if ((i = Misc.dec_cursor()) == -1 || (eol && i == 1))
	    return OK;
      }

      //
      // Move backward to end of the previous word
      //
      while (cls() == 0) {
	ViFPOS fpos = G.curwin.getWCursor();
	if (fpos.getColumn() == 0 && Util.lineempty(fpos.getLine()))
	  break;
	if ((i = Misc.dec_cursor()) == -1 || (eol && i == 1))
	  return OK;
      }
    }
    return OK;
  }

  /**
   * Skip a row of characters of the same class.
   * @return TRUE when end-of-file reached, FALSE otherwise.
   */
  private static boolean skip_chars(int cclass, int dir) {
    while (cls() == cclass)
      if ((dir == FORWARD ? Misc.inc_cursor() : Misc.dec_cursor()) == -1)
	return true;
    return false;
  }

  /////////////////////////////////////////////////////////////////////
  //
  // regualr expression handling stuff
  //


  // simple cache. Helps "n" and "N" commands
  static RegExp lastRegExp;
  static String lastRegExpPattern = "";
  static boolean lastRegExpIC;

  /** Get a compiled regular expression. Clean up the escaping as needed. */
  static RegExp getRegExp(String pattern, boolean ignoreCase) {
    String cleanPattern = cleanupPattern(pattern);
    // NEEDSWORK: getRegExp: cache re's, LRU?
    if(cleanPattern.equals(lastRegExpPattern) && lastRegExpIC == ignoreCase) {
      return lastRegExp;
    }
    RegExp re = null;
    try {
      int flags = ignoreCase ? RegExp.IGNORE_CASE : 0;
      re = RegExpFactory.create();
      String compilePattern = re.patternType() == RegExp.PATTERN_SIMPLE
                                ? pattern : cleanPattern;
      re.compile(cleanPattern, flags);
      // cache the information
      lastRegExpPattern = cleanPattern;
      lastRegExpIC = ignoreCase;
      lastRegExp = re;
    } catch(RegExpPatternError ex) {
      Msg.emsg(ex.getMessage() + " [" + ex.getIndex() + "]" + pattern);
      //Msg.emsg("Invalid search string: \"" + pattern + "\" " + ex.getMessage());
      re = null;
    }
    return re;
  }

  private static final int ESCAPED_FLAG = 0x10000;
  /**
   * Change metacharacter escaping of input pattern to match
   * the perl5 requirements. Do this because non-standard metachars
   * may be be escaped in patterns according to metaEscape option.
   * <p> (This should be an html table:)
   * <br/>   'c' is escaped      |   'c' is not escaped
   * <br/>        c  --> \c      |         c  --> c
   * <br/>        \c --> c       |         \c --> \c
   * </p><p> Previously, vim did not allow ? to indicate optional atom, so
   * there was some very messy code. Now, since '?' is accepted by vim,
   * just need to deal with converting '=' to '?'. Still messy, but not
   * quite so bad.
   * <br/>   '=' is escaped      |   '=' is not escaped
   * <br/>        c  --> \c      |         c  --> c
   * <br/>        \c --> c       |         \c --> \c
   *
   * </p><p>
   * And finally, either \&lt; or \&gt; are replaced by \b. If there wasn't
   * this last rule, then could just return if p_meta_escape was empty and
   * p_meta_equals was false.
   * </p>
   */
  static String cleanupPattern(String s) {
    String metacharacterEscapes = G.p_meta_escape.getString();
    StringBuffer sb = new StringBuffer();
    boolean isEscaped = false;
    for(int in = 0; in < s.length(); in++) {
      char c = s.charAt(in);
      if( ! isEscaped && c == '\\') {
        isEscaped = true;
        continue;
      }
      
      if((c == '=') && G.p_meta_equals.getBoolean()) {
        // Have an '=' and that char is used to specify an optional atom.
        // Set useEscape if the '=' needs to be escaped to mean optional.
        boolean useEscape = metacharacterEscapes.indexOf("?") >= 0;
        if(isEscaped && useEscape
           || !isEscaped && !useEscape) {
          // the '=' is intened to indicated an optional atom,
          // convert it to a '?'
          c = '?';
        }
      }
      if(metacharacterEscapes.indexOf(c) >= 0) { // metachar gets escaped
        // reverse of what was seen
        if( ! isEscaped) {
          sb.append("\\");
        }
        sb.append(c);
      } else if(isEscaped && (c == '<' || c == '>')) {
        sb.append("\\b");
      } else {
        // pass through what was seen
        if(isEscaped) {
          sb.append("\\");
        }
        sb.append(c);
      }
      isEscaped = false;
    }
    return sb.toString();
  }

  static String top_bot_msg = "search hit TOP, continuing at BOTTOM";
  static String bot_top_msg = "search hit BOTTOM, continuing at TOP";

  // HACK: who uses this?
  private static int search_match_len;

  /**
   * lowest level search function.
   * Search for 'count'th occurrence of 'str' in direction 'dir'.
   * Start at position 'pos' and return the found position in 'pos'.
   *
   * <br>if (options & SEARCH_MSG) == 0 don't give any messages
   * <br>if (options & SEARCH_MSG) == SEARCH_NFMSG dont give 'notfound' messages
   * <br>if (options & SEARCH_MSG) == SEARCH_MSG give all messages
   * <br>if (options & SEARCH_HIS) put search pattern in history
   * <br>if (options & SEARCH_END) return position at end of match
   * <br>if (options & SEARCH_START) accept match at pos itself
   * <br>if (options & SEARCH_KEEP) keep previous search pattern
   *
   * @return OK for success, FAIL for failure.
   */
   //
   // Somehow anchoring at the beginning of the line seems to work fine.
   // Not sure how. The vim code has a "at start of line" flag it passes
   // to the reg exp matcher.
   //
  static int searchit(Window win,      // BUF,    NOT USED
                      ViFPOS pos,      // FPOS,
                      int dir,
                      String pattern,
                      int count,
                      int options,
                      int pat_use,
                      boolean ignoreCase)
  {
    ViFPOS start_pos;
    boolean found;
    boolean at_first_line;
    boolean match_ok;

    int extra_col;
    int lnum;
    int match = 0;
    int matchend = 0;
    int p;
    String wmsg = null;


    RegExp prog = getRegExp(pattern, ignoreCase); // various arguments in vim
    if(prog == null) {
        //Msg.emsg("Invalid search string: " + pattern);
        return FAIL;
    }

    if ((options & SEARCH_START) != 0)
      extra_col = 0;
    else
      extra_col = 1;


    /*
     * find the string
     */
    do	// loop for count
    {
      start_pos = pos.copy(); // remember start pos for detecting no match
      found = false;		// default: not found
      at_first_line = true;	// default: start in first line
      // if (pos.getLine() == 0) // correct lnum for when starting in line 0

      //
      // Start searching in current line, unless searching backwards and
      // we're in column 0.
      //
      if (dir == BACKWARD && start_pos.getColumn() == 0) {
        lnum = pos.getLine() - 1;
        at_first_line = false;
      } else {
        lnum = pos.getLine();
      }

      for (int loop = 0; loop <= 1; ++loop) {   // loop twice if 'wrapscan' set
        for ( ; lnum > 0 && lnum <= G.curwin.getLineCount();
              lnum += dir, at_first_line = false)
        {
          //
          // Look for a match somewhere in the line.
          //
          //////////ptr = ml_get_buf(buf, lnum, FALSE);
          Segment seg = G.curwin.getLineSegment(lnum);
                                                // NEEDSWORK: AT_BOL == TRUE
          if(prog.search(seg.array, seg.offset, seg.count)) {
            match = prog.start(0) - seg.offset; // column index
            matchend = prog.stop(0) - seg.offset;
            int eolColumn = G.curwin.getLineEndOffset(lnum) -
                             G.curwin.getLineStartOffset(lnum) - 1;
            //
            // Forward search in the first line: match should be after
            // the start position. If not, continue at the end of the
            // match (this is vi compatible).
            //
            if (dir == FORWARD && at_first_line) {
              match_ok = true;
              //
              // When *match == NUL the cursor will be put one back
              // afterwards, compare with that position, otherwise
              // "/$" will get stuck on end of line.
              //
              while (((options & SEARCH_END) != 0) ?
                     (matchend - 1  < start_pos.getColumn() + extra_col)
                     : (match - (match == eolColumn ? 1 : 0)
                                    < start_pos.getColumn() + extra_col))
              {
                //
                // If vi-compatible searching, continue at the end
                // of the match, otherwise continue one position
                // forward.
                //
                if(G.p_cpo_search.getBoolean()) {
                  p = matchend;
                  if (match == p && p != eolColumn)
                    ++p;
                } else {
                  p = match;
                  if (p != eolColumn)
                    ++p;
                }
                if (p != eolColumn
                                                // NEEDSWORK: AT_BOL == FALSE
                    && prog.search(seg.array, seg.offset + p, seg.count - p)) {
                  match = prog.start(0) - seg.offset; // column index
                  matchend = prog.stop(0) - seg.offset;
                } else {
                  match_ok = false;
                  break;
                }
              }
              if (!match_ok)
                continue;
            }
            if (dir == BACKWARD)
            {
              //
              // Now, if there are multiple matches on this line,
              // we have to get the last one. Or the last one before
              // the cursor, if we're on that line.
              // When putting the new cursor at the end, compare
              // relative to the end of the match.
              //
              match_ok = false;
              for (;;)
              {
                int match01 = prog.start(0) - seg.offset; // column index
                int matchend01 = prog.stop(0) - seg.offset;
                if (!at_first_line
                    || (((options & SEARCH_END) != 0)
                        ? (matchend01 - 1 + extra_col
                                               <= start_pos.getColumn())
                        : (match01 + extra_col <= start_pos.getColumn())))
                {
                  match_ok = true;
                  match = match01;
                  matchend = matchend01;
                }
                else
                  break;
                //
                // If vi-compatible searching, continue at the end
                // of the match, otherwise continue one position
                // forward.
                //
                if(G.p_cpo_search.getBoolean()) {
                  p = matchend;
                  if (p == match && p != eolColumn)
                    ++p;
                } else {
                  p = match;
                  if (p != eolColumn)
                    ++p;
                }
                if (p == eolColumn
                                                // NEEDSWORK: AT_BOL == FALSE
                    || !prog.search(seg.array, seg.offset + p, seg.count - p)) {
                  break;
                }
              }

              //
              // If there is only a match after the cursor, skip
              // this match.
              //
              if (!match_ok)
                continue;
            }

            int tcol;
            if (((options & SEARCH_END) != 0) && (options & SEARCH_NOOF) == 0)
              tcol = matchend - 1;
            else
              tcol = match;
            pos.setPosition(lnum, tcol);
            found = true;
            break;
          }
          Misc.line_breakcheck();	// stop if ctrl-C typed
          if (false/*got_int*/)
            break;

          if (loop != 0 && lnum == start_pos.getLine())
            break;	    // if second loop, stop where started
        }
        at_first_line = false;

        //
        // stop the search if wrapscan isn't set, after an interrupt and
        // after a match
        ///
        if (!G.p_ws.getBoolean() /*|| got_int*/ || found)
          break;

        //
        // If 'wrapscan' is set we continue at the other end of the file.
        // If 'shortmess' does not contain 's', we give a message.
        // This message is also remembered in keep_msg for when the screen
        // is redrawn. The keep_msg is cleared whenever another message is
        // written.
        //
        if (dir == BACKWARD) {    // start second loop at the other end
          lnum = G.curwin.getLineCount();
          if ((options & SEARCH_MSG) != 0)
	    // defer message until after things are positioned.
            // Msg.wmsg(top_bot_msg/*, true*/);
	    wmsg = top_bot_msg;
        } else {
          lnum = 1;
          if ((options & SEARCH_MSG) != 0)
            // Msg.wmsg(bot_top_msg/*, true*/);
	    wmsg = bot_top_msg;
        }
      }
      if (false/*got_int*/)
        break;
    } while (--count > 0 && found);   // stop after count matches or no match

    if (!found) {	    // did not find it
      if (false/*got_int*/)
        Msg.emsg(Messages.e_interr);
      else if ((options & SEARCH_MSG) == SEARCH_MSG) {
        if (G.p_ws.getBoolean())
          Msg.emsg(Messages.e_patnotf2 + pattern);
        else if (lnum == 0)
          Msg.emsg("search hit TOP without match for: " + pattern);
        else
          Msg.emsg("search hit BOTTOM without match for: " + pattern);
      }
      return FAIL;
    }
    search_match_len = matchend - match;

    if((options & SEARCH_MARK) != 0) {
      MarkOps.setpcmark();
    }
    Misc.gotoLine(G.curwin.getLineNumber(pos.getOffset()), 0);
    G.curwin.setSelect(pos.getOffset(), pos.getOffset() + search_match_len);
    G.curwin.setWSetCurswant(true);
    if(wmsg != null) {
      Msg.wmsg(wmsg/*, true*/);
    }
    return OK;
  }

  ////////////////////////////////////////////////////////////////
  //
  // Stuff from ex_cmds.c
  //
  
  private static int nSubMatch;
  private static int nSubLine = 0;
  
  private static String lastSubstituteArg;

  /**
   * Substitute command
   * @param cev cev's first arg is /pattern/substitution/{flags}
   */
  static void substitute(ColonCommands.ColonEvent cev) {
    // The substitute command doesn't parse arguments,
    // so it has 0 or 1 argument.
    String cmd;
    if(cev.getNArg() == 0) {
      cmd = lastSubstituteArg;
      if(cmd == null) {
	Msg.emsg("No previous substitute argument");
	return;
      }
    } else {
      cmd = cev.getArg(1);
      lastSubstituteArg = cmd;
    }
    String pattern = null;
    RegExp prog = null;
    char[] substitution = null;
    boolean doAll = false; // should be bit flag???
    boolean doPrint = false; // should be bit flag???
    boolean hasEscape = false;
    char delimiter = cmd.charAt(0);
    Segment line;
    int cursorLine = 0; // set to line number of last change
    int sidx = 1; // after delimiter
    
    //
    // pick up the pattern
    //

    int sidx01 = sidx;
    sidx = skip_regexp(cmd, sidx, delimiter, true);
    if(sidx01 == sidx) {
      pattern = lastPattern;
    } else {
      pattern = cmd.substring(sidx01, sidx);
      lastPattern = pattern;
    }
    
    //
    // pick up the substitution string
    //

    sidx++; // first char of substitution
    sidx01 = sidx;
    for( ; sidx < cmd.length(); sidx++) {
      char c = cmd.charAt(sidx);
      if(c == delimiter) {
        break;
      }
      if(c == '\\' && sidx+1 < cmd.length()) {
        ++sidx;
      }
    }
    if(sidx01 == sidx) {
      lastSubstitution = "";
    } else {
      lastSubstitution = cmd.substring(sidx01, sidx);
    }
    substitution = lastSubstitution.toCharArray();
    hasEscape = lastSubstitution.indexOf('\\') != -1
                || lastSubstitution.indexOf('&') != -1;
                // NEEDSWORK: || lastSubstitution.indexOf('~', sidx01) != -1;
    
    //
    // pick up the flags
    //
    
    ++sidx; // move past the delimiter
    for( ; sidx < cmd.length(); sidx++) {
      char c = cmd.charAt(sidx);
      switch(c) {
        case 'g':
          doAll = true;
          break;
        
        case 'p':
          doPrint = true;
          break;
        
        case ' ':
          // silently ignore blanks
          break;
          
        default:
          Msg.emsg("ignoring flag: '" + c + "'");
          break;
      }
    }
    
    //
    // compile regex
    //
    
    prog = getRegExp(pattern, G.p_ic.getBoolean());
    if(prog == null) {
      return;
    }

    int line1 = cev.getLine1();
    int line2 = cev.getLine2();
    StringBuffer sb;
    if(! G.global_busy) {
      nSubLine = 0;
      nSubMatch = 0;
    }
    for(int i = line1; i <= line2; i++) {
      line = G.curwin.getLineSegment(i);
      sb = substitute_line(prog, line, doAll, substitution, hasEscape);
      if(sb != null) {
        G.curwin.replaceString(G.curwin.getLineStartOffset(i),
                               G.curwin.getLineEndOffset(i)-1,
                               sb.toString());
        cursorLine = i;  // keep track of last line changed
	nSubLine++;
	if(doPrint) {
	  ColonCommands.outputPrint(i, 0, 0);
	}
        // System.err.println("sub: " + sb);
      }
    }
    
    if(! G.global_busy) {
      if(cursorLine > 0) {
	Misc.gotoLine(cursorLine, BL_WHITE | BL_FIX);
      }
      if(nSubMatch == 0) {
	Msg.emsg(Messages.e_patnotf2 + pattern);
      } else {
	do_sub_msg();
      }
    }
    
  }

  /**
   * Give message for number of substitutions.
   * Can also be used after a ":global" command.
   * Return TRUE if a message was given.
   */
  static boolean do_sub_msg() {
    if(nSubMatch >= G.p_report.getInteger()) {
      String msg = "" + nSubMatch + " substitution" + Misc.plural(nSubMatch)
		   + " on " + nSubLine + " line" + Misc.plural(nSubLine);
      G.curwin.getStatusDisplay().displayStatusMessage(msg);
      return true;
    }
    return false;
  }

  /**
   * This method preforms the substitution within one line of text.
   * null is returned if there was no match on the line.
   * This is not adapted vim code.
   * <br><b>NEEDSWORK:</b><ul>
   * <li>Handle more flags, not just doAll.
   * </ul>
   * @param prog the compiled regular expression
   * @param line text to check for match and substitue
   * @param doAll if true, substitute all occurences within the line
   * @param subs the substitution string
   * @param hasEscape if true, then escape processing is needed on <i>subs</i>.
   * @return a string buffer with the new line, null is
   * returned if there was no match on the line.
   */
  static StringBuffer substitute_line(RegExp prog,
                                      Segment line,
                                      boolean doAll,
                                      char[] subs,
                                      boolean hasEscape)
  {
    StringBuffer sb = null;
    int offset = line.offset;
    int lookHere = offset;
    int count = line.count -1; // don't count trailing newline
    int endOffset = offset + count;
    int lastMatch = -1;

    while(prog.search(line.array, lookHere, count)) {
      nSubMatch++;
      if(sb == null) { // got a match, make sure there's a buffer
        sb = new StringBuffer();
      }
      // copy characters skipped finding match from input string to output.
      // If no match, then copy to end of line.
      int matchOffset = prog.start(0);
      if(lastMatch == matchOffset) {
        // prevent infinite loops, can happen with match of zero characters
        ++lookHere;
        count = endOffset - offset;
        if(count <= 0) {
          break;
        }
        continue;
      }
      lastMatch = matchOffset;
      sb.append(line.array, offset, matchOffset - offset);
      // copy substitution string, do escaping if needed
      if(hasEscape) {
        translateSubstitution(prog, line, sb, subs);
      } else {
        sb.append(subs);
      }
      // advance past matched characters
      offset = prog.stop(0);
      lookHere = offset;
      count = endOffset - offset;
      if( ! doAll) {
        // only do one substitute
        break;
      }
    }
    if(sb != null && endOffset > offset) {
      // if there was any substitution, copy chars after last match
      sb.append(line.array, offset, endOffset - offset);
    }
    return sb;
  }

  /**
   * Handle substitution where there is escape handling.
   * Append the substitution to string buffer argument.
   * This is not adapted vim code.
   * <br><b>NEEDSWORK:</b><ul>
   * <li>append multiple non-escaped chars at once, see jpython's RegexObject
   * </ul>
   * @param prog the compiled regular expression with match result
   * @param line the line that has the match
   * @param sb append substitution to here
   * @param subs substitution string, contains escape characters
   */
  static void translateSubstitution(RegExp prog,
                                   Segment line,
                                   StringBuffer sb,
                                   char[] subs)
  {
    int i = 0;
    char c;

    for( ; i < subs.length; i++) {
      c = subs[i];
      switch(c) {
        case '&':
          // copy chars that matched
          sb.append(line.array, prog.start(0), prog.length(0));
          break;

        case '\\':
          if(i+1 < subs.length) {
            i++;
            c = subs[i];
            switch(c) {
              case '&':
                sb.append('&');
                break;

              case '0':
              case '1':
              case '2':
              case '3':
              case '4':
              case '5':
              case '6':
              case '7':
              case '8':
              case '9':
                int group = c - '0';
                sb.append(line.array, prog.start(group), prog.length(group));
                break;

              default:
                // escaped a regular character, just append it
                sb.append(c);
                break;
            }
            break; // after handling the escape character
          }

          // last char of string was backslash
          // fall through to append the '\\'

        default:
          sb.append(c);
          break;
      }
    }
  }

  /**
   * global command. first arg is /pattern/some_command_goes_here
   * <br>
   * Only do print for now.
   */

  static void global(ColonCommands.ColonEvent cev) {
    G.global_busy = true;
    try {
      doGlobal(cev);
    }
    finally {
      G.global_busy = false;
    }
    
  }
  
  static void doGlobal(ColonCommands.ColonEvent cev) {
    if(cev.getNArg() != 1) {
      Msg.emsg("global takes an argument (FOR NOW)");
      return;
    }
    int old_lcount = G.curwin.getLineCount();
    nSubLine = 0;
    nSubMatch = 0;
    String cmd = cev.getArg(1);
    String cmdExec;
    String pattern = null;
    RegExp prog = null;
    char delimiter = cmd.charAt(0);
    Segment line;
    int cursorLine = 0; // set to line number of last found line
    int sidx = 1; // after delimiter

    //
    // pick up the pattern
    //

    int sidx01 = sidx;
    sidx = skip_regexp(cmd, sidx, delimiter, true);
    if(sidx01 == sidx) {
      pattern = lastPattern;
    } else {
      pattern = cmd.substring(sidx01, sidx);
      lastPattern = pattern;
    }
    if(lastPattern == null) {
      Msg.emsg(Messages.e_noprevre);
      return;
    }

    //
    // pick up the command
    //

    sidx++; // first char of command
    if(sidx < cmd.length()) {
      cmdExec = cmd.substring(sidx);
    } else {
      cmdExec = "";
    }

    //
    // compile regex
    //

    prog = getRegExp(pattern, G.p_ic.getBoolean());
    if(prog == null) {
      return;
    }

    // figure out what command to execute for all the indicated lines
    
    ActionListener cmdAction = null;
    ColonCommands.ColonEvent cevAction = null;

    if(cmdExec.equals("")) {
      // if no command specified then "p" command
      cmdAction = ColonCommands.ACTION_print;
    } else {
      cevAction = ColonCommands.parseCommand(cmdExec);
      if(cevAction != null) {
	cmdAction = cevAction.getAction();
      }
    }

    int nLine = G.curwin.getLineCount();
      
    // for now special case a few known commands that can be global'd
    // NEEDSWORK: make global two pass, check vim sources. There's no nice
    // way to keep track of the matched lines for the seconde pass. The only
    // generalized thing that would seem to work is to catch the document
    // events for delete and remove any lines from the global list that are
    // deleted.
    
    ViOutputStream result;
    if(cmdAction == ColonCommands.ACTION_print) {
      result = ViManager.createOutputStream(G.curwin,
                                            ViOutputStream.SEARCH, pattern);
    } else {
      result = ViManager.createOutputStream(G.curwin,
                                            ViOutputStream.LINES, pattern);
    }
    
    for(int lnum = 1; lnum <= nLine; lnum++) {
      line = G.curwin.getLineSegment(lnum);
      if(prog.search(line.array, line.offset, line.count)) {
	// if full parse each time command executed,
	// then should move cursor (or equivilent) but.....
	if(cevAction != null) {
	  cevAction.line1 = lnum;
	  cevAction.line2 = lnum;
	}
	if(cmdAction == ColonCommands.ACTION_print) {
	    result.println(lnum, prog.start(0) - line.offset, prog.length(0));
	} else if(cmdAction == ColonCommands.ACTION_substitute) {
	  ColonCommands.executeCommand(cevAction);
	} else if(cmdAction == ColonCommands.ACTION_delete) {
	  OPARG oa = ColonCommands.setupExop(cevAction);
	  oa.op_type = OP_DELETE;
	  Misc.op_delete(oa);
	  // The troublesome command/situation
	  // A line has just been deleted
	  --nLine;
	  --lnum;
	} else if(cmdAction == ColonCommands.ACTION_global) {
	  Msg.emsg("Cannot do :global recursively");
	  return;
	} else {
	  // no command specified, but cursorLine is getting set above
	}
	cursorLine = lnum;  // keep track of last line matched
      }
    }

    if(cursorLine > 0) {
      Misc.gotoLine(cursorLine, BL_WHITE | BL_FIX);
    }
    result.close();
    
    if( ! do_sub_msg()) {
      Misc.msgmore(G.curwin.getLineCount() - old_lcount);
    }
  }


  ////////////////////////////////////////////////////////////////
  //
  // Stuff from regexp.c
  //

  /**
   * REGEXP_INRANGE contains all characters which are always special in a []
   * range after '\'.
   */
  static private String REGEXP_INRANGE = "]^-\\";
  /**
   * REGEXP_ABBR contains all characters which act as abbreviations after '\'.
   * These are:
   *  <ul>
   * <li> \r	- New line (CR).
   * <li> \t	- Tab (TAB).
   * <li> \e	- Escape (ESC).
   * <li> \b	- Backspace (Ctrl('H')).
   */
  static private String REGEXP_ABBR = "rteb";

  /**
   * Skip past regular expression.
   * Stop at end of 'p' of where 'dirc' is found ('/', '?', etc).
   * Take care of characters with a backslash in front of it.
   * Skip strings inside [ and ].
   * @param s string containing regular expression
   * @param sidx first char of regular expression
   * @param dirc char that terminates (and started) regular expression
   * @param magic 
   * @return index of char that terminated regexp, may be past end of string
   */
  static int skip_regexp(String s, int sidx, char dirc, boolean magic) {
    char c = (char)0;
    while(sidx < s.length()) {
      c = s.charAt(sidx);
      if (c == dirc)	// found end of regexp
        break;
      if (c == '[' && magic
          || c == '\\'
             && sidx+1 < s.length()
             && s.charAt(sidx+1) == '['
             && ! magic) {
        sidx = skip_range(s, sidx + 1);
        if(sidx >= s.length()) {
          break;
        }
      }
      else if (c == '\\' && sidx+1 < s.length())
        ++sidx;    // skip next character
      ++sidx;
    }
    return sidx;
  }

  /**
   * Skip over a "[]" range.
   * "p" must point to the character after the '['.
   * The returned pointer is on the matching ']', or the terminating NUL.
   */
  static private int skip_range(String s, int sidx) {
    boolean    cpo_lit;	    /* 'cpoptions' contains 'l' flag */
    char c;

    if(sidx >= s.length()) {
      return sidx;
    }

    //cpo_lit = (!reg_syn && vim_strchr(p_cpo, CPO_LITERAL) != NULL);
    cpo_lit = false;

    c = s.charAt(sidx);

    if (c == '^')	/* Complement of range. */
      ++sidx;
    if (c == ']' || c == '-')
      ++sidx;
    while( sidx < s.length()) {
      c = s.charAt(sidx);
      if(c == ']') {
        break;
      }
      if (c == '-') {
        ++sidx;
        if(sidx < s.length() && (c = s.charAt(sidx)) != ']') {
          ++sidx;
        }
      }
      else if (c == '\\') {
        if(sidx+1 < s.length()) {
          char c2 = s.charAt(sidx+1);
          if(Util.vim_strchr(REGEXP_INRANGE, c2) != null
             || (!cpo_lit && Util.vim_strchr(REGEXP_ABBR, c2) != null)) {
            sidx += 2;
          }
        }
      }
      else if (c == '[')
      {
        MutableInt mi = new MutableInt(sidx);
        if (skip_class_name(s, mi) == null) {
          ++sidx; /* It was not a class name */
        } else {
          sidx = mi.getValue();
        }
      }
      else
        ++sidx;
    }

    return sidx;
  }

  static boolean findsent(int dir, int count) {
    FPOS pos, tpos;
    int c;
    int startlnum;
    boolean noskip = false;
    boolean cpo_J;
    boolean found_dot;

    pos = (FPOS) G.curwin.getWCursor().copy();

    while (count-- > 0) {

found:
      do {
        if (Misc.gchar_pos(pos) == '\n') {
          do {
            if (Misc.inclDeclV7(pos, dir) == -1)
              break;
          } while (Misc.gchar_pos(pos) == '\n');

          if (dir == FORWARD)
            break found;
        }
        else if (dir == FORWARD && pos.getColumn() == 0 &&
          startPS(pos.getLine(), NUL, false)) {
          if (pos.getLine() == G.curwin.getLineCount())
            return false;
          pos.setPosition(pos.getLine() + 1, 0);
          break found;
        }
        else if (dir == BACKWARD)
          Misc.decl(pos);

        // go back to previous non-blank character
        found_dot = false;
        while ((c = Misc.gchar_pos(pos)) == ' ' || c == '\t' ||
          (dir == BACKWARD && Util.vim_strchr(".!?)]\"'", c) != null)) {
          if (Util.vim_strchr(".!?", c) != null) {
            // Only skip over a '.', '!' and '?' once.
            if (found_dot)
              break;
            found_dot = true;
          }
          if (Misc.decl(pos) == -1)
            break;
          // when going forward: Stop in front of empty line
          if (Util.lineempty(pos.getLine()) && dir == FORWARD) {
            Misc.inclV7(pos);
            break found;
          }
        }

        // remember the line where the search started
        startlnum = pos.getLine();
        cpo_J = G.p_cpo_j.value;

        for (;;) {
          c = Misc.gchar_pos(pos);
          if (c == '\n' ||
            (pos.getColumn() == 0 && startPS(pos.getLine(), NUL, false))) {
            if (dir == BACKWARD && pos.getLine() != startlnum)
              pos.setPosition(pos.getLine() + 1, 0);
            break;
          }
          if (c == '.' || c == '!' || c == '?') {
            tpos = (FPOS) pos.copy();
            do
              if ((c = Misc.inc(tpos)) == -1)
                break;
            while (Util.vim_strchr(")]\"'", c = Misc.gchar_pos(tpos)) != null);

            if (c == -1 || (!cpo_J && (c == ' ' || c == '\t')) || c == '\n'
              || (cpo_J && (c == ' ' && Misc.inc(tpos) >= 0
              && Misc.gchar_pos(tpos) == ' '))) {
              pos = tpos;
              if (Misc.gchar_pos(pos) == '\n') // skip '\n' at EOL
                Misc.inc(pos);
              break;
            }
          }
          if (Misc.inclDeclV7(pos, dir) == -1) {
            if (count > 0)
              return false;
            noskip = true;
            break;
          }
        }
      } while (false);

      while (!noskip && ((c = Misc.gchar_pos(pos)) == ' ' || c== '\t'))
        if (Misc.inclV7(pos) == -1) break;
    }
    MarkOps.setpcmark();
    G.curwin.setCaretPosition(pos.getOffset());
    return true;
  }

  static boolean findpar(CMDARG oap, int dir, int count, int what,
    boolean both) {

    int curr = G.curwin.getWCursor().getLine();

    while (count-- > 0) {
      boolean did_skip = false; //TRUE after separating lines have been skipped 
      boolean first = true;     // TRUE on first line 
      do {
        if (!Util.lineempty(curr))
          did_skip = true;

        if (!first && did_skip && startPS(curr, what, both))
          break;

        if ((curr += dir) < 1 || curr > G.curwin.getLineCount()) {
          if (count > 0)
            return false;
          curr -= dir;
          break;
        }
        first = false;
      } while (true);
    }

    MarkOps.setpcmark();

    if (both && !Util.lineempty(curr) && Util.ml_get(curr).charAt(0) == '}')
      ++curr;

    int offset = 0;

    if (curr == G.curwin.getLineCount()) {
      offset = Util.lineLength(curr);
      if (offset > 0) {
        offset--;
        oap.oap.inclusive = true;
      }
    }

    G.curwin.setCaretPosition(curr, offset);

    return true;
  }

  //
  // Character class stuff from vim's regexp.c
  //

  /**
   * character class namedata.
   */
  static class CCNameData {
    String name;
    CCCheck checkFunc;
    CCNameData(String name, CCCheck checkFunc) {
      this.name = name;
      this.checkFunc = checkFunc;
    }
  }

  /**
   * character class check function
   */
  interface CCCheck {
    boolean doCheck(int c);
  }

  /* *****************************************************************
#define t(n, func) { sizeof(n) - 1, func, n }
      static const namedata_t class_names[] =
      {
          t("alnum:]", isalnum),		t("alpha:]", isalpha),
          t("blank:]", my_isblank),     	t("cntrl:]", iscntrl),
          t("digit:]", isdigit),		t("graph:]", isgraph),
          t("lower:]", islower),		t("print:]", vim_isprintc),
          t("punct:]", ispunct),		t("space:]", vim_isspace),
          t("upper:]", isupper),		t("xdigit:]", isxdigit),
          t("tab:]",   my_istab),		t("return:]", my_isreturn),
          t("backspace:]", my_isbspace),	t("escape:]", my_isesc)
      };
#undef t
  ***********************************************************************/

  /**
   * Check for a character class name.  "pp" is at the '['.
   * If not: NULL is returned; If so, a function of the sort "is*"
   * is returned and
   * the name is skipped.
   * @param s
   * @param mi on entry hold start string index;
   * on return string index to skip name (if any)
   * @return function used to check, null if not a char class
   */
  static CCCheck skip_class_name(String s, MutableInt mi) {
    int sidx = mi.getValue();
    // Assume there is not a class name
    return null;

    /* ******************************************************************
    const namedata_t *np;

    if ((*pp)[1] != ':')
        return NULL;
    for (   np = class_names;
            np < class_names + sizeof(class_names) / sizeof(*class_names);
            np++)
        if (STRNCMP(*pp + 2, np.name, np.len) == 0)
        {
            *pp += np.len + 2;
            return np.func;
        }
    return NULL;
    *********************************************************************/
  }

    /* include: TRUE == include white space */
    /* type:  'p' for paragraph, 'S' for section */
  static int current_par(OPARG oap, int count, boolean include, int type) {
    int /*linenr_t*/ start_lnum;
    int /*linenr_t*/ end_lnum;
    int white_in_front;
    int dir;
    int start_is_white;
    int prev_start_is_white;
    int retval = OK;
    boolean do_white = false;
    int t;
    int i;

    if (type == 'S')	    /* not implemented yet */
        return FAIL;

    start_lnum = G.curwin.getWCursor().getLine();

    /*
     * When visual area is more than one line: extend it.
     */
    if (G.VIsual_active && start_lnum != G.VIsual.getLine())
    {
extend:
        if (start_lnum < G.VIsual.getLine())
            dir = BACKWARD;
        else
            dir = FORWARD;
        for (i = count; --i >= 0; )
        {
            if (start_lnum ==
                    (dir == BACKWARD ? 1 : G.curwin.getLineCount()/*G.curbuf.b_ml.ml_line_count*/))
            {
                retval = FAIL;
                break;
            }

            prev_start_is_white = -1;
            for (t = 0; t < 2; ++t)
            {
                start_lnum += dir;
                start_is_white = linewhite(start_lnum) ? 1 : 0;
                if (prev_start_is_white == start_is_white)
                {
                    start_lnum -= dir;
                    break;
                }
                for (;;)
                {
                    if (start_lnum == (dir == BACKWARD
                                ? 1 : G.curwin.getLineCount() /*G.curbuf.b_ml.ml_line_count*/))
                        break;
                    if (start_is_white != (linewhite(start_lnum + dir) ? 1 : 0)
                            || (start_is_white <= 0
                                && startPS(start_lnum + (dir > 0
                                        ? 1 : 0), 0, false)))
                        break;
                    start_lnum += dir;
                }
                if (!include)
                    break;
                if (start_lnum == (dir == BACKWARD
                            ? 1 : G.curwin.getLineCount() /*G.curbuf.b_ml.ml_line_count*/))
                    break;
                prev_start_is_white = start_is_white;
            }
        }
        G.curwin.setCaretPosition(start_lnum, 0);
        return retval;
    }

    /*
     * First move back to the start_lnum of the paragraph or white lines
     */
    white_in_front = linewhite(start_lnum) ? 1 : 0;
    while (start_lnum > 1)
    {
        if (white_in_front > 0)	    /* stop at first white line */
        {
            if (!linewhite(start_lnum - 1))
                break;
        }
        else		/* stop at first non-white line of start of paragraph */
        {
            if (linewhite(start_lnum - 1) || startPS(start_lnum, 0, false))
                break;
        }
        --start_lnum;
    }

    /*
     * Move past the end of any white lines.
     */
    end_lnum = start_lnum;
    while (linewhite(end_lnum) && end_lnum < G.curwin.getLineCount() /*curbuf.b_ml.ml_line_count*/)
        ++end_lnum;

    --end_lnum;
    i = count;
    if (!include && white_in_front > 0)
        --i;
    while (i-- > 0)
    {
        if (end_lnum == G.curwin.getLineCount()/*curbuf.b_ml.ml_line_count*/)
            return FAIL;

        if (!include)
            do_white = linewhite(end_lnum + 1);

        if (include || !do_white)
        {
            ++end_lnum;
            /*
             * skip to end of paragraph
             */
            while (end_lnum < G.curwin.getLineCount()//curbuf.b_ml.ml_line_count
                    && !linewhite(end_lnum + 1)
                    && !startPS(end_lnum + 1, 0, false))
                ++end_lnum;
        }

        if (i == 0 && white_in_front > 0)
            break;

        /*
         * skip to end of white lines after paragraph
         */
        if (include || do_white)
            while (end_lnum < G.curwin.getLineCount()//curbuf.b_ml.ml_line_count
                    && linewhite(end_lnum + 1))
                ++end_lnum;
    }

    /*
     * If there are no empty lines at the end, try to find some empty lines at
     * the start (unless that has been done already).
     */
    if (white_in_front <= 0 && !linewhite(end_lnum) && include)
        while (start_lnum > 1 && linewhite(start_lnum - 1))
            --start_lnum;

    if (G.VIsual_active)
    {
        /* Problem: when doing "Vipipip" nothing happens in a single white
         * line, we get stuck there.  Trap this here. */
//TODO: FIXME_VISUAL goto extend
//        if (VIsual_mode == 'V' && start_lnum == curwin.w_cursor.lnum)
//            goto extend;
        G.VIsual.setPosition(start_lnum, 0); //G.VIsual.lnum = start_lnum;
        G.VIsual_mode = 'V';
        Normal.update_curbuf(NOT_VALID);	/* update the inversion */
        Misc.showmode();
    }
    else
    {
        oap.start.setPosition(start_lnum, 0); //was: oap.start.lnum = start_lnum;
        oap.motion_type = MLINE;
    }
    G.curwin.setCaretPosition(end_lnum, 0);

    return OK;
}
    static boolean linewhite(int /*linenr_t*/ lnum) {
        Segment seg = Util.ml_get(lnum);
        int idx = Misc.skipwhite(seg);
        return seg.array[seg.offset + idx] == '\n';
    }
    
  /*
   * startPS: return TRUE if line 'lnum' is the start of a section or paragraph.
   * If 'para' is '{' or '}' only check for sections.
   * If 'both' is TRUE also stop at '}'
   */
  static boolean startPS(int /*linenr_t*/lnum, int para, boolean both) {
    Segment seg = Util.ml_get(lnum);
    // if seg.count == 1, then only a \n, ie empty line
    char s = seg.count > 1 ? seg.array[seg.offset] : 0;
    // '\f' is formfeed, oh well, it doesn't hurt to be here
    if (s == para || s == '\f' || (both && s == '}'))
      return true;
//    if (s == '.' && (inmacro(p_sections, s + 1) ||
//            (!para && inmacro(p_para, s + 1))))
//        return true;
    return false;
  }
}
