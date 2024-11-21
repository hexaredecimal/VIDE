/*--- formatted by Jindent 2.1, (www.c-lab.de/~jindent) ---*/
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

import java.util.Vector;
import java.lang.reflect.*;

//
// NEEDSWORK: stevesoft fails when escape character is set
//

/**
 * Factory class for vending <i>RegExp</i> instances.
 * Most of the work is finding a regular expression implementation
 * to use. Note: RegExpFactory has no public constructor.
 * @see RegExp
 */
public class RegExpFactory {
  protected static Vector<String> reImp = new Vector<String>(5);

  static {
    reImp.add("com.raelity.text.RegExpJava");
    reImp.add("com.raelity.text.RegExpStevesoft");
    reImp.add("com.raelity.text.RegExpOroinc");
    reImp.add("com.raelity.text.RegExpGNU");
  };

  /**
   * The <i>reClass</i> variable holds the <b>Class</b> which
   * is vended.
   */
  protected static Class    reClass = null;

  /**
   * The name of the class that is being vended.
   */
  protected static String   reClassName = null;

  /**
   * The class name of the regular expression handler
   * that is being adapted by the vended class. This
   * is the empty string for direct implementations.
   */
  protected static String   reAdapted = null;

  /**
   * Name of package being used.
   */
  protected static String reDisplayName = null;

  /**
   * Constructor declaration
   *
   *
   * @see
   */
  private RegExpFactory() {}

  /**
   * Create a new instance of a regular expresion handler.
   * <br>A NoClassDefFoundError is thrown if the factory
   * is inoporable.
   * @return An instance of a regular expression handler.
   * @see RegExp
   */
  public static RegExp create() {
    return doCreate(false, "");
  }

  /**
   * Create a new instance of a regular expresion handler
   * and compile <i>pattern</i> for the regular expression.
   * <br>A NoClassDefFoundError is thrown if the factory
   * is inoporable.
   * @return An instance of a regular expression handler.
   * @see RegExp
   */
  public static RegExp create(String pattern) {
    return doCreate(true, pattern);
  }

  /**
   * Create a new instance of a regular expression handler
   * and compile the pattern. If a pattern error occurs dump
   * the stack trace and rethrow the exception. Normall used
   * from static initializers since it seems exception handling
   * is funny in thows cases.
   * @return An instance of RegExp with a pattern.
   * @see RegExp
   */
  public static RegExp createReport(String pattern) {
    RegExp regexp;

    try {
      regexp = doCreate(true, pattern);
    } catch (RegExpPatternError e) {
      e.printStackTrace();

      throw e;
    }

    return regexp;
  }

  /**
   * Do the actual instance creation. Optionally compile a pattern.
   * @ return The regular expression handler.
   */
  final private static RegExp doCreate(boolean compflag, String pattern) {
    if (reClass == null) {
      initFactory();

      if (reClass == null) {
	throw new NoClassDefFoundError("Can not find a RegExp handler.");
      }
    }

    RegExp regexp;

    try {
      regexp = (RegExp) reClass.newInstance();

      if (compflag) {
	regexp.compile(pattern);
      }
    } catch (IllegalAccessException e) {
      throw new NoClassDefFoundError("IllegalAccessException really.");
    } catch (InstantiationException e) {
      throw new NoClassDefFoundError("InstantiationException really.");
    }

    // catch(NoSuchMethodException e) {
    // throw new NoClassDefFoundError("NoSuchMethodException really.");
    // }
    return regexp;
  }

  /**
   * @return The class names of the the know implementations.
   */
  public static String[] getKnownImplementations() {
    String[] imps = new String[reImp.size()];
    for(int i = 0; i < reImp.size(); i++) {
      imps[i] = (String)reImp.elementAt(i);
    }
    return imps;
  }

  /**
   * Append the argument to the list of known implementations.
   * Don't allow duplicates.
   */
  public static void addImplementation(String imp) {
    if(reImp.indexOf(imp) >= 0) {
      return;
    }
    reImp.addElement(imp);
  }

  /**
   * @return The name of the implementation that is being vended or null
   * if the factory is not operable.
   */
  public static String getRegExpClass() {
    return reClass.getName();
  }

  /**
   * @return The name of the actual regular expression handler or null
   * if the factory is not operable.
   */
  public static String getRegExpAdapted() {
    return reAdapted;
  }

  /**
   * @return the display name of the req exp handler or null if factory
   * is not operable.
   */
  public static String getRegExpDisplayName() {
    return reDisplayName;
  }

