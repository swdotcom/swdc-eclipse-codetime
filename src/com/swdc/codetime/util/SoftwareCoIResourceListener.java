package com.swdc.codetime.util;

import java.util.logging.Logger;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;

public class SoftwareCoIResourceListener implements IResourceChangeListener {
	public static final Logger LOG = Logger.getLogger("SoftwareCoIResourceListener");
	
    public void resourceChanged(IResourceChangeEvent event) {
        switch (event.getType()) {
           case IResourceChangeEvent.POST_CHANGE:
              try {
            	  event.getDelta().accept(new SoftwareCoDeltaPrinter());
              } catch (Exception e) {
            	  LOG.warning("Error listening for resource post change event: " + e.getMessage());
              }
              break;
        }
    }
}
