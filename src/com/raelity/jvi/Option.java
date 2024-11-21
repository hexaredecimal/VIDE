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

import java.awt.Color;
import java.beans.PropertyVetoException;

public abstract class Option {
    protected String name;
    protected String displayName;
    protected String stringValue;
    protected String desc;
    protected String defaultValue;
    protected boolean fExpert;
    protected boolean fHidden;
    
    protected boolean fPropogate; // used in logic, not part of option type
    
    public Option(String key, String defaultValue) {
	name = key;
	this.defaultValue = defaultValue;
	String initialValue = Options.getPrefs().get(key, defaultValue);
	fExpert = false;
        fHidden = false;

	fPropogate = false;
	setValue(initialValue);
	fPropogate = true;
    }

    public abstract void setValue(String value);

    public String getValue() {
	return stringValue;
    }

    public String getName() {
	return name;
    }
    
    public String getDefault() {
	return defaultValue;
    }
    
    public String getDesc() {
	return desc;
    }

    public String getDisplayName() {
	if(displayName != null) {
	    return displayName;
	} else {
	    return name;
	}
    }

    public boolean isExpert() {
	return fExpert;
    }

    public boolean isHidden() {
	return fHidden;
    }
    
    public void setHidden(boolean f) {
        fHidden = f;
    }
    
    public void setExpert(boolean f) {
        fExpert = f;
    }

    /**
     * The preferences data base has changed, stay in sync.
     * Do not propogate change back to data base.
     */
    void preferenceChange(String newValue) {
	fPropogate = false;
        try {
	    //System.err.println("preferenceChange " + name + ": " + newValue);
            setValue(newValue);
        } finally {
	    fPropogate = true;
        }
    }

    protected void propogate() {
	if(fPropogate) {
	    Options.prefs.put(name, stringValue);
	}
    }
    
    public int getInteger() {
        throw new ClassCastException(this.getClass().getSimpleName()
                                     + " is not an IntegerOption");
    }
    
    public boolean getBoolean() {
        throw new ClassCastException(this.getClass().getSimpleName()
                                     + " is not a BooleanOption");
    }
    
    public String getString() {
        throw new ClassCastException(this.getClass().getSimpleName()
                                     + " is not a StringOption");
    }
    
    public Color getColor() {
        throw new ClassCastException(this.getClass().getSimpleName()
                                     + " is not a ColorOption");
    }
    
    public void validate(int val) throws PropertyVetoException {
        throw new ClassCastException(this.getClass().getSimpleName()
                                     + " is not an IntegerOption");
    }
    
    public void validate(boolean val) throws PropertyVetoException {
        throw new ClassCastException(this.getClass().getSimpleName()
                                     + " is not a BooleanOption");
    }
    
    public void validate(String val) throws PropertyVetoException {
        throw new ClassCastException(this.getClass().getSimpleName()
                                     + " is not a StringOption");
    }
    
    public void validate(Color val) throws PropertyVetoException {
        throw new ClassCastException(this.getClass().getSimpleName()
                                     + " is not a ColorOption");
    }
    
    public void validate(Object val) throws PropertyVetoException {
        if(val instanceof String)
            validate((String)val);
        else if(val instanceof Color)
            validate((Color)val);
        else if(val instanceof Boolean)
            validate(((Boolean)val).booleanValue());
        else if(val instanceof Integer)
            validate(((Integer)val).intValue());
        else 
            throw new ClassCastException(val.getClass().getSimpleName()
                                    + " is not int, boolean, Color or String");
    }
    
    //////////////////////////////////////////////////////////////////////
    //
    // ColorOption
    //
    
    public static class ColorOption extends Option {
        private Validator validator;
        Color value;
        
        public ColorOption(String key, Color defaultValue) {
            this(key, defaultValue, null);
        }
        
        public ColorOption(String key, Color defaultValue, Validator validator) {
            super(key, xformToString(defaultValue));
            if(validator == null) {
                // The default validation accepts everything
                validator = new Validator() {
                    public void validate(Color val) throws PropertyVetoException {
                    }
                };
            }
            this.validator = validator;
        }

        public final Color getColor() {
            return value;
        }
        
        public static String xformToString(Color c) {
            return String.format("0x%x", c.getRGB() & 0xffffff);
        }

        /**
        * Set the value of the parameter.
        */
        public void setColor(Color newValue) {
            Color oldValue = value;
            value = newValue;
            stringValue = xformToString(value);
            propogate();
            Options.getOptions().pcs.firePropertyChange(name, oldValue, newValue);
        }

        /**
        * Set the value as a string.
        */
        public void setValue(String newValue) throws IllegalArgumentException {
            setColor(Color.decode(newValue));
        }
        
        public void validate(Color val) throws PropertyVetoException {
            validator.validate(val);
        }
        
        public static abstract class Validator {
            protected ColorOption opt;
            
            public abstract void validate(Color val) throws PropertyVetoException;
        }
    }
}
