/*
 * OpsBase.java
 *
 * Created on January 1, 2007, 10:33 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.raelity.jvi.swing;

import com.raelity.jvi.Constants;
import com.raelity.jvi.ViTextView;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.text.DefaultEditorKit;

import static com.raelity.jvi.Constants.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

/**
 * This provides default swings JEditorPane actions that are used by vi.
 *
 * Usually {@link #xop(int)} is overridden for the environment. When overridden,
 * the superclass should be invoked if given an unrecognized op.
 *
 * This implementation simply uses actions from the editor kit. The action names
 * are taken from the default editor kit. The action map is built as needd, so
 * if the same instance is used with different editors which have different
 * actions, curious results could ensue.
 *
 * @author erra
 */
public class OpsBase implements TextOps {

	/**
	 * Creates a new instance of OpsBase
	 */
	public OpsBase(ViTextView textView) {
		this.textView = textView;
	}

	public void init(RSyntaxTextArea editorPane) {
	}

	public void xact(Action action) {
		event.setSource(textView.getEditorComponent());
		action.actionPerformed(event);
	}

	public void xact(Action action, String s) {
		event.setActionCommand(s);
		xact(action);
	}

	public void xop(int op, String s) {
		event.setActionCommand(s);
		xop(op);
	}

	public void xact(String actionName) {
		Action action = findAction(actionName);
		xact(action);
	}

	/**
	 * Try to do something useful with the op.
	 */
	public void xop(int op) {
		String actionName;
		switch (op) {
			case INSERT_TEXT:
				actionName = DefaultEditorKit.insertContentAction;
				break;
			case KEY_TYPED:
				actionName = DefaultEditorKit.defaultKeyTypedAction;
				break;
			case INSERT_NEW_LINE:
				actionName = DefaultEditorKit.insertBreakAction;
				break;
			case INSERT_TAB:
				actionName = DefaultEditorKit.insertTabAction;
				break;
			case DELETE_PREVIOUS_CHAR:
				actionName = DefaultEditorKit.deletePrevCharAction;
				break;
			default:
				throw new RuntimeException("Unknown op: " + op);
		}
		xact(actionName);
	}

	protected void xact(String actionName, String cmd) {
		event.setActionCommand(cmd);
		xact(actionName);
	}

	protected Action findAction(String actionName) {
		Action action = (Action) actionMap.get(actionName);
		if (action == null) {
			Action[] actions
				= textView.getEditorComponent().getActions();
			for (int i = 0; i < actions.length; i++) {
				String name = (String) actions[i].getValue(Action.NAME);
				if (name.equals(actionName)) {
					action = actions[i];
					actionMap.put(name, action);
					break;
				}
			}
		}
		if (action == null) {
			throw new RuntimeException("Action " + actionName + "not found");
		}
		return action;
	}

	protected static class ReusableEvent extends ActionEvent {

		private String cmd;

		ReusableEvent() {
			super("", ActionEvent.ACTION_PERFORMED, "");
		}

		public void setSource(Object source) {
			this.source = source;
		}

		void setActionCommand(String cmd) {
			this.cmd = cmd;
		}

		public String getActionCommand() {
			return cmd;
		}
	}

	protected ViTextView textView;

	protected OpsBase.ReusableEvent event = new ReusableEvent();

	protected static Map actionMap = new HashMap();
}
