package com.raelity.text;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;

public class RegExpResultJava extends RegExpResult
{
    public RegExpResultJava(Matcher m) {
	if(m == null) {
	    return;
	}
        this.result = m.toMatchResult();
    }
    public boolean isMatch() {
        return (null != result);
    }
    public int nGroup() {
        if(result == null) {
	    return 0;
	}
        return result.groupCount();
    }
    public String group(int i) {
        if(result == null) {
	    return null;
	}
        return result.group(i);
    }
    public int length(int i) {
        if(result == null || i > nGroup() || result.end(i) < 0) {
	    return -1;
	}
        return result.end(i) - result.start(i);
    }
    public int start(int i) {
        if(result == null) {
	    return -1;
	}
        return result.start(i);
    }
    public int stop(int i) {
        if(result == null) {
	    return -1;
	}
        return result.end(i);
    }
    private MatchResult result;
}
