/*
 * A command line entry widget that sits on the glass pane.
 * Doing this, instead of a modal dialog, avoids problems
 * with interpreter bugs, particularly on linux.
 */

package com.raelity.jvi.swing;
import com.raelity.jvi.*;
import com.raelity.jvi.swing.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.*;
import javax.swing.text.JTextComponent;

public class InlineCmdEntry implements ViCmdEntry, ActionListener{
	protected int entryType;
	protected ActionListener listener;
	protected CommandLine commandLine;
	protected String lastCommand;
	protected ViTextView parentTV;
	private String initText;
	private MouseListener mouseListener;
	public InlineCmdEntry(){
		this(ViCmdEntry.COLON_ENTRY);
	}
	public InlineCmdEntry(int type){
		entryType=type;
		commandLine = new CommandLine();
		commandLine.setupBorder();
		commandLine.addActionListener(this);
		commandLine.setMode(entryType == ViCmdEntry.COLON_ENTRY
                                    ? ":" : "/");
		commandLine.setList(new LinkedList());
                // NEEDSWORK: FOCUS: use FocusTraversalPolicy
		commandLine.setNextFocusableComponent(commandLine);
		mouseListener = new MouseAdapter() {
		  public void mousePressed(MouseEvent evt) {
		    commandLine.getToolkit().beep();
		  }
		};
	}
	public void activate(String mode, ViTextView parent){
		this.activate(mode, parent, "", false);
	};
	public void activate(String mode, ViTextView parent,
			     String initialText, boolean passThru){
		parentTV=parent;
		initText=initialText;
		lastCommand = "";
		if(passThru) {
		  lastCommand = initialText;
		  fireEvent(new ActionEvent(parentTV.getEditorComponent(),
					    ActionEvent.ACTION_PERFORMED,
					    "\n"));
		  return;
		}
		commandLine.setMode(mode);
		commandLine.init(initText);

		JPanel p = (JPanel)getRootPane().getGlassPane();
		placeCmdEntry(p);
		if(p.getLayout() != null) {
		  p.setLayout(null);
		}
		p.setVisible(true);
		p.addMouseListener(mouseListener);
		// by placing p.add(commandLine) after the p.setVisible
		// a blanking and redraw of the combo box is avoided.
		p.add(commandLine);
		commandLine.takeFocus(true);
	};
	/**
	 * Position the CmdEntry just beneath the editor component's
	 * scroll bar.
	 */
	private void placeCmdEntry(JPanel glass) {
		Container jc = SwingUtilities.getAncestorOfClass(
			    javax.swing.JScrollPane.class,
			    parentTV.getEditorComponent());
		if(jc == null) {
		  jc = parentTV.getEditorComponent();
		}

		Dimension d00 = commandLine.getPreferredSize();
		Rectangle pos = jc.getBounds(); // become bounds for commmand entry
		pos.translate(0, jc.getHeight());  // just beneath component
		pos.height = d00.height;
		// trim the width
		pos.width = Math.min(500, jc.getWidth());
		// now translate bounds so relative to glass pane
		Point p00 = SwingUtilities.convertPoint(jc.getParent(),
							pos.x, pos.y, glass);
		pos.setLocation(p00);
		// If past bottom of glass pane, shift it up
		int offset = glass.getHeight() - (pos.y + pos.height);
		if(offset < 0) {
		  pos.translate(0, offset);
		}
		commandLine.setBounds(pos);
	}
	protected JRootPane getRootPane(){
		if(parentTV!=null)
			return (JRootPane)SwingUtilities.getRootPane(parentTV.getEditorComponent());
		else
			return null;
	}
	public String getCommand(){
		return lastCommand;
	};
	public void cancel(){
	  lastCommand = "";
	  shutdownEntry();
	};
	public void append(String s){
		commandLine.append(s);
	};

        public JTextComponent getTextComponent() {
            return commandLine.getTextField();
        }

	public void actionPerformed(ActionEvent e) {
            // VISUAL REPAINT HACK
            // Repaint before executing commands.. 
            // so that I can be sure the visual area didn't change yet
            // and all has been repainted
            if(G.drawSavedVisualBounds) {
                G.drawSavedVisualBounds = false;
                G.curwin.updateVisualState();
            }
            // END VISUAL REPAINT HACK
	    lastCommand = commandLine.getCommand();
            if(Options.getOption(Options.dbgKeyStrokes).getBoolean())
                System.err.println("CommandAction: '" + lastCommand + "'");
	    shutdownEntry();
	    fireEvent(e);
	    commandLine.clear();
	}
	protected void shutdownEntry(){
		//commandLine.removeActionListener(this);
		JPanel glass = (JPanel)getRootPane().getGlassPane();
		glass.removeMouseListener(mouseListener);
		glass.setVisible(false);
		glass.remove(commandLine);
		
		// repaint area around entry right now so it looks faster
		JComponent jc = (JComponent)getRootPane().getContentPane();
		Rectangle pos = commandLine.getBounds();
		Point p00 = SwingUtilities.convertPoint(glass,
							pos.x, pos.y, jc);
		pos.setLocation(p00);						
		jc.paintImmediately(pos);
							
		parentTV.getEditorComponent().requestFocus();
		parentTV = null;
	}

	/** Send the event, if it is a successful entry, a CR, then
	 * record the input. Note that initial Text is not part of
	 * the recorded input.
	 */
	protected void fireEvent(ActionEvent e){
		if(e.getActionCommand().charAt(0) == '\n') {
			StringBuffer sb = new StringBuffer();
			if( ! initText.equals("")
				&& lastCommand.startsWith(initText)) {
				sb.append(lastCommand.substring(initText.length()));
			} else {
				sb.append(lastCommand);
			}
			sb.append('\n');
			GetChar.userInput(new String(sb));
		}
		listener.actionPerformed(e);
	}

	public void addActionListener(ActionListener l)
		throws TooManyListenersException {
		if(listener != null) {
			throw new TooManyListenersException();
		}
		listener = l;
	}

	public void removeActionListener(ActionListener l) {
		if(listener == l) {
			listener = null;
		}
	}
}