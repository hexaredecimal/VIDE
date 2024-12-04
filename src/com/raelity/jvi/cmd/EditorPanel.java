package com.raelity.jvi.cmd;

import blazing.fs.FileSystem;
import com.raelity.jvi.BooleanOption;
import com.raelity.jvi.Buffer;
import com.raelity.jvi.Normal;
import com.raelity.jvi.Options;
import com.raelity.jvi.ViManager;
import com.raelity.jvi.swing.DefaultViFactory;
import com.raelity.jvi.swing.StatusDisplay;
import com.raelity.jvi.swing.TextView;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import org.fife.rsta.ac.LanguageSupportFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextAreaEditorKit.PasteAction;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 *
 * @author hexaredecimal
 */
public class EditorPanel extends JPanel {

	private RSyntaxTextArea editorPane;
	private JLabel generalStatusBar;
	private JLabel strokeStatusBar;
	private JLabel modeStatusBar;
	private JLabel bufferName;
	private JLabel syntaxType;
	private ArrayList<EditorBuffer> buffers;
	private int selectedBuffer = 0;

	public EditorPanel() {
		setup();
		setupVi();
	}

	public RSyntaxTextArea getEditor() {
		return editorPane;
	}

	public ArrayList<EditorBuffer> getBuffers() {
		return buffers;
	}

	public void nextBuffer() {
		int buffers_size = this.buffers.size();
		selectedBuffer = ++selectedBuffer % buffers_size;
	}

	public void previousBuffer() {
		int buffers_size = this.buffers.size();
		selectedBuffer = --selectedBuffer % buffers_size;
		if (selectedBuffer < 0) {
			selectedBuffer = buffers_size - 1;
		}
	}

	public void updateCurrentBuffer() {
		this.buffers.get(this.selectedBuffer).setText(editorPane.getText());
	}

	public EditorBuffer currentBuffer() {
		return this.buffers.get(this.selectedBuffer);
	}

	public boolean isCurrentBufferDirty() {
		String editor_text = editorPane.getText();
		String buffer_text = this.buffers.get(this.selectedBuffer).getText();
		return !buffer_text.equals(editor_text);
	}

	public void setCurrentBufferText(String text) {
		this.buffers.get(this.selectedBuffer).setText(text);
	}

	public void setCurrentBufferFile(String file) {
		this.buffers.get(this.selectedBuffer).setFile(file);
	}

	public EditorBuffer emplaceBuffer() {
		buffers.add(new EditorBuffer(null, null));
		this.nextBuffer();
		return this.currentBuffer();
	}

	public void updateBufferStatus() {
		this.bufferName.setText(this.buffers.get(this.selectedBuffer).getFile());
	}

	public void updateEditorFrame() {
		this.updateBufferStatus();
		this.setSyntaxFromBufferName();
		this.editorPane.setText(this.currentBuffer().getText());
	}

	private void setSyntaxFromBufferName() {
		String buffer = this.buffers.get(this.selectedBuffer).getFile();
		if (buffer == null) {
			editorPane.setSyntaxEditingStyle(VideLanguages.fromFileType("txt").getHighlight());
			return;
		}
		var ext = FileSystem.fileExtension(buffer);
		String type = ext == null ? "txt" : ext;
		this.setSyntaxTypeLabel(type);
		editorPane.setSyntaxEditingStyle(VideLanguages.fromFileType(type).getHighlight());
	}

	public boolean isBufferExists(String path) {
		for (var buffer : this.buffers) {
			if (buffer.getText().equals(path)) {
				return true;
			}
		}
		return false;
	}

	public EditorBuffer getBufferByFile(String path) {
		for (var buffer : this.buffers) {
			if (buffer.getText().equals(path)) {
				return buffer;
			}
		}
		return null;
	}

	public EditorBuffer getBufferByIndex(int index) {
		return this.buffers.get(index);
	}

	public int getCurrentBufferIndex() {
		return selectedBuffer;
	}

	public EditorBuffer getFirstBuffer() {
		return this.buffers.get(0);
	}

	public void replaceFirstBuffer(EditorBuffer buffer) {
		this.buffers.set(0, buffer);
	}

	public void replaceBuffer(EditorBuffer buffer, int index) {
		this.buffers.set(index, buffer);
	}

	private void setSyntaxTypeLabel(String type) {
		this.syntaxType.setText(type);
	}

	private void setup() {
		buffers = new ArrayList<>();
		buffers.add(new EditorBuffer(null, ""));
		BorderLayout borderLayout2 = new BorderLayout();
		generalStatusBar = new JLabel();
		strokeStatusBar = new JLabel();
		modeStatusBar = new JLabel();
		bufferName = new JLabel();
		syntaxType = new JLabel();

		GridBagLayout gridBagLayout1 = new GridBagLayout();
		editorPane = new RSyntaxTextArea();
		LanguageSupportFactory.get().register(editorPane);
		RTextScrollPane editor_scroll;

		//editorPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_GO);
		editorPane.setCodeFoldingEnabled(true);
		editorPane.setTabSize(2);
		
		ViManager.installKeymap(editorPane);
		setDarkMode();

		// Wrap it in an RTextScrollPane to enable line numbers
		editor_scroll = new RTextScrollPane(editorPane);
		editor_scroll.setLineNumbersEnabled(true); // Line numbers are enabled by default
		editor_scroll.setFoldIndicatorEnabled(true);
		editor_scroll.setIconRowHeaderEnabled(true);

		this.setLayout(borderLayout2);

		syntaxType.setText("txt");
		bufferName.setText("buffer");
		generalStatusBar.setText("");
		strokeStatusBar.setText("");
		modeStatusBar.setText("NORMAL");

		this.add(editor_scroll, BorderLayout.CENTER);
		// Create the toolbar
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false); // Make the toolbar fixed

