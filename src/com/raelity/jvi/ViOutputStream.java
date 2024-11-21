
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

/**
 * When jVi wants to output multi-line information, for example lines
 * matching a search or result of some command execution, the output
 * is sent to a ViOutputStream.
 */
public interface ViOutputStream {
  /** Indicates that the output stream is for search results */
  public static final String SEARCH = "Search";
  /** Indicates that the output stream is random lines from a file */
  public static final String LINES = "Text";
  /** Indicates that the output stream is command state information, reuse */
  public static final String OUTPUT = "Output";
  
  /**
   * Add a message to this output stream. This argument information 
   * could be used
   * to identify a match within a line.
   * @param line line number corresponding to this message
   * @param offset offset within line
   * @param length length, from offset, in the line
   */
  public void println(int line, int offset, int length);
  
  /**
   * Add a text line to the output stream.
   */
  public void println(String s);
  
  /**
   * Done with the stream.
   */
  public void close();
}
