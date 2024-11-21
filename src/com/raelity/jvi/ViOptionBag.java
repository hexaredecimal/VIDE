/*
 * ViOptionBag.java
 *
 * Created on March 5, 2007, 4:40 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.raelity.jvi;

/**
 * Interface for classes that have vim options, i.e. ViTextView and Buffer
 * aka {@link G#curwin} and {@link G#curbuf}; these are per document and per
 * window options.
 * Multiple views/windows
 * of the same document share the <code>Buffer</code> variables;
 * for example, if you do ":set sw=8" on a file,
 * any open windows into that file all have the same shiftwidth.
 * Global options are <code>static</code> and are found in <code>G</code>.
 *<p>
 * The values are initialized from global options and change with the set
 * command. The options are referenced by name through curwin and curbuf
 * and notification is sent with {@link #viOptionSet} when one of
 * its options changes. These options may interact with paltform features,
 * and {@link #activateOptions} is invoked when a window becomes active.
 * For example, on the NetBeans platform the 'number' option is a global,
 * but jVi treats it like a window option. Look at
 * {@link org.netbeans.modules.jvi.NbTextView} to see how the
 * option w_p_nu is handled; and {@link org.netbeans.modules.jvi.NbBuffer}
 * shows some handling of buffer options.
 * </p>
 *
 * @author erra
 */
public interface ViOptionBag {
    
    /** The set command used to change an option */
    public void viOptionSet(ViTextView tv, String name);
    
    /** This is invoked when switchto bag's associated editor. */
    public void activateOptions(ViTextView tv);
}
