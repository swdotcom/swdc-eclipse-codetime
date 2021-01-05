package com.swdc.codetime.tree;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import com.swdc.codetime.util.SWCoreImages;

public class MetricsTreeLabelProvider implements ILabelProvider {

	@Override
	public void addListener(ILabelProviderListener arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isLabelProperty(Object arg0, String arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Image getImage(Object node) {
		String iconName = ((MetricTreeNode)node).getIconName();
		if (iconName != null) {
			return SWCoreImages.findImage(iconName);
		}
		return null;
	}

	@Override
	public String getText(Object node) {
		if (((MetricTreeNode)node).isSeparator()) {
			return "--------------------------------";
		}
		return ((MetricTreeNode)node).getLabel();
	}

}
