package com.raelity.text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.text.Segment;

public class RegExpJava extends RegExp
{
    public RegExpJava() {
    }
    public static String getDisplayName() {
        return "Java 1.4+ Regular Expression";
    }
    public static int patternType() {
        return RegExp.PATTERN_PERL5;
    }
    public static boolean canInstantiate() {
        return true;
    }
    public static String getAdaptedName() {
        return "java.util.regex.Pattern";
    }


    /**
     * Prepare this regular expression to use <i>pattern</i>
     * for matching. The string can begin with "(e?=x)" to
     * specify an escape character.
     */
    public void compile(String patternString, int compileFlags) 
    throws RegExpPatternError {

	int flags = Pattern.MULTILINE; // also treats end of string as newline
	flags |= ((compileFlags & RegExp.IGNORE_CASE) != 0)
	          ? Pattern.CASE_INSENSITIVE : 0;
	char fixupBuffer[];
	if(patternString.startsWith("(?e=")
		&& patternString.length() >= 6
		&& patternString.charAt(5) == ')') {
	    // change the escape character
	    fixupBuffer = patternString.toCharArray();
	    patternString = fixupEscape(fixupBuffer, 6, fixupBuffer[4]);
	} else if(escape != '\\') {
	    // change the escape character
	    patternString = fixupEscape(patternString.toCharArray(), 0, escape);
	}
	try {
	    pat = Pattern.compile(patternString, flags);
	} catch (PatternSyntaxException e) {
            throw new RegExpPatternError(e.getDescription(), e.getIndex(), e);
        }
    }

    public boolean search(String input) {
        return search(input, 0);
    }

    public boolean search(String input, int start) {
	// NEEDSWORK: check anchoring issues (like GNU)
        Matcher m = pat.matcher(input);
        matched = m.find();
        result = new RegExpResultJava(matched ? m : null);
        return matched;
    }

    public boolean search(char[] input, int start, int len) {
	MySegment s = new MySegment(input, 0, input.length);
        Matcher m = pat.matcher(s).region(start, start+len);
        matched = m.find();
        result = new RegExpResultJava(matched ? m : null);
        return matched;
    }

    public RegExpResult getResult() {
        return result;
    }
    public int nGroup() {
        return result.nGroup();
    }
    public String group(int i) {
        return result.group(i);
    }
    public int length(int i) {
        return result.length(i);
    }
    public int start(int i) {
        return result.start(i);
    }
    public int stop(int i) {
        return result.stop(i);
    }
    private RegExpResultJava result;
    private Pattern pat;
    
    /** To support jdk1.5, need a segment that isa CharSequence */
    private class MySegment extends Segment implements CharSequence {
        public MySegment() {
            super();
        }
        
        public MySegment(char[] array, int offset, int count) {
            super(array, offset, count);
        }
        
        public char charAt(int index) {
            if (index < 0 
                || index >= count) {
                throw new StringIndexOutOfBoundsException(index);
            }
            return array[offset + index];
        }

        public int length() {
            return count;
        }

        public CharSequence subSequence(int start, int end) {
            if (start < 0) {
                throw new StringIndexOutOfBoundsException(start);
            }
            if (end > count) {
                throw new StringIndexOutOfBoundsException(end);
            }
            if (start > end) {
                throw new StringIndexOutOfBoundsException(end - start);
            }
            MySegment segment = new MySegment();
            segment.array = this.array;
            segment.offset = this.offset + start;
            segment.count = end - start;
            return segment;
        }
    }
}
