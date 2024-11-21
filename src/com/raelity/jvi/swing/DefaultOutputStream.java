
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

import com.raelity.jvi.ViTextView;
import com.raelity.jvi.ViOutputStream;
import com.raelity.jvi.OutputStreamAdaptor;

public class DefaultOutputStream extends OutputStreamAdaptor {
  String type;
  String info;
  ViTextView tv;

  public DefaultOutputStream(ViTextView tv, String type, String info) {
    this.type = type;
    this.info = info;
    this.tv = tv;
    
    String fName = tv != null ? tv.getDisplayFileName() : "no-file";
    System.err.println("ViOutputStream: type: " + type
                       + ", info: " + info + ", " + fName);
  }

  public void println(int line, int offset, int length) {
    System.err.println("ViOutputStream: " + type + ", " + info + ": "
                       + "line: " + line + ", "
                       + "offset: " + offset + ", "
                       + "length: " + length
		       );
  }

  public void println(String s) {
    System.err.println("ViOutputStream: " + s);
  }

  public void close() {
  }
}