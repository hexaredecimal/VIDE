package com.raelity.jvi;

/**
 * Simple class so there is some way to change vi options in the IDE.
 * The bean is its own beanInfo.
 */
public class OptionsBean {
    
    static public class General extends OptionsBeanBase {
	public General() {
	    super(General.class, "jVi Options and Configuration",
		  Options.generalList);
        }
    }
    
    static public class Search extends OptionsBeanBase {
	
	public Search() {
	    super(Search.class, "Search Options", Options.searchList);
	}
    }
    
    static public class Misc extends OptionsBeanBase {
	
	public Misc() {
	    super(Misc.class, "Miscelaneous Options", Options.miscList);
	}
    }
    
    static public class CursorWrap extends OptionsBeanBase {
	
	public CursorWrap() {
	    super(CursorWrap.class, "Cursor Wrap Options",
                  Options.cursorWrapList);
	}
    }
    
    static public class Debug extends OptionsBeanBase {
	
	public Debug() {
	    super(Debug.class, "Debug Output", Options.debugList);
	}
    }
}
