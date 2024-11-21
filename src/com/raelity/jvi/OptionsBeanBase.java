/*
 * OptionsBeanBase.java
 *
 * Created on January 23, 2007, 11:04 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.raelity.jvi;

import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyDescriptor;
import java.beans.PropertyVetoException;
import java.beans.SimpleBeanInfo;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.util.List;
import java.util.prefs.Preferences;

import com.raelity.jvi.Option.ColorOption;

/**
 * Base class for jVi options beans. This method contains the read/write methods
 * for all options. Which options are made visible is controlled by the
 * optionsList given to the contstructor. Using this class, options are
 * grouped into different beans.
 *
 * @author erra
 */
public class OptionsBeanBase extends SimpleBeanInfo {
    private Class clazz;
    private List<String> optionsList;
    private String displayName;
    
    private final PropertyChangeSupport pcs = new PropertyChangeSupport( this );
    private final VetoableChangeSupport vcs = new VetoableChangeSupport( this ); 
    
    /** Creates a new instance of OptionsBeanBase */
    public OptionsBeanBase(Class clazz, String displayName,
                           List<String> optionsList) {
        this.clazz = clazz;
        this.displayName = displayName;
        this.optionsList = optionsList;
    }
    
    public BeanDescriptor getBeanDescriptor() {
        return new ThisBeanDescriptor();
    }

    public PropertyDescriptor[] getPropertyDescriptors() {
	PropertyDescriptor[] descriptors
                    = new PropertyDescriptor[optionsList.size()];
	int i = 0;

	for(String name : optionsList) {
            PropertyDescriptor d;
            if(name.equals("jViVersion")) {
                try {
                    d = new PropertyDescriptor(name, clazz,
                            "getJViVersion", null);
                } catch (IntrospectionException ex) {
                    ex.printStackTrace();
                    continue;
                }
                d.setDisplayName("jVi Version");
            } else {
                try {
                    d = createPropertyDescriptor(name, name, clazz);
                } catch (IntrospectionException ex) {
                    ex.printStackTrace();
                    continue;
                }
            }
	    descriptors[i++] = d;
	}
	return descriptors;
    }
    
    protected PropertyDescriptor createPropertyDescriptor(String optName,
                                                          String methodName,
                                                          Class clazz)
    throws IntrospectionException {
        PropertyDescriptor d = null;
        Option opt = Options.getOption(optName);
        d = new PropertyDescriptor(methodName, clazz);
        d.setDisplayName(opt.getDisplayName());
        d.setExpert(opt.isExpert());
        d.setHidden(opt.isHidden());
        d.setShortDescription(opt.getDesc());
        if(opt instanceof IntegerOption
        || opt instanceof StringOption) {
            d.setBound(true);
            d.setConstrained(true);
        }
        return d;
    }
    
    /* This doesn't work. wonder why?
    public static Image getJViLogo(int type) {
        if (type == BeanInfo.ICON_COLOR_16x16
                || type == BeanInfo.ICON_MONO_16x16) {
            if (icon == null)
                icon = Toolkit.getDefaultToolkit().getImage(
                            "/com/raelity/jvi/resources/jViLogo.png");
            return icon;
        } else {
            if (icon32 == null)
                icon = Toolkit.getDefaultToolkit().createImage(
                            "/com/raelity/jvi/resources/jViLogo32.png");
            return icon32;
        }
    }
     */
    
    private static Image icon, icon32;
    public Image getIcon (int type) {
        if (type == BeanInfo.ICON_COLOR_16x16
                || type == BeanInfo.ICON_MONO_16x16) {
            if (icon == null)
                icon = loadImage("/com/raelity/jvi/resources/jViLogo.png");
            return icon;
        } else {
            if (icon32 == null)
                icon = loadImage("/com/raelity/jvi/resources/jViLogo32.png");
            return icon32;
        }
    }
    
    private class ThisBeanDescriptor extends BeanDescriptor {
        ThisBeanDescriptor() {
            super(clazz);
        }
        
        public String getDisplayName() {
	    return displayName;
        }
    }
    
    //
    // Look like a good bean
    //
    
    public void addPropertyChangeListener( PropertyChangeListener listener )
    {
        this.pcs.addPropertyChangeListener( listener );
    }

    public void removePropertyChangeListener( PropertyChangeListener listener )
    {
        this.pcs.removePropertyChangeListener( listener );
    }
    
    public void addVetoableChangeListener( VetoableChangeListener listener )
    {
        this.vcs.addVetoableChangeListener( listener );
    }

    public void removeVetoableChangeListener( VetoableChangeListener listener )
    {
        this.vcs.addVetoableChangeListener( listener );
    } 
    
    //
    //      The interface to preferences.
    //
    private Preferences prefs = ViManager.getViFactory().getPreferences();

