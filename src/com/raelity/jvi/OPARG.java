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
 * Arguments for operators.
 */
public class OPARG
{
    int	    op_type;		/* current pending operator type */
    int	    regname;		/* register to use for the operator */
    int	    motion_type;	/* type of the current cursor motion */
    boolean inclusive;		/* TRUE if char motion is inclusive (only
				   valid when motion_type is MCHAR */
    boolean end_adjusted;	/* backuped b_op_end one char (only used by
				   do_format()) */
    FPOS    start;		/* start of the operator */
    FPOS    end;		/* end of the operator */
    int     line_count;		/* number of lines from op_start to op_end
				   (inclusive) */
    boolean empty;		/* op_start and op_end the same (only used by
				   do_change()) */
    boolean is_VIsual;		/* operator on Visual area */
    boolean block_mode;		/* current operator is Visual block mode */
    int	    start_vcol;		/* start col for block mode operator */
    int	    end_vcol;		/* end col for block mode operator */

    public void clearop() {
      op_type = Constants.OP_NOP;
      regname = 0;
    }

    public String toString() {

      return                "op_type: " + op_type
			 + " regname: " + regname
			 + " motion_type: " + motion_type
			 + " inclusive: " + inclusive
			 + " end_adjusted: " + end_adjusted
			 + "\n start: {" + start + "}"
			 + " end: {" + end + "}"
			 + " line_count: " + line_count
			 + " empty: " + empty
			 + " is_VIsual: " + is_VIsual
			 + " block_mode: " + block_mode
			 + " start_vcol: " + start_vcol
			 + " end_vcol: " + end_vcol
			 ;
    }
}
