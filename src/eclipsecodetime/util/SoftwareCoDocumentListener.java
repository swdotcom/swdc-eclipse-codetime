/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved
 */
package eclipsecodetime.util;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;

import eclipsecodetime.Activator;

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
		//
	}

	@Override
	public void documentChanged(DocumentEvent event) {
		Activator.handleChangeEvents(event);
	}

}
