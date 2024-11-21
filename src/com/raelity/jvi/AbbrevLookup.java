
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

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This is a lookup table where the key can be abbreviated. It is convenient
 * for words that are entered by users.
 */
public class AbbrevLookup {
  List list = new ArrayList();

  public AbbrevLookup() {
  }

  /**
   * This method returns a read-only copy of the list.
   * The elements of the list are {@link CommandElement}.
   */
  public List getList() {
    return Collections.unmodifiableList(list);
  }

  /**
   * Register a command; the abbrev must be unique.  The abbrev is the "key"
   * for the command, for example since "s" is the abbreviation for "substitue"
   * and "se" is the abbreviation for "set" then "substitute" sorts earlier
   * than "set" because "s" sorts earlier than "se". Consider this when
   * adding commands, since unique prefix has nothing to do with how commands
   * are recognized.
   * @exception IllegalArgumentException this is thrown if the abbreviation
   * and/or the name already exist in the list or there's a null argument.
   */
  public void add(String abbrev, String name, Object value) {
    if(abbrev == null || name == null || value == null) {
      throw new IllegalArgumentException("All arguments must be non-null");
    }
    int idx = Collections.binarySearch(list, getKey(abbrev));
    if(idx >= 0) {
      throw new IllegalArgumentException("The abbreviation '" + abbrev
                                 + "' is already registered");
    }
    // spin through the list insuring command name not in use
    Iterator iter = list.iterator();
    while(iter.hasNext()) {
      if(((CommandElement)iter.next()).name.equals(name)) {
        throw new IllegalArgumentException("The name '" + abbrev
                                 + "' is already registered");
      }
    }
    if( ! name.startsWith(abbrev)) {
      throw new IllegalArgumentException("'" + name + "' does not start with '"
                                 + abbrev + "'");
    }
    // turn idx into something that can be used for insertion into list
    idx = -idx - 1;
    list.add(idx, new CommandElement(abbrev, name, value));
  }

  /**
   * Search the list for the command. Sequentially search a list starting
   * with the list that start with the first letter of the input command.
   */
  public CommandElement lookupCommand(String command) {
    CommandElement ce = null;
    if(command.length() == 0) {
      return ce;
    }
    String firstChar = command.substring(0, 1);
    String nextChar = new String( new char[] { (char)(command.charAt(0) + 1) });
    int cidx = Collections.binarySearch(list, getKey(firstChar));
    if(cidx < 0) {
      cidx = -cidx - 1;
    }
    Iterator iter = list.listIterator(cidx);
    while(iter.hasNext()) {
      CommandElement ce01 = (CommandElement)iter.next();
      if(ce01.abbrev.compareTo(nextChar) >= 0) {
        break; // not in list
      }
      if(ce01.match(command)) {
        ce = ce01;
        break;
      }
    }
    return ce;
  }

  private CommandElement aCommandElement = new CommandElement();
  /**
   * Return a command element that can be used for searching the
   * command list. The returned element is reusable.
   */
  private CommandElement getKey(String abbrev) {
    aCommandElement.abbrev = abbrev;
    return aCommandElement;
  }

  /**
   * A registered command is represented by this class.
   * Comparison and equality is based on the command abbreviation.
   */
  static public class CommandElement
                      implements Comparable
  {
    private String abbrev;
    private String name;
    private Object value;

    CommandElement() {
    }

    CommandElement(String abbrev, String name, Object value) {
      this.abbrev = abbrev;
      this.name = name;
      this.value = value;
    }

    /**
     * @return the abbreviation for the command
     */
    public String getAbbrev() {
      return abbrev;
    }

    /**
     * @return the full command name
     */
    public String getName() {
      return name;
    }

    /**
     * @return the value.
     */
    public Object getValue() {
      return value;
    }

    /**
     * Check if the argument can invoke this command.
     */
    public boolean match(String tryName) {
      return tryName.startsWith(this.abbrev)
             && this.name.startsWith(tryName);
    }

    public int compareTo(Object o1) {
      return abbrev.compareTo(((CommandElement)o1).abbrev);
    }

    public boolean equals(Object o1) {
      if( ! (o1 instanceof CommandElement)) {
        return false;
      }
      return abbrev.equals(o1);
    }
  }
}
