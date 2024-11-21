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

public class Msg {
  public static void smsg(String msg) {
    // VV_STATUSMSG
    G.curwin.getStatusDisplay().displayStatusMessage(msg);
  }

  public static void wmsg(String msg) {
    // VV_STATUSMSG
    // NEEDSWORK: wmsg: implement a warning display method
    G.curwin.getStatusDisplay().displayErrorMessage(msg);
  }

  public static void emsg(String msg) {
    // VV_ERRMSG; HLF_E highlight
    G.curwin.getStatusDisplay().displayErrorMessage(msg);
    GetChar.flush_buffers(false);
  }
  
  public static void clearMsg() {
    G.curwin.getStatusDisplay().clearMessage();
  }
}
