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

// - VERIFY NEW MATCH IS NON DESTRUCTIVE OF RESULT CLASS
// - DETERMINE behavior when something like ...(...)|(...) is
//   encountered, Should be numbered 1,2 is the non-matched
//   null or empty? how many matches?
// - Check out semantics of using a start at option in stevesoft
//   what is returned for the start at indications

/**
 * This is a simple interface to regular expressions.
 * It exists because the available regex packages have
 * different interfaces. This package does not contain
 * regular expression handling, rather it makes different reqular
 * expression packages appear to have the same interface.
 * It supports compiling a regular expression, matching
 * a regular expression against a string and retrieving
 * the matched parenthesized subexpressions.
 * <p> This RegExp class is not directly instantiated.
 * Concrete classes that are derived from this class are available
 * though <a href="RegExpFactory.html">RegExpFactory</a>.
 * <p>
 * Fortunately the available packages all seemed to
 * be modeled after perl and
 * the cababilities are generally the same. For better documentation
 * on regular expressions refer to perl documentation or the excellent www
 * pages of the packages, noted below, that are adapted.
 * </p><p>
 * The regular expression packages that have adaptors
 * are <a href="http://javaregex.com/">
 * <b>com.stevesoft.pat</b></a>
 * and <a href="http://www.quateams.com/oro/">
 * <b>com.oroinc.text.regex</b></a>.
 * To use this software you must have one of the supported packages
 * installed on your system and available in the classpath.
 * </p><p>
 * Here are simple examples to demonstrate the use of this
 * software, not to teach you regular expressions.
 * </p><p><pre><code>
 *     RegExp re = RegExpFactory.create();
 *     re.setEscape('#'); // use '#' instead of '\' for sanity
 *     try {
 *         re.compile("#s?(#w+)#s+(#w+)#s?"); // 2 words and maybe white space
 *     } catch(RegExpPatternError) {
 *       e.printStackTrace();  // Prints implementation stacktrace
 *     }
 *     re.search("hello      world       ");   // returns true
 *     System.out.println(re.group(1)+" "+re.group(2)); // "hello world"
 * </code></pre></p>
 * <p>The example above shows direct access to the RegExp object
 * to get results. Alternately a result object can be obtained; it
 * has the same methods for access. So the end of the above example
 * could have been written as:
 * <pre><code>
 *     RegExpResult r = re.getResult();
 *     System.out.println(r.group(1)+" "+r.group(2)); // "hello world"
 * </code></pre>
 * <p>
 * When searching a character array or a string can be specified to be
 * searched, and an index/offset from the beginning of the chars can also
 * be specified. If the index/offset is zero, then this is assumed to be
 * the beginning of line for use with the '^' anchor. If the index/offset
 * is greater than zero, then the character before the index/offset is
 * used to determin beginning of line.
 * </p>
 * <br>
 * <font size="+1">
 * Extensions
 * </font>
 * <br>Some extensions are available.
 * <ul>
 * <li>selectable escape character
 *     <br>There are two ways that the escape character can be
 *     specified. The <i>setEscape</i> method as seen in the
 *     above example. It can also be inlined in the pattern by
 *     starting the pattern with "(?e=#)" where '#' is any character
 *     you want for the escape character. This must be the start
 *     of the pattern.
 * </ul>
 * </p><p>There are features of the underlying packages that are
 * not available. In the future.....
 * </p>
 * @see RegExpFactory
 */
public abstract class RegExp {
  /** A compile option */
  public static final int IGNORE_CASE = 0x01;

  /** simple pattern matching only */
  public static final int PATTERN_NONE = 0;

  /** simple pattern matching only */
  public static final int PATTERN_SIMPLE = 1;

  /** perl5 patterns */
  public static final int PATTERN_PERL5 = 2;

  /**
   * The escape character.
   */
  protected char escape = '\\';

  /**
   * Records if the last call to search returned true.
   */
  protected boolean matched = false;

  /**
   * When true do max optimization.
   */
  protected boolean optim = false;

  public static String getDisplayName() {
    return "unnamed";
  }

  /**
   * This method returns the type of pattern that is handled.
   */
  public static int patternType() {
    return PATTERN_NONE;
  }

  /**
   * This is used internally to determine if an underlying
   * regular expression implementation is available.
   * A direct implementation of RegExp always returns true.
   * <p>Since this is usually accessed by a <b>Method</b>,
   * it is declared public to reduce the security requirements.
   * </p>
   * @return True if the underlying regular expression
   * implementation is available, otherwise false.
   */
  public static boolean canInstantiate() {
    return false;
  }

