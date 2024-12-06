package com.raelity.jvi.fs;

import blazing.fs.FileSystem;
import blazing.types.Result;
import com.raelity.jvi.Msg;
import com.raelity.jvi.cmd.EditorBuffer;
import com.raelity.jvi.cmd.EditorPanel;
import com.raelity.jvi.cmd.JviFrame;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JSplitPane;

/**
 *
 * @author user
 */
public class Utils {

	public static Result<Boolean, Exception> writeToFile(String path, String text) {
		File fp = new File(path);
		try {
			fp.createNewFile();
		} catch (IOException ex) {
			Msg.emsg("error: " + ex.getMessage());
			return Result.err(ex);
		}
		return FileSystem.writeToFile(fp, text);
	}

	public static void saveSelectedBuffer() {
		var selected = JviFrame.selected;
		selected.updateCurrentBuffer();
		selected.updateEditorFrame();
		int index = selected.getCurrentBufferIndex();
		var buffer = selected.getBufferByIndex(index);
		if (buffer.getFile() != null) {
			assert writeToFile(buffer.getFile(), buffer.getText()).isOk();
			return;
		}

		JFileChooser fc = new JFileChooser(".");
		SupportedFiles.setAcceptedFiles(fc);
		int result = fc.showSaveDialog(selected);
		if (result == JFileChooser.APPROVE_OPTION) {
			String path = fc.getSelectedFile().getAbsolutePath();
			assert writeToFile(path, buffer.getText()).isOk();
		}
	}

	public static void saveSelectedBufferAs() {
		var selected = JviFrame.selected;
		selected.updateCurrentBuffer();
		selected.updateEditorFrame();
		int index = selected.getCurrentBufferIndex();
		var buffer = selected.getBufferByIndex(index);
		JFileChooser fc = new JFileChooser(".");
		SupportedFiles.setAcceptedFiles(fc);
		int result = fc.showSaveDialog(selected);
		if (result == JFileChooser.APPROVE_OPTION) {
			String path = fc.getSelectedFile().getAbsolutePath();
			assert writeToFile(path, buffer.getText()).isOk();
		}
	}

	public static void chooseFile() {
		var selected = JviFrame.selected;
		JFileChooser fc = new JFileChooser(".");
		SupportedFiles.setAcceptedFiles(fc);
		int result = fc.showOpenDialog(selected);
		if (result == JFileChooser.APPROVE_OPTION) {
			String path = fc.getSelectedFile().getAbsolutePath();
			openFile(path);
		}
	}

	public static void chooseFileAndSplit() {
		var selected = JviFrame.selected;
		JFileChooser fc = new JFileChooser(".");
		SupportedFiles.setAcceptedFiles(fc);
		int result = fc.showOpenDialog(selected);
		if (result == JFileChooser.APPROVE_OPTION) {
			String path = fc.getSelectedFile().getAbsolutePath();
			var editor = EditorPanel.emplaceEditor();
			var buffer = editor.getFirstBuffer();
			buffer.setFile(new File(path).getName());
			var contents = FileSystem.readFileToString(path);
			if (contents.isErr()) { // Open a new buffer
				buffer.setText("");
			} else {
				var text = contents.unwrap();
				buffer.setText(text);
			}

			JviFrame.split(JSplitPane.HORIZONTAL_SPLIT, editor);
			editor.updateEditorFrame();
			JviFrame.updateOpenBuffers();
		}
	}

	public static void openFile(String file_path) {
		JviFrame.selected.updateCurrentBuffer();

		var first_buffer = JviFrame.selected.getFirstBuffer();
		EditorBuffer buffer = null;
		if (first_buffer.getFile() == null) {// Replace the default buffer if we still have it
			buffer = first_buffer;
		} else {
			buffer = JviFrame.selected.isBufferExists(file_path)
				? JviFrame.selected.getBufferByFile(new File(file_path).getName())
				: JviFrame.selected.emplaceBuffer();
		}

		if (buffer.getFile() == null) {
			buffer.setFile(new File(file_path).getName());
		}

		var contents = FileSystem.readFileToString(file_path);
		if (contents.isErr()) { // Open a new buffer
			buffer.setText("");
		} else {
			var text = contents.unwrap();
			buffer.setText(text);
		}

		JviFrame.selected.updateEditorFrame();
		JviFrame.updateOpenBuffers();
	}
}
