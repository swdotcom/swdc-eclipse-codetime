package com.swdc.codetime.util;

import org.eclipse.jface.action.LegacyActionTools;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import com.swdc.codetime.CodeTimeActivator;

import swdc.java.ops.manager.EventTrackerManager;
import swdc.java.ops.snowplow.entities.UIElementEntity;
import swdc.java.ops.snowplow.events.UIInteractionType;

public class SWCoreStatusBar extends org.eclipse.ui.menus.WorkbenchWindowControlContribution {

	private static SWCoreStatusBar ITEM;

	private CLabel label;

	Listener listener = new Listener() {
		@Override
		public void handleEvent(Event event) {
			CodeTimeActivator.displayCodeTimeMetricsTree();
			
			UIElementEntity elementEntity = new UIElementEntity();
	        elementEntity.element_name = "ct_status_bar_metrics_btn";
	        elementEntity.element_location = "ct_status_bar";
	        elementEntity.color = "blue";
	        elementEntity.cta_text = "Status bar metrics";
	        elementEntity.icon_name = "clock";
	        EventTrackerManager.getInstance().trackUIInteraction(UIInteractionType.click, elementEntity);
		}
	};

	private String errorText;

	private String errorDetail;

	private String tooltip = "";

	// initialize with a long string
	private String text = "Active Code Time";

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
		layout.wrap = false;
		composite.setLayout(layout);
		new Label(composite, SWT.SEPARATOR);

		label = new CLabel(composite, SWT.BOLD);

		update();

		label.addListener(SWT.MouseDown, listener);

		return composite;
	}

	public void update() {
		if (label != null && !label.isDisposed()) {
			label.setForeground(label.getParent().getForeground());
			if (errorText != null && !errorText.equals("")) {
				if (errorDetail != null) {
					label.setToolTipText(escape(errorDetail));
				} else if (tooltip != null) {
					label.setToolTipText(escape(tooltip));
				}
				label.setText("Code Time");
			} else {
				if ((text == null || text.trim().equals("")) && !iconName.equals("clock.png")) {
					text = "Code Time";
				} else {
					text = text.trim();
				}
			}
			iconName = iconName == null || iconName.trim().equals("") ? iconName = "paw.png" : iconName;

			try {
				// the parent layout update should handle image updates, no need to dispose
				// if (label.getImage() != null) {
				// label.getImage().dispose();
				// }
				ImageDescriptor imgDescriptor = SWCoreImages.create("icons/", iconName);
				label.setImage(SWCoreImages.getImage(imgDescriptor));
			} catch (Exception e) {
				SWCoreLog.log(e);
			}

			if (tooltip != null) {
				label.setToolTipText(tooltip.trim());
			}
			label.setText(text);
		}

		try {
			label.layout(true);
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