  /**
   * @return The name of the regular expression class that this
   * class is adapting is returned.
   */
  public static String getAdaptedName() {
    return null;
  }

  /**
   * Use the specified char as the escape character.
   * The escape character defaults to '\'.
   * To have an effect this must be done before the <i>compile</i>
   * method is invoked.
   */
  public void setEscape(char escape) {
    this.escape = escape;
  }

  /**
   * Use this before <i>compile</i> is called. When true
   * the pattern parser is optimized for speed. Note this is
   * called before compile. Not all regular expression
   * packages have optional optimization. Optimization defaults
   * to false.
   */
  public void enableOptimize(boolean state) {
    optim = state;
  }

  /**
   * Prepare this regular expression to use <i>pattern</i>
   * for matching.
   * @param pattern The regular expression.
   * @exception RegExpPatternError This is thrown if there
   * is a syntax error detected in the regular expression.
   */
  public void compile(String pattern)
            throws RegExpPatternError {
       compile(pattern, 0);
   }

  /**
   * Prepare this regular expression to use <i>pattern</i>
   * for matching.
   * @param pattern The regular expression.
   * @exception RegExpPatternError This is thrown if there
   * is a syntax error detected in the regular expression.
   */
  public abstract void compile(String pattern, int compileFlags)
            throws RegExpPatternError;

  /**
   * Search for match.
   * @return true if <i>input</i> matches this regular expression.
   */
  public abstract boolean search(String input);

  /**
   * Search for a match in string starting at char position <i>start</i>
   * @return true if <i>input</i> matches this regular expression.
   */
  public abstract boolean search(String input, int start);

  /**
   * Search for a match in char array starting at char position <i>start</i>
   * with the indicated <i>length</i>.
   * @return true if <i>input</i> matches this regular expression.
   */
  public abstract boolean search(char input[], int start, int length);

  /**
   * Check if the last call to <i>search</i> matched.
   * @return True if the match succeded. False if it faled
   * or if no match has been attempted.
   */
  public boolean isMatch() {
    return matched;
  }

  /**
   * Returns a result object for the last call that used this
   * RegExp for searching an input, for example <i>match</i>.
   * Further calls to match do not change the RegExpResult
   * that is returned.
   * @return The results of the match or null if the match failed.
   * @see RegExpResult
   */
  public abstract RegExpResult getResult();

  /**
   * Return the number of backreferences; this is the number
   * of parenthes pairs.
   * @see RegExpResult#nGroup()
   */
  public abstract int nGroup();

  /**
   * Get the <i>i</i>th backreference.
   * @see RegExpResult#group(int)
   */
  public abstract String group(int i);

  /**
   * The length of the corresponding backreference.
   @ see RegExpResult#length(int)
   */
  public abstract int length(int i);

  /**
   * The offset from the begining of the input to
   * the start of thespecified group.
   * @see com.raelity.text.RegExpResult#start(int)
   */
  public abstract int start(int i);

  /**
   * The offset from the begining of the input to
   * the end of the specified group.
   * @see com.raelity.text.RegExpResult#stop(int)
   */
  public abstract int stop(int i);

  // public abstract String[] split(String input);

  /**
   * This function is provided for use by an adaptor
   * when a reg exp implementation does not
   * allow an alternate escape character to be specified.
   * So we search the char array and substite each <i>escape</i>
   * with a '\'. Also a pair of escape characters in a row is
   * replaced by a single character. And a '\' is replaced by two
   * of them.
   * @param escape is the charater that was used as the escape character
   * in <i>pattern</i>
   */
  static String fixupEscape(char[] pattern, int offset, char escape) {
    StringBuffer outpattern = new StringBuffer(2*pattern.length);
    int in;
    char c;
    for(in = offset; in < pattern.length; in++) {
      c = pattern[in];
      if(c == escape) {
        if(in < pattern.length-1 && pattern[in+1] == escape) {
          // two escapes in a row, replace by single escape char
          in++; // skip the second escape
          outpattern.append(escape);
        } else {
          // replace escape char
          outpattern.append('\\');
        }
      } else if(c == '\\') {
        outpattern.append('\\');
        outpattern.append('\\');
      } else {
        outpattern.append(c);
      }
    }
    return outpattern.toString();
  }
}
