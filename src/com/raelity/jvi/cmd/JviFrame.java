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

import com.raelity.jvi.Msg;
import com.raelity.jvi.Normal;
import com.raelity.jvi.ViManager;
import com.raelity.jvi.fs.Utils;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class JviFrame extends JFrame {

	JPanel contentPane;

	BorderLayout borderLayout1 = new BorderLayout();
	public static JTree file_tree = new JTree();
	public static JSplitPane split_root = new JSplitPane();
	public static EditorPanel selected = null;
	private static JMenu buffers = null;
	private static JFrame self = null;
	private static JMenuBar mb = new JMenuBar(); 
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
		self = this;
		contentPane = (JPanel) this.getContentPane();
		contentPane.setLayout(borderLayout1);
		this.setSize(new Dimension(400, 285));
		this.setTitle("Vide");
		this.setSize(800, 600);
		this.setLocationRelativeTo(null);
		
		split_root.setDividerLocation(0);
		loadDirectory(System.getProperty("user.dir"));
		split_root.setLeftComponent(new JScrollPane(file_tree));
		split_root.setRightComponent(new EditorPanel());
		split_root.setDividerLocation(0);
		contentPane.add(split_root);
		setUpMenuBar();
		this.setJMenuBar(mb); 
	}

	private void loadDirectory(String file) {
		File fp = new File(file);
		if (!fp.exists()) {
			System.err.println("error: " + file + " does not extit");
			return;
		}

		if (fp.isFile()) {
			return;
		}

		if (fp.isDirectory()) {
			DefaultTreeModel model = (DefaultTreeModel) file_tree.getModel();
			DefaultMutableTreeNode top = new DefaultMutableTreeNode(fp.getName());
			model.setRoot(top);
			file_tree.setModel(model);
			createNodes(top, fp);
		}
	}

	private void setUpMenuBar() {
		String[] file_menu_items = {
			"Open", "Split-Open", "OpenTab", "New", "Close", "_",
			"Save", "Save As...", "_", "Split Diff With...", "Split Patched By...", "_",
			"Print", "_", "Save-Exit", "Exit"
		};

		JMenu file = getMenuWithItems("File", file_menu_items, action -> {
			String target = action.getActionCommand();
			if (target.equals("Save")) {
				Utils.saveSelectedBuffer();
			} else if (target.equals("Save As...")) {
				Utils.saveSelectedBufferAs();
			} else if (target.equals("Open")) {
				Utils.chooseFile();
			} else if (target.equals("Split-Open")) {
				Utils.chooseFileAndSplit();
			}
		});
		mb.add(file);

		String[] edit_menu_items = {
			"Undo", "Redo", "Repeat", "_", "Cut", "Copy", "Paste", "Put Before", "Put After",
			"Select All", "_", "Find", "Find And Replace", "_", "Settings Window", "Startup Settings"
		};

		JMenu edit = getMenuWithItems("Edit", edit_menu_items, action -> {
			String cmd = action.getActionCommand();
			var editor = selected.getEditor();

			if (cmd.equals("Undo")) {
				editor.undoLastAction();
				return;
			}

			if (cmd.equals("Redo")) {
				editor.redoLastAction();
				return;
			}

			if (cmd.equals("Repeat")) {
				Normal.normal_cmd('.', true);
				return;
			}

			if (cmd.equals("Cut")) {

				return;
			}

			if (cmd.equals("Copy")) {

				return;
			}

			if (cmd.equals("Paste")) {

				return;
			}

		});
		mb.add(edit);
		// TODO: Add Global Settings, FileSettings

		String[] colors = {"Dark", "Light"};
		JMenu color_scheme = getMenuWithItems("color Scheme", colors, action -> {
			String target = action.getActionCommand();
			if (target.equals("Dark")) {
				//JviFrame.selected.setDarkMode();
				editors.forEach(editor -> {
					editor.setDarkMode();
				});
			} else if (target.equals("Light")) {
				editors.forEach(editor -> {
					editor.setLightMode();
				});
			}
		});
		edit.add(color_scheme);

		String[] tools_menu_items = {
			"Jump To This Tag", "Jump Back", "Build Tags File", "_",};

		String[] spelling_menu_items = {
			"Spelling Check On", "Spelling Check Off", "To Next Error", "To Previous Error",
			"Suggest Corrections"
		};

		String[] folding_menu_items = {
			"Enable/Disable Folds", "View Cursor Line", "View Cursor Line Only", "Close More Folds", "Close All Folds",
			"Open More Folds", "Open All Folds"
		};

		var tools = getMenuWithItems("Tools", tools_menu_items);
		mb.add(tools);

		var spelling = getMenuWithItems("Spelling", spelling_menu_items);
		tools.add(spelling);

		var folding = getMenuWithItems("Folding", folding_menu_items);
		tools.add(folding);

		String[] syntax_menu_items = {
			"Off", "Manual", "Automatic", "On/Off for this file"
		};

		var syntax = getMenuWithItems("Syntax", syntax_menu_items, (action) -> {
			String lang = action.getActionCommand();

			switch (lang) {
				case "Off": {
					selected.getEditor().setSyntaxEditingStyle(VideLanguages.TXT.getHighlight());
				}
			}
		});
		mb.add(syntax);

		var langs = Stream.of(VideLanguages.values()).map(lang -> lang.toString()).collect(Collectors.toList());
		Collections.sort(langs, (left, right) -> {
			return left.compareTo(right);
		});

		String[] lang_items = new String[langs.size()];
		for (int i = 0; i < langs.size(); i++) {
			lang_items[i] = langs.get(i);
		}

		var languages = getMenuWithItems("Languages", lang_items, (action) -> {
			String lang = action.getActionCommand();
			selected.getEditor().setSyntaxEditingStyle(VideLanguages.valueOf(lang).getHighlight());
		});
		syntax.add(languages, 0);

		makeBuffersMenu();

	}

	private static void makeBuffersMenu() {
		String[] buffers_menu_items = {"Refresh Menu", "Delete", "Alternate", "Next", "Previous", "_"};
		buffers = getMenuWithItems("Buffers", buffers_menu_items, (action) -> {
			String cmd = action.getActionCommand();
			if (cmd.equals("Next")) {
				selected.nextBuffer();
				selected.updateEditorFrame();
				return;
			}

			if (cmd.equals("Previous")) {
				selected.previousBuffer();
				selected.updateEditorFrame();
				return;
			}
		});
		mb.add(buffers);
		String[] window_items = {
			"New", "Split", "Split To #", "Split Vertically", "_", "Close", "Close Others", "_",
			"Move Up", "Move Down"
		};
		var window = getMenuWithItems("Window", window_items, action -> {
		});
		mb.add(window);

		var _buffers = selected.getBuffers();
		for (int i = 0; i < _buffers.size(); i++) {
			var buffer = _buffers.get(i);
			var name = buffer.getFile();
			JMenuItem item = new JMenuItem(String.format("[%s] (%d)", name == null ? "No name" : name, i + 1));
			buffers.add(item);
		}
	}

	public static void updateOpenBuffers() {
		mb.remove(buffers);
		makeBuffersMenu();
		mb.revalidate();
		mb.repaint();
	}

	private static void putMenuItems(JMenu menu, String[] items) {
		for (String itm : items) {
			if (itm.equals("_")) {
				menu.add(new JSeparator());
				continue;
			}

			JMenuItem item = new JMenuItem(itm);
			menu.add(item);
		}
	}

	private static JMenu getMenuWithItems(String name, String[] items) {
		JMenu menu = new JMenu(name);
		for (String itm : items) {
			if (itm.equals("_")) {
				menu.add(new JSeparator());
				continue;
			}

			JMenuItem item = new JMenuItem(itm);
			menu.add(item);
		}
		return menu;
	}

	private static JMenu getMenuWithItems(String name, String[] items, ActionListener l) {
		JMenu menu = new JMenu(name);
		for (String itm : items) {
			if (itm.equals("_")) {
				menu.add(new JSeparator());
				continue;
			}

			JMenuItem item = new JMenuItem(itm);
			item.addActionListener(l);
			menu.add(item);
		}
		return menu;
	}

	private void createNodes(DefaultMutableTreeNode top, File fp) {
		DefaultMutableTreeNode folder = null;
		DefaultMutableTreeNode filed = null;

		File[] files = fp.listFiles();

		for (File file : files) {
			if (file.isFile()) {
				DefaultMutableTreeNode f = new DefaultMutableTreeNode(file.getName());
				top.add(f);
			} else {
				DefaultMutableTreeNode dir = new DefaultMutableTreeNode(file.getName());
				createNodes(dir, file);
				top.add(dir);
			}
		}
	}

	public static void split(int direction, EditorPanel panel) {
		if (split_root == null || JviFrame.selected == null) {
			System.out.println("No split root or selected component.");
			return;
		}

		var focused = JviFrame.selected;

		// Traverse and add the new split pane
		boolean result = traverseAndSplit(split_root, focused, direction, panel);

		if (!result) {
			System.out.println("Failed to split: Focused editor not found in the split hierarchy.");
		}
	}

	private static boolean traverseAndSplit(JSplitPane parent, Component focused, int direction, EditorPanel panel) {
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
			JviFrame.selected = panel;
			newSplit.setRightComponent(panel); // Add the new editor on the right
			newSplit.setDividerLocation(0.6);
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
		boolean leftResult = (left instanceof JSplitPane) && traverseAndSplit((JSplitPane) left, focused, direction, panel);
		boolean rightResult = (right instanceof JSplitPane) && traverseAndSplit((JSplitPane) right, focused, direction, panel);

		return leftResult || rightResult;
	}

	public static void unsplitFocused() {
		if (split_root == null || JviFrame.selected == null) {
			System.out.println("No split root or selected component.");
			return;
		}

		// Traverse and modify the tree
		boolean result = traverseAndUnsplit(split_root, JviFrame.selected);

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
