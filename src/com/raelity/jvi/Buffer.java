/*
 * Buffer.java
 *
 * Created on March 5, 2007, 11:23 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.raelity.jvi;

/**
 * Buffer: structure that holds information about one file, primarily
 * per file options.
 *
 * Several windows can share a single Buffer.
 *
 * @author erra
 */
public class Buffer implements ViOptionBag {
    
    /** Creates a new instance of Buffer, initialize values from Options. */
    public Buffer() {
        initOptions();
    }
    
    protected void initOptions() {
        b_p_ts = Options.getOption(Options.tabStop).getInteger();
        b_p_sw = Options.getOption(Options.shiftWidth).getInteger();
        b_p_et = Options.getOption(Options.expandTabs).getBoolean();
    }

    public void viOptionSet(ViTextView tv, String name) {
    }
    
    /** switchto */
    public void activateOptions(ViTextView tv) {
    }
    
    //
    // Declare the variables referenced as part of a ViOptionBag
    //
    
    public int b_p_ts;     // tab stop
    public int b_p_sw;     // shiftw width
    public boolean b_p_et;     // expand tabs
    
    //
    // Other per buffer variables
    //
    
    /* Save the current VIsual area for '< and '> marks, and "gv" */
    public ViMark b_visual_start;
    public ViMark b_visual_end;
    public int b_visual_mode;
    public String b_p_mps; // used in nv_object
}