  /**
   * Look for the known implementations of RegExp. Set up the
   * factory to use the first one found. If the factory is
   * already initialized just return. This is called automatically,
   * but calling it during application startup may make more sense.
   * @return True if a the factory is usable, otherwise false.
   */
  public static boolean initFactory() {
    if (reClass != null) {
      return true;
    }
    for (int i = 0; i < reImp.size(); i++) {
      try {
	load((String)reImp.elementAt(i));

	return true;
      } catch (Throwable e) {}
    }

    return false;
  }

  /**
   * Load <i>reClassName</i> and use it as the implementation
   * of regular expression handling. <i>reClassName</i> is the class
   * name of the adaptor class; it must be a subclass
   * of RegExp. If reClassName is legitimate then it replaces the
   * current implementation, if any, being used. If the class
   * can not be loaded for any reason then an exception or error
   * is thrown. In other words, any return indicates success.
   * In the event of an error or exception, a previously loaded
   * RegExp continues in use by this factory.
   * @see RegExp
   * @see RegExp#canInstantiate
   */
  public static void load(String reClassName)
	  throws ClassNotFoundException, IllegalArgumentException,
		 ClassCastException, NoSuchMethodException,
		 SecurityException {
    Class cls = Class.forName(reClassName);  // may throw

    // Must be a subclass of RegExp
    if (!com.raelity.text.RegExp.class.isAssignableFrom(cls)) {
      throw new ClassCastException(reClassName + " does not extend "
				   + "com.raelity.text.RegExp");
    }

    String aString;
    String bString;

    try {
      Method t = cls.getMethod("canInstantiate", new Class[0]);

      if (!((Boolean) t.invoke(null, new Object[0])).booleanValue()) {
	throw new Exception();
      }

      t = cls.getMethod("getAdaptedName", new Class[0]);
      aString = (String) t.invoke(null, new Object[0]);

      t = cls.getMethod("getDisplayName", new Class[0]);
      bString = (String) t.invoke(null, new Object[0]);
    } catch (Exception e) {
      throw new ClassNotFoundException(reClassName + " can not instantiate");
    }

    RegExpFactory.reAdapted = aString;
    RegExpFactory.reClass = cls;
    RegExpFactory.reClassName = reClassName;
    RegExpFactory.reDisplayName = bString;
    if(false){
      System.err.println("reAdapted = " + reAdapted );
      System.err.println("reClassName = " + reClassName );
      System.err.println("reDisplayName = " + reDisplayName );
    }

    return;
  }

  // debug

  /**
   * Method declaration
   *
   *
   * @param args
   *
   * @see
   */
  public static void main(String args[]) {
    builtinTest();
  }

  /**
   * Runs some tests using the known implementations.
   * Print PASS or FAIL.
   */
  static public boolean builtinTest() {

    // triplets, pattern, input, optional escape character.
    Object[] testInput = new Object[] {
      "(?e=#)^([+-]?)(#d*)$", "", null,
      "(?e=#)^([+-]?)(#d*)$", "+", null,
      "(?e=#)^([+-]?)(#d*)$", "1", null,
      "(?e=#)^([+-]?)(#d*)$", "-1", null,
      "xyz", "abc", null,
      "#w+", "hello", new Character('#'),
      "([abc]+)([def]+)([ghi]+)?([jkl]+)z*", "xxaaeekkzzzzzzz", null,
      "(?e=#)#s+(#w)+#s", "       how       ", null,
      "(?e=#)#s(\\\\)(##)(#w+)", " \\\\#hi(*&^^%", null,
      "#s(\\\\)(##)(#w+)", " \\\\#hi(*&^^%", null,
      "#s(\\\\)(##)(#w+)", " \\\\#hi(*&^^%", new Character('#')
    };

    test1("com.raelity.text.RegExpJava", testInput);
    test1("com.raelity.text.RegExpGNU", testInput);
    test1("com.raelity.text.RegExpStevesoft", testInput);
    test1("com.raelity.text.RegExpOroinc", testInput);

    // NEEDSWORK: check for errors.
    return true;
  }

