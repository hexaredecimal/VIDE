package com.raelity.jvi.cmd;

/**
 *
 * @author hexaredecimal
 */
public class EditorBuffer {
	private String file, text; 

	public EditorBuffer(String file, String text) {
		this.file = file;
		this.text = text;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
