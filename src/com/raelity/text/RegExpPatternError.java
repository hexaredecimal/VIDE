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

import java.io.*;

/**
 * This exception occurs when a regular expression pattern
 * has an error and will not compile. It is thrown by the
 * RegExp adaptor if it catches an exception from the
 * underlying implementation. Unlike most exceptions it has a field
 * of type Throwable. This field is the implementation dependant
 * exception. The printStackTrace methods are overloaded to
 * print the machine dependant exception, since it may provide
 * detailed information about the problem.
 * Note that this class is derived from Error, if a regular expression
 * is built from user input this should be caught.
 * @see RegExp#compile(java.lang.String)
 */
public class RegExpPatternError extends Error {
  // Everything's handled by the new Java 1.4 throwable architeture
  // Simply delegate the constructors.
  
  private int index;

  RegExpPatternError(String msg, int idx, Throwable cause) {
    super(msg, cause);
    index = idx;
  }
  
  public int getIndex() {
      return index;
  }
}
