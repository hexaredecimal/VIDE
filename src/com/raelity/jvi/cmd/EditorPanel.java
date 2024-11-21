package com.raelity.jvi.cmd;

import com.raelity.jvi.BooleanOption;
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
import java.io.InputStream;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
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

	public EditorPanel() {
		setup();
		setupVi();
	}

	public RSyntaxTextArea getEditor() {
		return editorPane;
	}

	private void setup() {
		BorderLayout borderLayout2 = new BorderLayout();
		JPanel statusPanel = new JPanel();
		generalStatusBar = new JLabel();
		strokeStatusBar = new JLabel();
		modeStatusBar = new JLabel();
		
		GridBagLayout gridBagLayout1 = new GridBagLayout();
		editorPane = new RSyntaxTextArea();
		RTextScrollPane editor_scroll;

		editorPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		editorPane.setCodeFoldingEnabled(true);
		ViManager.installKeymap(editorPane);
		setDarkMode();
		
		// Wrap it in an RTextScrollPane to enable line numbers
		editor_scroll = new RTextScrollPane(editorPane);
		editor_scroll.setLineNumbersEnabled(true); // Line numbers are enabled by default
		editor_scroll.setFoldIndicatorEnabled(true);
		editor_scroll.setIconRowHeaderEnabled(true);

		this.setLayout(borderLayout2);
		statusPanel.setLayout(gridBagLayout1);
		generalStatusBar.setText("commandInputAndGeneralStatus");

		strokeStatusBar.setMinimumSize(new Dimension(60, 21));
		strokeStatusBar.setPreferredSize(new Dimension(60, 0));
		strokeStatusBar.setText("strokes");
		modeStatusBar.setMinimumSize(new Dimension(80, 4));
		modeStatusBar.setPreferredSize(new Dimension(80, 4));
		modeStatusBar.setText("NORMAL");

		this.add(editor_scroll, BorderLayout.CENTER);
		this.add(statusPanel, BorderLayout.SOUTH);
		statusPanel.add(generalStatusBar, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 111, 0));
		statusPanel.add(strokeStatusBar, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 2, 0, 0), 0, 0));
		statusPanel.add(modeStatusBar, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
			GridBagConstraints.WEST, GridBagConstraints.VERTICAL, new Insets(0, 2, 0, 0), 0, 0));

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
		setTheme("dark");
	}

	public void setLight() {
		setTheme("light");
	}
}
