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

import javax.swing.JEditorPane;

public class G {
  static public final String metaEscapeDefault = "()|+?{";
  static public final String metaEscapeAll = "()|+?{";



  /**
   * Set the current editor.
   */
  static void switchTo(ViTextView textView, Buffer buf) {
    G.curwin = textView;
    G.curbuf = buf;
  }

  final static public void setModMask(int mod_mask) {
    G.mod_mask = mod_mask;
  }

  final static public int getModMask() {
    return mod_mask;
  }

  static int mod_mask;

  public static ViTextView curwin;
  
  static Buffer curbuf; // per file options. as int,boolean
        //public static BooleanOption b_p_et;	// expandtabs, per file
        //public static IntegerOption b_p_sw;	// per file
        //public static IntegerOption b_p_ts;	// per file

  /* ******************************************************************
  static boolean asyncCaretMovement = false;

  public final static boolean getAsyncCaretMovement() {
    return asyncCaretMovement;
  }

  public final static void setAsyncCaretMovement(boolean flag) {
    asyncCaretMovement = flag;
  }
  ********************************************************************/

  static int no_mapping;
  static int allow_keys;
  static boolean finish_op;
  static int State;
  static int restart_edit;
  public static boolean Recording;
  static boolean Exec_reg;      // true when executing a register
  
  // for the global command
  static boolean global_busy;
  ViOutputStream global_output;

  static boolean clear_cmdline = false;
  static int old_indent = 0;
  static int ai_col = 0;

  // static String p_ww = "bshl<>[]"; // b,s
  // options for whichwrap
  static BooleanOption p_ww_bs;
  static BooleanOption p_ww_sp;
  static BooleanOption p_ww_h;
  static BooleanOption p_ww_l;
  static BooleanOption p_ww_larrow;
  static BooleanOption p_ww_rarrow;
  static BooleanOption p_ww_tilde;

  static BooleanOption p_cb;  // clipboard, treat as boolean for 'unnamed'
  static BooleanOption p_sr;   // shiftround
  static BooleanOption p_notsol;  // startofline
  static BooleanOption p_cpo_w; // change word blanks
  static BooleanOption p_cpo_search; // continue search from end of match
  static BooleanOption p_cpo_j; // sentence search two spaces
  static BooleanOption p_js;   // joinspaces, 2 spaces after .?!
  static BooleanOption p_to;   // tildeop
  static BooleanOption p_ws;   // wrapscan
  
  public static BooleanOption useFrame;	// use JFrame for command entry

  public static BooleanOption p_ic;   // ignorecase

  public static BooleanOption p_meta_equals;  // true use '=' instead of '?'
  public static StringOption p_meta_escape;   // metacharacters escaped
  public static BooleanOption p_incr_search;
  public static BooleanOption p_highlight_search;

  public static BooleanOption p_smd; // showmode edit/command mode in display
  public static BooleanOption p_sc; // showcmd

  public static IntegerOption p_report;
  public static IntegerOption p_bs ;   // backspace over start of insert, and more
  public static IntegerOption p_so;   // scrolloff, lines before/after current

  static String p_km = "";      // keymodel
  
  public static StringOption p_sel; // 'selection'
  public static StringOption p_slm; // 'selectionmode'
  
  static String p_fp;           // formatprg
  static String p_cpo = "aABceFs"; // cpoptions
  
  public static BooleanOption readOnlyHack;
  public static BooleanOption isClassicUndo;
  public static BooleanOption dbgEditorActivation;

  public static BooleanOption dbgKeyStrokes;
  public static IntegerOption dbgFlow;  // 1-switch
                                        // 3-op/exec
                                        // 5-not supported
                                        // 7-not implemented
                                        // 9-xop

  public static boolean debugPrint = false;
  public static boolean debugOpPrint = false;
  public static boolean switchDebug = false;
  
  public static FPOS VIsual; // start position of active Visual selection
  public static boolean VIsual_active; // whether Visual mode is active
  public static boolean VIsual_select; // whether Select mode is active
  public static boolean VIsual_reselect;
  // VISUAL repaint hack when in colon command from visual mode
  public static boolean drawSavedVisualBounds = false;
  // end VISUAL repaint hack when in colon command from visual mode

				// whether to restart the selection after a
				// Select mode mapping or menu
  public static int VIsual_mode; // type of Visual mode
  
  public static boolean redo_VIsual_busy = false;
      /* The visual area is remembered for redo */
}
