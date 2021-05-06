package fr.w3blog.zpl.model;

import java.util.ArrayList;
import java.util.List;

import fr.w3blog.zpl.constant.ZebraFont;
import fr.w3blog.zpl.constant.ZebraPaperType;
import fr.w3blog.zpl.constant.ZebraPrintMode;
import fr.w3blog.zpl.utils.ZplUtils;

public class ZebraLabel {

	/**
	 * Width explain in dots
	 */
	private Integer widthDots;
	/**
	 * Height explain in dots
	 */
	private Integer heightDots;

	private ZebraPrintMode zebraPrintMode = ZebraPrintMode.TEAR_OFF;

	private ZebraPaperType zebraPaperType = ZebraPaperType.CONTINUOUS;

	private PrinterOptions printerOptions = new PrinterOptions();

	private List<ZebraElement> zebraElements = new ArrayList<ZebraElement>();

	public ZebraLabel() {
		super();
		zebraElements = new ArrayList<ZebraElement>();
	}

	public ZebraLabel(PrinterOptions printerOptions) {
		super();
		this.printerOptions = printerOptions;
	}

	/**
	 * Create label with size
	 * 
	 * @param heightDots
	 *            height explain in dots
	 * @param widthDots
	 *            width explain in dots
	 */
	public ZebraLabel(int widthDots, int heightDots) {
		super();
		this.widthDots = widthDots;
		this.heightDots = heightDots;
	}

	/**
	 * 
	 * @param heightDots
	 *            height explain in dots
	 * @param widthDots
	 *            width explain in dots
	 * @param printerOptions
	 */
	public ZebraLabel(int widthDots, int heightDots, PrinterOptions printerOptions) {
		super();
		this.widthDots = widthDots;
		this.heightDots = heightDots;
		this.printerOptions = printerOptions;
	}

	/**
	 * Function to add Element on etiquette.
	 * 
	 * Element is abstract, you should use one of child Element( ZebraText, ZebraBarcode, etc)
	 * 
	 * @param zebraElement
	 * @return
	 */
	public ZebraLabel addElement(ZebraElement zebraElement) {
		zebraElements.add(zebraElement);
		return this;
	}

	/**
	 * Use to define a default Zebra font on the label
	 * 
	 * @param defaultZebraFont
	 *            the defaultZebraFont to set
	 */
	public ZebraLabel setDefaultZebraFont(ZebraFont defaultZebraFont) {
		printerOptions.setDefaultZebraFont(defaultZebraFont);
		return this;
	}

	/**
	 * Use to define a default Zebra font size on the label (11,13,14).
	 * Not explain in dots (convertion is processed by library)
	 * 
	 * @param defaultFontSize
	 *            the defaultFontSize to set
	 */
	public ZebraLabel setDefaultFontSize(Integer defaultFontSize) {
		printerOptions.setDefaultFontSize(defaultFontSize);
		return this;
	}

	public Integer getWidthDots() {
		return widthDots;
	}

	public ZebraLabel setWidthDots(Integer widthDots) {
		this.widthDots = widthDots;
		return this;
	}

	public Integer getHeightDots() {
		return heightDots;
	}

	public ZebraLabel setHeightDots(Integer heightDots) {
		this.heightDots = heightDots;
		return this;
	}

	public PrinterOptions getPrinterOptions() {
		return printerOptions;
	}

	public void setPrinterOptions(PrinterOptions printerOptions) {
		this.printerOptions = printerOptions;
	}

	/**
	 * @return the zebraPrintMode
	 */
	public ZebraPrintMode getZebraPrintMode() {
		return zebraPrintMode;
	}

	/**
	 * @param zebraPrintMode
	 *            the zebraPrintMode to set
	 */
	public ZebraLabel setZebraPrintMode(ZebraPrintMode zebraPrintMode) {
		this.zebraPrintMode = zebraPrintMode;
		return this;
	}

	/**
	 * @return the zebraPrintMode
	 */
	public ZebraPaperType getZebraPaperType() {
		return zebraPaperType;
	}

	/**
	 * @param zebraPaperType
	 *            the zebraPaperType to set
	 */
	public ZebraLabel setZebraPaperType(ZebraPaperType zebraPaperType) {
		this.zebraPaperType = zebraPaperType;
		return this;
	}

	/**
	 * @return the zebraElements
	 */
	public List<ZebraElement> getZebraElements() {
		return zebraElements;
	}

	/**
	 * @param zebraElements
	 *            the zebraElements to set
	 */
	public void setZebraElements(List<ZebraElement> zebraElements) {
		this.zebraElements = zebraElements;
	}

	public String getZplCode() {
		StringBuilder zpl = new StringBuilder();
        //zpl.append("CT~~CD,~CC^~CT~"); //Put printer in ZPL Mode (necessary if it was previously set in Line Mode)
		zpl.append(ZplUtils.zplCommandSautLigne("XA"));//Start Label
		zpl.append(zebraPrintMode.getZplCode());
		zpl.append(zebraPaperType.getZplCode());

		if (widthDots != null) {
			//Define width for label
			zpl.append(ZplUtils.zplCommandSautLigne("PW", widthDots));
		}

		if (heightDots != null) {
			zpl.append(ZplUtils.zplCommandSautLigne("LL", heightDots));
		}

		//Default Font and Size
		if (printerOptions.getDefaultZebraFont() != null && printerOptions.getDefaultFontSize() != null) {
			zpl.append(ZplUtils.zplCommandSautLigne("CF", (Object[]) ZplUtils.extractDotsFromFont(printerOptions.getDefaultZebraFont(), printerOptions.getDefaultFontSize(), printerOptions.getZebraPPP())));
		}

		for (ZebraElement zebraElement : zebraElements) {
			zpl.append(zebraElement.getZplCode(printerOptions));
		}
		zpl.append(ZplUtils.zplCommandSautLigne("XZ"));//End Label
		return zpl.toString();
	}
}
