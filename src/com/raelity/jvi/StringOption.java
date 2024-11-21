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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;

public class StringOption extends Option {
  private Validator validator;
  
  StringOption(String key, String defaultValue) {
    this(key, defaultValue, null);
  }
  
  StringOption(String key, String defaultValue, Validator validator) {
    super(key, defaultValue);
    if(validator == null) {
      // provide the default validator
      validator = new Validator() {
        public void validate(String val) throws PropertyVetoException {
          if(val == null) {
            throw new PropertyVetoException(
                        "null is not a valid string option",
                        new PropertyChangeEvent(opt, opt.getName(),
                                                opt.getString(), val));
          }
        }
      };
    }
    validator.opt = this;
    this.validator = validator;
  }

  public final String getString() {
    return stringValue;
  }

  public final char charAt(int i) {
    return stringValue.charAt(i);
  }

  /**
   * Set the value of the parameter.
   */
  public void setString(String newValue) {
    String oldValue = stringValue;
    stringValue = newValue;
    propogate();
    Options.getOptions().pcs.firePropertyChange(name, oldValue, newValue);
  }

  /**
   * Set the value as a string.
   */
  public void setValue(String newValue) {
    setString(newValue);
  }
  
  /**
   * Validate the setting value.
   */
  public void validate(String val) throws PropertyVetoException {
    validator.validate(val);
  }
  
  public static abstract class Validator {
    StringOption opt;
    
    public abstract void validate(String val) throws PropertyVetoException;
  }
}
