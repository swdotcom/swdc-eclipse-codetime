package eclipsecodetime;

import org.eclipse.ui.IStartup;

import eclipsecodetime.util.SWCorePlugin;

public class SWBootstrap implements IStartup
{

    public void earlyStartup()
    {
        SWCorePlugin.getDefault();
    }

}
