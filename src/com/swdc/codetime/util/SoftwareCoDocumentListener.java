/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved
 */
package com.swdc.codetime.util;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;

import com.swdc.codetime.CodeTimeActivator;

/**
 * 
 * Document listener to send character change events to the Activator class,
 * which then may possibly process the keystroke object if the 1 minute
 * threshold is met.
 *
 */
public class SoftwareCoDocumentListener implements IDocumentListener {

	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {
		CodeTimeActivator.handleBeforeChangeEvent(event);
	}

	@Override
	public void documentChanged(DocumentEvent event) {
		CodeTimeActivator.handleChangeEvents(event);
	}

}
