package com.swdc.codetime.util;

import org.eclipse.osgi.util.NLS;

public class SWCoreMessages extends NLS
{
    private static final String BUNDLE_NAME = "com.swdc.ide.core.resources"; //$NON-NLS-1$

    // public static String PreferencesPage_summary;
    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, SWCoreMessages.class);
    }

    private SWCoreMessages()
    {
    }

}