  /**
   * Method declaration
   *
   *
   * @param cls
   * @param testInput
   *
   * @see
   */
  static void test1(String cls, Object testInput[]) {
    String pattern, input;

    System.out.println("\n************* " + cls);

    try {
      load(cls);
    } catch (Exception e) {
      e.printStackTrace();

      return;
    }
    System.out.println("************* " + getRegExpDisplayName());

    for (int i = 0; i < testInput.length; i += 3) {
      pattern = (String) testInput[i];
      input = (String) testInput[i + 1];

      Character c = (Character) testInput[i + 2];
      String    extra = c == null ? "" : " Escape: '" + c.toString() + "'";

      System.out.println("\nPattern: " + pattern + extra);
      System.out.println("Input: '" + input + "'");

      RegExp re = create();

      if (c != null) {
	re.setEscape(c.charValue());
      }

      try {
	re.compile(pattern);
      } catch (Throwable e) {
	e.printStackTrace();

	return;
      }

      re.search(input);
      dumpResult(re);

      // dumpResult(re.getResult());
    }
  }

  /**
   * Dump all the information about
   * a match. The general form of an output line is
   * <br><pre>
   * matched_string [start,stop) num_chars
   * </pre><br>This can be usefull for debugging.
   */
  public static void dumpResult(RegExp result) {
    System.out.println("Match " + result.isMatch()
                       + ": '" + result.group(0) + "' [" + result.start(0)
		       + "," + result.stop(0) + ")" + " " + result.length(0));

    int n = result.nGroup();

    System.out.println("Groups: " + n);

    for (int i = 1; i <= n; i++) {
      System.out.println(i + ": '" + result.group(i) + "' ["
			 + result.start(i) + "," + result.stop(i) + ")" + " "
			 + result.length(i));
    }
  }

  /**
   * Same as <i>dumpResult(RegExp)</i> except it takes
   * a <i>RegExpResult</i>.
   */
  public static void dumpResult(RegExpResult result) {
    System.out.println("Match: '" + result.group(0) + "' [" + result.start(0)
		       + "," + result.stop(0) + ")" + " " + result.length(0));

    int n = result.nGroup();

    System.out.println("Groups: " + n);

    for (int i = 1; i <= n; i++) {
      System.out.println(i + ": '" + result.group(i) + "' ["
			 + result.start(i) + "," + result.stop(i) + ")" + " "
			 + result.length(i));
    }
  }

}

/*

************* com.raelity.text.RegExpOroinc

Pattern: (?e=#)^([+-]?)(#d*)$
Input: ''
Match: '' [0,0) 0
Groups: 2
1: '' [0,0) 0
2: '' [0,0) 0

Pattern: (?e=#)^([+-]?)(#d*)$
Input: '+'
Match: '+' [0,1) 1
Groups: 2
1: '+' [0,1) 1
2: '' [1,1) 0

Pattern: (?e=#)^([+-]?)(#d*)$
Input: '1'
Match: '1' [0,1) 1
Groups: 2
1: '' [0,0) 0
2: '1' [0,1) 1

Pattern: (?e=#)^([+-]?)(#d*)$
Input: '-1'
Match: '-1' [0,2) 2
Groups: 2
1: '-' [0,1) 1
2: '1' [1,2) 1

Pattern: xyz
Input: 'abc'
Match: 'null' [-1,-1) -1
Groups: 0

Pattern: #w+ Escape: '#'
Input: 'hello'
Match: 'hello' [0,5) 5
Groups: 0

Pattern: ([abc]+)([def]+)([ghi]+)?([jkl]+)z*
Input: 'xxaaeekkzzzzzzz'
Match: 'aaeekkzzzzzzz' [2,15) 13
Groups: 4
1: 'aa' [2,4) 2
2: 'ee' [4,6) 2
3: 'null' [-1,-1) -1
4: 'kk' [6,8) 2

Pattern: (?e=#)#s+(#w)+#s
Input: '       how       '
Match: '       how ' [0,11) 11
Groups: 1
1: 'w' [9,10) 1

Pattern: (?e=#)#s(\\)(##)(#w+)
Input: ' \\#hi(*&^^%'
Match: ' \\#hi' [0,6) 6
Groups: 3
1: '\\' [1,3) 2
2: '#' [3,4) 1
3: 'hi' [4,6) 2

Pattern: #s(\\)(##)(#w+)
Input: ' \\#hi(*&^^%'
Match: 'null' [-1,-1) -1
Groups: 0

Pattern: #s(\\)(##)(#w+) Escape: '#'
Input: ' \\#hi(*&^^%'
Match: ' \\#hi' [0,6) 6
Groups: 3
1: '\\' [1,3) 2
2: '#' [3,4) 1
3: 'hi' [4,6) 2
*/


/*--- formatting done in "Sun Java Convention" style on 02-25-2000 ---*/