    protected void put(String name, String val) throws PropertyVetoException {
        String old = getString(name);
	Option opt = Options.getOption(name);
        ((StringOption)opt).validate(val);
        this.vcs.fireVetoableChange( name, old, val );
	prefs.put(name, val);
        this.pcs.firePropertyChange( name, old, val );
    }

    protected void put(String name, int val) throws PropertyVetoException {
        int old = getint(name);
	Option opt = Options.getOption(name);
        ((IntegerOption)opt).validate(val);
        this.vcs.fireVetoableChange( name, old, val );
	prefs.putInt(name, val);
        this.pcs.firePropertyChange( name, old, val );
    }

    protected void put(String name, Color val) throws PropertyVetoException {
        Color old = getColor(name);
	ColorOption opt = (ColorOption)Options.getOption(name);
        opt.validate(val);
        this.vcs.fireVetoableChange( name, old, val );
	prefs.put(name, opt.xformToString(val));
        this.pcs.firePropertyChange( name, old, val );
    }

    protected void put(String name, boolean val) {
	prefs.putBoolean(name, val);
    }

    protected String getString(String name) {
	Option opt = Options.getOption(name);
	return prefs.get(name, opt.getDefault());
    }

    protected int getint(String name) {
	Option opt = Options.getOption(name);
	return prefs.getInt(name, Integer.parseInt(opt.getDefault()));
    }

    protected Color getColor(String name) {
	Option opt = Options.getOption(name);
        String s = prefs.get(name, opt.getDefault());
	return Color.decode(s);
    }

    protected boolean getboolean(String name) {
	Option opt = Options.getOption(name);
	return prefs.getBoolean(name, Boolean.parseBoolean(opt.getDefault()));
    }
    
    //
    // All the known options
    //      The bean getter/setter
    //
    
    /** this read-only option is special cased */
    public String getJViVersion() {
        return ViManager.getReleaseString();
    }

    public void setViSelectColor(Color arg)  throws PropertyVetoException {
        put(Options.selectColor, arg);
    }

    public Color getViSelectColor() {
        return getColor(Options.selectColor);
    }

    public void setViSelection(String arg)  throws PropertyVetoException {
        put(Options.selection, arg);
    }

    public String getViSelection() {
	return getString(Options.selection);
    }

    public void setViIncrSearch(boolean arg) {
        put(Options.incrSearch, arg);
    }

    public boolean getViIncrSearch() {
	return getboolean(Options.incrSearch);
    }

    public void setViSelectMode(boolean arg) {
        put(Options.selectMode, arg);
    }

    public boolean getViSelectMode() {
	return getboolean(Options.selectMode);
    }

    public void setViEndOfSentence(boolean arg) {
        put(Options.endOfSentence, arg);
    }

    public boolean getViEndOfSentence() {
	return getboolean(Options.endOfSentence);
    }

    public void setViHighlightSearch(boolean arg) {
        put(Options.highlightSearch, arg);
    }

    public boolean getViHighlightSearch() {
	return getboolean(Options.highlightSearch);
    }

    public void setViShowCommand(boolean arg) {
        put(Options.showCommand, arg);
    }

    public boolean getViShowCommand() {
	return getboolean(Options.showCommand);
    }

    public void setViShowMode(boolean arg) {
        put(Options.showMode, arg);
    }

    public boolean getViShowMode() {
	return getboolean(Options.showMode);
    }

    public void setViCommandEntryFrameOption(boolean arg) {
        put("viCommandEntryFrameOption", arg);
    }

    public boolean getViCommandEntryFrameOption() {
	return getboolean("viCommandEntryFrameOption");
    }

    public void setViBackspaceWrapPrevious(boolean arg) {
        put("viBackspaceWrapPrevious", arg);
    }

    public boolean getViBackspaceWrapPrevious() {
	return getboolean("viBackspaceWrapPrevious");
    }

    public void setViHWrapPrevious(boolean arg) {
        put("viHWrapPrevious", arg);
    }

    public boolean getViHWrapPrevious() {
	return getboolean("viHWrapPrevious");
    }

    public void setViLeftWrapPrevious(boolean arg) {
        put("viLeftWrapPrevious", arg);
    }

    public boolean getViLeftWrapPrevious() {
	return getboolean("viLeftWrapPrevious");
    }

    public void setViSpaceWrapNext(boolean arg) {
        put("viSpaceWrapNext", arg);
    }

    public boolean getViSpaceWrapNext() {
	return getboolean("viSpaceWrapNext");
    }

    public void setViLWrapNext(boolean arg) {
        put("viLWrapNext", arg);
    }

    public boolean getViLWrapNext() {
	return getboolean("viLWrapNext");
    }

