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

package com.raelity.text;

import java.util.*;

/** Some convenient functions when working with text. This class is not
 * meant to be instantiated, its methods are all static.
 */
class TextUtil {

  /** Split a string into a vector of words. White space is used
   * for the delimeters, " \t\n\r".
   */
  public Vector split(String s) {
    Vector word = new Vector();
    StringTokenizer parse = new StringTokenizer(s);
    while(parse.hasMoreElements()) {
      word.addElement(parse.nextElement());
    }
    return word;
  }

  /** Split a string into a vector of words, using <i>separarators</i>
   * to delineate the words.
   */
  public Vector split(String s, String separators) {
    Vector word = new Vector();
    StringTokenizer parse = new StringTokenizer(s, separators);
    while(parse.hasMoreElements()) {
      word.addElement(parse.nextElement());
    }
    return word;
  }
}
