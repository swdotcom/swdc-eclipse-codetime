package eclipsecodetime.util;

import org.eclipse.jface.action.LegacyActionTools;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import eclipsecodetime.Activator;

public class SWCoreStatusBar extends org.eclipse.ui.menus.WorkbenchWindowControlContribution {

	private static SWCoreStatusBar ITEM;

	private CLabel label;

	Listener listener = new Listener() {
		@Override
		public void handleEvent(Event event) {
			Activator.displayCodeTimeMetricsTree();
		}
	};

	private String errorText;

	private String errorDetail;

	private String tooltip = "";

	private String text = "                                 ";

	private String iconName;

	public SWCoreStatusBar() {
		ITEM = this;
	}

	public SWCoreStatusBar(String id) {
		super(id);
		ITEM = this;
	}

	public static SWCoreStatusBar get() {
		return ITEM;
	}

	public String getErrorText() {
		return errorText;
	}

	public void setErrorText(String errorText) {
		this.errorText = errorText;
	}

	public String getErrorDetail() {
		return errorDetail;
	}

	public void setErrorDetail(String errorDetail) {
		this.errorDetail = errorDetail;
	}

	public String getTooltip() {
		return tooltip;
	}

	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getIconName() {
		return iconName;
	}

	public void setIconName(String iconName) {
		this.iconName = iconName;
	}

	@Override
	protected Control createControl(Composite parent) {

		Composite composite = new Composite(parent, SWT.NONE);

		RowLayout layout = new RowLayout(SWT.HORIZONTAL);
		layout.marginTop = 0;
		layout.marginBottom = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		new Label(composite, SWT.SEPARATOR);

		label = new CLabel(composite, SWT.SHADOW_NONE);

		update();

		label.addListener(SWT.MouseDown, listener);

		return composite;
	}

	public void update() {
		if (label != null && !label.isDisposed()) {
			Display display = label.getDisplay();
			if (errorText != null && errorText.length() > 0) {
				label.setForeground(JFaceColors.getErrorText(display));
				label.setText(escape(errorText));
				label.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
				if (errorDetail != null)
					label.setToolTipText(escape(errorDetail));
				else if (tooltip != null)
					label.setToolTipText(escape(tooltip));
				else
					label.setToolTipText(null);

			} else {
				label.setForeground(label.getParent().getForeground());
				label.setText(escape(text));
				if (iconName != null) {
					ImageDescriptor imgDescriptor = SWCoreImages.create("icons/", iconName);
					try {
						// the parent layout update should handle image updates, no need to dispose
//						if (label.getImage() != null) {
//							label.getImage().dispose();
//						}
						label.setImage(SWCoreImages.getImage(imgDescriptor));
					} catch (Exception e) {
						SWCoreLog.log(e);
					}
				}

				if (tooltip != null)
					label.setToolTipText(escape(tooltip));
				else
					label.setToolTipText(null);
			}
		}
		try {
			label.getParent().layout(true);
		} catch (Exception e) {
			SWCoreLog.error("Unable to render the status bar text", e);
		}
	}

	private String escape(String text) {
		if (text == null)
			return text;
		return LegacyActionTools.escapeMnemonics(text);
	}

}
