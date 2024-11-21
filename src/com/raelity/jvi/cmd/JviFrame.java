/**
 * Title:        jVi<p>
 * Description: A VI-VIM clone. Use VIM as a model where applicable.<p>
 * Copyright: Copyright (c) Ernie Rael<p>
 * Company: Raelity Engineering<p>
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
package com.raelity.jvi.cmd;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class JviFrame extends JFrame {

	JPanel contentPane;

	BorderLayout borderLayout1 = new BorderLayout();
	JTree file_tree = new JTree();
	public static JSplitPane split_root = new JSplitPane();
	public static EditorPanel selected = null;

	//Construct the frame
	public JviFrame() {
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		try {
			jbInit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//Component initialization
	private void jbInit() throws Exception {
		contentPane = (JPanel) this.getContentPane();
		contentPane.setLayout(borderLayout1);
		this.setSize(new Dimension(400, 285));
		this.setTitle("Frame Title");
		this.setSize(800, 600);
		this.setLocationRelativeTo(null);

		split_root.setLeftComponent(file_tree);
		split_root.setRightComponent(new EditorPanel());
		split_root.setDividerLocation(0);
		contentPane.add(split_root);
	}

	public static void split(int direction) {
		if (split_root == null || JviFrame.selected == null) {
			System.out.println("No split root or selected component.");
			return;
		}

		var focused = JviFrame.selected;

		// Traverse and add the new split pane
		boolean result = traverseAndSplit(split_root, focused, direction);

		if (!result) {
			System.out.println("Failed to split: Focused editor not found in the split hierarchy.");
		}
	}

	private static boolean traverseAndSplit(JSplitPane parent, Component focused, int direction) {
		if (parent == null) {
			return false;
		}

		Component left = parent.getLeftComponent();
		Component right = parent.getRightComponent();

		// Base case: if the focused editor is a direct child of this split pane
		if (left == focused || right == focused) {
			// Create a new split pane
			JSplitPane newSplit = new JSplitPane(direction);
			newSplit.setLeftComponent(focused); // Keep the focused editor as the left component
			var editor = new EditorPanel();
			editor.getEditor().requestFocusInWindow();
			JviFrame.selected = editor;
			newSplit.setRightComponent(editor); // Add the new editor on the right

			// Replace the focused editor with the new split pane in the parent split pane
			if (left == focused) {
				parent.setLeftComponent(newSplit);
			} else {
				parent.setRightComponent(newSplit);
			}

			// Update UI
			parent.revalidate();
			parent.repaint();
			return true;
		}

		// Recursive case: check child splits
		boolean leftResult = (left instanceof JSplitPane) && traverseAndSplit((JSplitPane) left, focused, direction);
		boolean rightResult = (right instanceof JSplitPane) && traverseAndSplit((JSplitPane) right, focused, direction);

		return leftResult || rightResult;
	}

	
	/*
	public static void unsplitFocused() {
		//TODO
	}*/

	public static void unsplitFocused() {
		if (split_root == null || JviFrame.selected == null) {
			System.out.println("No split root or selected component.");
			return;
		}

		var focused = JviFrame.selected;

		// Traverse and modify the tree
		boolean result = traverseAndUnsplit(split_root, focused);

		if (!result) {
			System.out.println("Failed to unsplit: Focused editor not found in the split hierarchy.");
		}
	}

	private static boolean traverseAndUnsplit(JSplitPane parent, Component focused) {
		if (parent == null) {
			return false;
		}

		Component left = parent.getLeftComponent();
		Component right = parent.getRightComponent();

		// Base case: if the focused editor is a direct child of this split pane
		if (left == focused || right == focused) {
			// Get the sibling of the focused editor
			Component sibling = (left == focused) ? right : left;

			// Replace the parent JSplitPane with the sibling in the grandparent container
			Container grandParent = parent.getParent();
			if (grandParent instanceof JSplitPane grandSplit) {
				if (grandSplit.getLeftComponent() == parent) {
					grandSplit.setLeftComponent(sibling);
				} else {
					grandSplit.setRightComponent(sibling);
				}
			} else if (grandParent instanceof JRootPane) {
				// Replace the root split with the sibling
				grandParent.remove(parent);
				grandParent.add(sibling);
			} else {
				System.out.println("Unrecognized grandparent type: " + grandParent.getClass().getName());
			}

			// Revalidate and repaint the UI
			sibling.revalidate();
			sibling.repaint();
			return true; // Unsplit successful
		}

		// Recursive case: check child splits
		boolean leftResult = (left instanceof JSplitPane) && traverseAndUnsplit((JSplitPane) left, focused);
		boolean rightResult = (right instanceof JSplitPane) && traverseAndUnsplit((JSplitPane) right, focused);

		return leftResult || rightResult;
	}

	

	//File | Exit action performed
	public void fileExit_actionPerformed(ActionEvent e) {
		System.exit(0);
	}

	//Help | About action performed
	public void helpAbout_actionPerformed(ActionEvent e) {
	}

	//Overridden so we can exit when window is closed
	protected void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			fileExit_actionPerformed(null);
		}
	}
}