    public void setViRightWrapNext(boolean arg) {
        put("viRightWrapNext", arg);
    }

    public boolean getViRightWrapNext() {
	return getboolean("viRightWrapNext");
    }

    public void setViTildeWrapNext(boolean arg) {
        put("viTildeWrapNext", arg);
    }

    public boolean getViTildeWrapNext() {
	return getboolean("viTildeWrapNext");
    }

    public void setViUnnamedClipboard(boolean arg) {
        put("viUnnamedClipboard", arg);
    }

    public boolean getViUnnamedClipboard() {
	return getboolean("viUnnamedClipboard");
    }

    public void setViJoinSpaces(boolean arg) {
        put("viJoinSpaces", arg);
    }

    public boolean getViJoinSpaces() {
	return getboolean("viJoinSpaces");
    }

    public void setViShiftRound(boolean arg) {
        put("viShiftRound", arg);
    }

    public boolean getViShiftRound() {
	return getboolean("viShiftRound");
    }

    public void setViNotStartOfLine(boolean arg) {
        put("viNotStartOfLine", arg);
    }

    public boolean getViNotStartOfLine() {
	return getboolean("viNotStartOfLine");
    }

    public void setViChangeWordBlanks(boolean arg) {
        put("viChangeWordBlanks", arg);
    }

    public boolean getViChangeWordBlanks() {
	return getboolean("viChangeWordBlanks");
    }

    public void setViTildeOperator(boolean arg) {
        put("viTildeOperator", arg);
    }

    public boolean getViTildeOperator() {
	return getboolean("viTildeOperator");
    }

    public void setViSearchFromEnd(boolean arg) {
        put("viSearchFromEnd", arg);
    }

    public boolean getViSearchFromEnd() {
	return getboolean("viSearchFromEnd");
    }

    public void setViWrapScan(boolean arg) {
        put("viWrapScan", arg);
    }

    public boolean getViWrapScan() {
	return getboolean("viWrapScan");
    }

    public void setViMetaEquals(boolean arg) {
        put("viMetaEquals", arg);
    }

    public boolean getViMetaEquals() {
	return getboolean("viMetaEquals");
    }

    public void setViMetaEscape(String arg) throws PropertyVetoException {
        put("viMetaEscape", arg);
    }

    public String getViMetaEscape() {
	return getString("viMetaEscape");
    }

    public void setViIgnoreCase(boolean arg) {
        put("viIgnoreCase", arg);
    }

    public boolean getViIgnoreCase() {
	return getboolean("viIgnoreCase");
    }

    public void setViExpandTabs(boolean arg) {
        put("viExpandTabs", arg);
    }

    public boolean getViExpandTabs() {
	return getboolean("viExpandTabs");
    }

    public void setViReport(int arg) throws PropertyVetoException {
        put("viReport", arg);
    }

    public int getViReport() {
	return getint("viReport");
    }

    public void setViBackspace(int arg) throws PropertyVetoException {
        put("viBackspace", arg);
    }

    public int getViBackspace() {
	return getint("viBackspace");
    }

    public void setViScrollOff(int arg) throws PropertyVetoException {
        put("viScrollOff", arg);
    }

    public int getViScrollOff() {
	return getint("viScrollOff");
    }

    public void setViShiftWidth(int arg) throws PropertyVetoException {
        put("viShiftWidth", arg);
    }

    public int getViShiftWidth() {
	return getint("viShiftWidth");
    }

    public void setViTabStop(int arg) throws PropertyVetoException {
        put("viTabStop", arg);
    }

    public int getViTabStop() {
	return getint("viTabStop");
    }

    public void setViReadOnlyHack(boolean arg) {
        put("viReadOnlyHack", arg);
    }

    public boolean getViReadOnlyHack() {
	return getboolean("viReadOnlyHack");
    }

    public void setViClassicUndo(boolean arg) {
        put("viClassicUndo", arg);
    }

    public boolean getViClassicUndo() {
	return getboolean("viClassicUndo");
    }

    public void setViDbgKeyStrokes(boolean arg) {
        put("viDbgKeyStrokes", arg);
    }

    public boolean getViDbgKeyStrokes() {
	return getboolean("viDbgKeyStrokes");
    }

    public void setViDbgCache(boolean arg) {
        put("viDbgCache", arg);
    }

    public boolean getViDbgCache() {
	return getboolean("viDbgCache");
    }

    public void setViDbgBang(boolean arg) {
        put("viDbgBang", arg);
    }

    public boolean getViDbgBang() {
	return getboolean("viDbgBang");
    }

    public void setViDbgEditorActivation(boolean arg) {
        put("viDbgEditorActivation", arg);
    }

    public boolean getViDbgEditorActivation() {
	return getboolean("viDbgEditorActivation");
    }
}
