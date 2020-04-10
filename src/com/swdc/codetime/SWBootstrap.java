package com.swdc.codetime;

import org.eclipse.ui.IStartup;

import com.swdc.codetime.util.SWCorePlugin;

public class SWBootstrap implements IStartup
{

    public void earlyStartup()
    {
        SWCorePlugin.getDefault();
    }

}
