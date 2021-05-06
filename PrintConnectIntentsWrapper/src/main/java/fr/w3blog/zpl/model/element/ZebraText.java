package fr.w3blog.zpl.model.element;

import fr.w3blog.zpl.constant.ZebraFont;
import fr.w3blog.zpl.constant.ZebraRotation;
import fr.w3blog.zpl.model.PrinterOptions;
import fr.w3blog.zpl.model.ZebraElement;
import fr.w3blog.zpl.utils.ZplUtils;

/**
 * Zebra element to add Text to specified position.
 * 
 * @author ttropard
 * 
 */
public class ZebraText extends ZebraElement {

	ZebraFont zebraFont = null;

	/**
	 * Explain Font Size (11,13,14).
	 * Not in dots.
	 */
	Integer fontSize = null;

	ZebraRotation zebraRotation = ZebraRotation.NORMAL;

	String text;

	public ZebraText(String text) {
		this.text = text;
	}

	public ZebraText(String text, int fontSize) {
		this.fontSize = fontSize;
		this.text = text;
	}

	public ZebraText(String text, ZebraFont zebraFont, int fontSize) {
		this.zebraFont = zebraFont;
		this.fontSize = fontSize;
		this.text = text;
	}

	public ZebraText(String text, ZebraFont zebraFont, int fontSize, ZebraRotation zebraRotation) {
		this.zebraFont = zebraFont;
		this.zebraRotation = zebraRotation;
		this.fontSize = fontSize;
		this.text = text;
	}

	public ZebraText(int positionX, int positionY, String text) {
		this.text = text;
		this.positionX = positionX;
		this.positionY = positionY;
	}

	public ZebraText(int positionX, int positionY, String text, int fontSize) {
		this.fontSize = fontSize;
		this.text = text;
		this.positionX = positionX;
		this.positionY = positionY;
	}

	public ZebraText(int positionX, int positionY, String text, ZebraFont zebraFont, int fontSize, ZebraRotation zebraRotation) {
		this.zebraFont = zebraFont;
		this.fontSize = fontSize;
		this.zebraRotation = zebraRotation;
		this.text = text;
		this.positionX = positionX;
		this.positionY = positionY;
	}

	public ZebraText(int positionX, int positionY, String text, ZebraFont zebraFont, int fontSize) {
		this.zebraFont = zebraFont;
		this.fontSize = fontSize;
		this.text = text;
		this.positionX = positionX;
		this.positionY = positionY;
	}

	/* (non-Javadoc)
	 * @see fr.w3blog.zpl.model.element.ZebraElement#getZplCode(fr.w3blog.zpl.model.PrinterOptions)
	 */
	@Override
	public String getZplCode(PrinterOptions printerOptions) {
		StringBuffer zpl = new StringBuffer();
		zpl.append(this.getZplCodePosition());

		if (fontSize != null && zebraFont != null) {
			//This element has specified size and font
			Integer[] dimension = ZplUtils.extractDotsFromFont(zebraFont, fontSize, printerOptions.getZebraPPP());
			zpl.append(ZplUtils.zplCommand("A", zebraFont.getLetter() + zebraRotation.getLetter(), dimension[0], dimension[1]));
		} else if (fontSize != null && printerOptions.getDefaultZebraFont() != null) {
			//This element has specified size, but with default font
			Integer[] dimension = ZplUtils.extractDotsFromFont(printerOptions.getDefaultZebraFont(), fontSize, printerOptions.getZebraPPP());
			zpl.append(ZplUtils.zplCommand("A", printerOptions.getDefaultZebraFont().getLetter() + zebraRotation.getLetter(), dimension[0], dimension[1]));
		}

		zpl.append("^FH\\^FD");//We allow hexadecimal and start element
		zpl.append(ZplUtils.convertAccentToZplAsciiHexa(text));
		zpl.append(ZplUtils.zplCommandSautLigne("FS"));

		return zpl.toString();
	}
}