		// Create the left label
		// Create the two right labels
		// Use a Box for right alignment of the labels
		Box rightBox = Box.createHorizontalBox();
		rightBox.add(Box.createHorizontalGlue()); // Push components to the right
		rightBox.add(generalStatusBar);
		rightBox.add(Box.createHorizontalStrut(10)); // Add spacing between labels
		rightBox.add(strokeStatusBar);
		this.add(toolBar, BorderLayout.SOUTH);


		Box leftBox = Box.createHorizontalBox();
		leftBox.add(modeStatusBar);
		leftBox.add(Box.createHorizontalStrut(10)); // Add spacing between labels
		leftBox.add(bufferName);
		leftBox.add(Box.createHorizontalStrut(10)); // Add spacing between labels
		leftBox.add(syntaxType);

		// Add the components to the toolbar
		toolBar.add(leftBox); // Left-aligned label
		toolBar.add(Box.createHorizontalGlue()); // Filler to push subsequent components to the right
		toolBar.add(rightBox);
		
		if (JviFrame.selected == null) 
			JviFrame.selected = this;

		editorPane.addCaretListener((event) -> {
			try {
				int caretPos = editorPane.getCaretPosition();
				int line = editorPane.getLineOfOffset(caretPos) + 1; // Line numbers are 0-based
				int column = caretPos - editorPane.getLineStartOffset(line - 1) + 1; // Column is 0-based
				generalStatusBar.setText(String.format("%d:%d", line, column));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});

		var pnl = this;
		editorPane.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent fe) {
				JviFrame.selected = pnl;
				JviFrame.updateOpenBuffers();
			}

			@Override
			public void focusLost(FocusEvent fe) {
			}
		});
	}

	private void setupVi() {

		// NEEDSWORK: editor is drawn, do rest in dispatch thread
		// f.editorPane.setCaretColor(Color.black);
		ViManager.registerEditorPane(editorPane);
		((BooleanOption) Options.getOption(Options.dbgKeyStrokes)).setBoolean(true);

		/*
    Font font = editorPane.getFont();
    editorPane.setFont(new Font("Monospaced",
				      font.getStyle(),
				      font.getSize()));
    font = editorPane.getFont();
    FontMetrics fm = editorPane.getFontMetrics(font);
    int width = fm.charWidth(' ') * 81;
    int height = fm.getHeight() * 40;
    editorPane.setSize(width, height);
		 */
		TextView tv = (TextView) ViManager.getViTextView(editorPane);
		StatusDisplay sd = (StatusDisplay) tv.getStatusDisplay();
		sd.generalStatus = generalStatusBar;
		sd.strokeStatus = strokeStatusBar;
		sd.modeStatus = modeStatusBar;
		sd.displayMode("normal".toUpperCase());
// G.setEditor(new TextView(editorPane, sd));

		// add a mouse listener so that selection by mouse events is treated as visual mode as well
		editorPane.addMouseMotionListener(new MouseMotionListener() {
			public void mouseMoved(MouseEvent e) {
			}

			public void mouseDragged(MouseEvent e) {
				ViManager.mouseMoveDot(editorPane.getCaret().getDot(), editorPane);
			}
		});
		Action[] actions = editorPane.getActions();
		for (int i = 0; i < actions.length; i++) {
			if (actions[i] instanceof PasteAction) {
				actions[i].setEnabled(false);
			}
		}
		editorPane.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {
				strokeStatusBar.setText("" + e.getKeyChar());
				if (e.getKeyChar() == 'v' && e.getModifiers() == KeyEvent.CTRL_MASK) {
					Normal.normal_cmd(0x1f & e.getKeyChar(), true);
				}

				String buffer_text = buffers.get(selectedBuffer).getText();
				String text = editorPane.getText();
				boolean insert_mode = modeStatusBar.getText().equals("INSERT");
				if (insert_mode) {
					if (buffer_text == null) {
						buffers.get(selectedBuffer).setText(text);
					} else if (!buffer_text.equals(text)) {
						System.out.println("Buffer add: " + text);
						buffers.get(selectedBuffer).setText(text);
					}
				}

			}

			public void keyPressed(KeyEvent e) {
			}

			public void keyReleased(KeyEvent e) {
			}

		});
	}

	public void setTheme(String theme) {
		try (InputStream is = RSyntaxTextArea.class.getResourceAsStream(String.format("/org/fife/ui/rsyntaxtextarea/themes/%s.xml", theme))) {
			Theme _theme = Theme.load(is);
			_theme.apply(editorPane);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setDarkMode() {
		FlatDarkLaf.setup();
		FlatDarkLaf.updateUI();
		setTheme("monokai");
	}

	public void setLightMode() {
		FlatLightLaf.setup();
		FlatLightLaf.updateUI();
		setTheme("vs");
	}
}
