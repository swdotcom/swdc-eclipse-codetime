/**
 * Copyright (c) 2018 by Software.com
 * All rights reserved
 */
package com.swdc.codetime.util;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import com.swdc.codetime.CodeTimeActivator;

/**
 * 
 * File editor listener will register the document listener once a file is
 * activated or become visible or opened.
 *
 */
public class SoftwareCoFileEditorListener implements IPartListener2 {

	private static Set<String> documentSet = new HashSet<String>();
	
	public SoftwareCoFileEditorListener(IWorkbenchPartReference partRef) {
		if (partRef != null) {
			// initialize it
			checkPart(partRef);
		}
	}

	private String checkPart(IWorkbenchPartReference partRef) {
		String fileName = null;
		IWorkbenchPart part = partRef.getPart(false /* try to restore the part */);
		if (part != null && part instanceof IEditorPart) {
			IEditorPart editor = (IEditorPart) part;
			IEditorInput input = editor.getEditorInput();

			// double check. Error Editors can also bring up this call
			if (editor instanceof ITextEditor && input instanceof FileEditorInput) {

				URI uri = ((IURIEditorInput) input).getURI();
				if (uri != null && uri.getPath() != null) {
					fileName = uri.getPath();
					SoftwareCoUtils.lastOpenFile = fileName;
					String projectName = CodeTimeActivator.getActiveProjectName(fileName);

					CodeTimeActivator.initializeKeystrokeObjectGraph(projectName, fileName);
				}

				IDocument document = (((ITextEditor) editor).getDocumentProvider()).getDocument(input);
				this.addDocumentListener(fileName, document);
			}
		}
		return fileName;
	}

	public void addDocumentListener(String fileName, IDocument document) {
		if (!documentSet.contains(fileName)) {
			documentSet.add(fileName);
			document.addDocumentListener(new SoftwareCoDocumentListener());
		}
	}

	@Override
	public void partActivated(IWorkbenchPartReference partRef) {
		this.checkPart(partRef);
	}

	@Override
	public void partBroughtToTop(IWorkbenchPartReference partRef) {
		this.checkPart(partRef);
	}

	@Override
	public void partClosed(IWorkbenchPartReference partRef) {
		String fileName = this.checkPart(partRef);
		CodeTimeActivator.handleFileClosedEvent(fileName);
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference partRef) {
		this.checkPart(partRef);
	}

	@Override
	public void partOpened(IWorkbenchPartReference partRef) {
		String fileName = this.checkPart(partRef);
		CodeTimeActivator.handleFileOpenedEvent(fileName);
	}

	@Override
	public void partHidden(IWorkbenchPartReference partRef) {
		this.checkPart(partRef);
	}

	@Override
	public void partVisible(IWorkbenchPartReference partRef) {
		this.checkPart(partRef);
	}

	@Override
	public void partInputChanged(IWorkbenchPartReference partRef) {
		this.checkPart(partRef);
	}

}