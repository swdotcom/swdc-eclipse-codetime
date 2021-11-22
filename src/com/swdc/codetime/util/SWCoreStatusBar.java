package com.swdc.codetime.util;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.swdc.codetime.CodeTimeActivator;
import com.swdc.codetime.managers.FlowManager;

import swdc.java.ops.manager.EventTrackerManager;
import swdc.java.ops.snowplow.entities.UIElementEntity;
import swdc.java.ops.snowplow.events.UIInteractionType;

public class SWCoreStatusBar extends org.eclipse.ui.menus.WorkbenchWindowControlContribution {

	private static SWCoreStatusBar ITEM;

	private CLabel actLabel;
	private CLabel flowLabel;

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
	
	Listener flowListener = new Listener() {
		@Override
		public void handleEvent(Event event) {
			if (FlowManager.isFlowModeEnabled()) {
				FlowManager.exitFlowMode();
			} else {
				FlowManager.enterFlowMode(false);
			}
		}
	};

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
		// new Label(composite, SWT.SEPARATOR);

		actLabel = new CLabel(composite, SWT.BOLD);
		flowLabel = new CLabel(composite, SWT.BOLD);

		update();

		actLabel.addListener(SWT.MouseDown, listener);
		flowLabel.addListener(SWT.MouseDown, flowListener);

		return composite;
	}

	public void update() {
		if (actLabel != null && !actLabel.isDisposed()) {
			actLabel.setForeground(actLabel.getParent().getForeground());

			if ((text == null || text.trim().equals("")) && !iconName.equals("clock.png")) {
				text = "Code Time";
			} else {
				text = text.trim();
			}
			iconName = iconName == null || iconName.trim().equals("") ? iconName = "paw.png" : iconName;

			try {
				ImageDescriptor imgDescriptor = SWCoreImages.create("icons/", iconName);
				actLabel.setImage(SWCoreImages.getImage(imgDescriptor));
			} catch (Exception e) {
				SWCoreLog.log(e);
			}

			if (tooltip != null) {
				actLabel.setToolTipText(tooltip.trim());
			}
			actLabel.setText(text);
		}
		
		if (flowLabel != null && !flowLabel.isDisposed()) {
			String flowIcon = "dot-outlined.png";
			String flowTooltip = "Enter Flow Mode";
			if (FlowManager.isFlowModeEnabled()) {
				flowIcon = "dot.png";
				flowTooltip = "Exit Flow Mode";
			}
			try {
				ImageDescriptor imgDescriptor = SWCoreImages.create("icons/", flowIcon);
				flowLabel.setImage(SWCoreImages.getImage(imgDescriptor));
			} catch (Exception e) {
				SWCoreLog.log(e);
			}
			
			flowLabel.setText("Flow");
			flowLabel.setToolTipText(flowTooltip);
		}

		try {
			actLabel.layout(true);
			actLabel.getParent().layout(true);
		} catch (Exception e) {
			SWCoreLog.error("Unable to render the status bar text", e);
		}
		
		try {
			flowLabel.layout(true);
			flowLabel.getParent().layout(true);
		} catch (Exception e) {
			SWCoreLog.error("Unable to render the status bar flow text", e);
		}
	}

}
