package eclipsecodetime.util;

//import java.net.URI;
//import java.net.URISyntaxException;

import org.apache.http.client.methods.HttpDelete;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
//import org.eclipse.equinox.internal.p2.ui.ProvUI;
//import org.eclipse.equinox.internal.p2.ui.ProvUIActivator;
//import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
//import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
//import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import eclipsecodetime.Activator;

public class SWCorePlugin extends AbstractUIPlugin implements IStartup
{
    private BundleContext       bundleContext;
    public static final String  ID = "com.swdc.ide.core";
    private static final String UPDATE_SITE_URL = "TODO";

    private static SWCorePlugin plugin;
    
    private Activator softwareCo;

    public SWCorePlugin()
    {
        plugin = this;
    }

    public void start(BundleContext context) throws Exception
    {

        super.start(context);
        this.bundleContext = context;
        //TODO: add alt update site
        //setupUpdatSite();
        
        softwareCo = new Activator();

        softwareCo.start(context);
    }

    public void stop(BundleContext context) throws Exception
    {
        super.stop(context);
        
        softwareCo.stop(context);
        softwareCo = null;
        plugin = null;
    }

    public BundleContext getBundleContext()
    {
        return bundleContext;
    }
    
    /**
     * This will update the Software.com site that the eclipse plugin has been uninstalled
     */
    public void uninstallPluginFromApp() {
    	SoftwareResponse responseInfo = SoftwareCoUtils.makeApiCall(
                		"/integrations/" + SoftwareCoUtils.pluginId, HttpDelete.METHOD_NAME, null);
        if (responseInfo != null && responseInfo.isOk()) {
        	SWCoreLog.logInfoMessage("Code Time: successfully uninstalled");
        }
    }

    /**
     * Returns the shared instance.
     */
    public static SWCorePlugin getDefault()
    {
        return plugin;
    }

    public void earlyStartup()
    {
        // ignore
    }

    public static String getID()
    {
        return getDefault().getBundle().getSymbolicName();
    }

    public static Display getStandardDisplay()
    {
        Display display;
        display = Display.getCurrent();
        if (display == null)
            display = Display.getDefault();
        return display;
    }

    public static IWorkspace getWorkspace()
    {
        return ResourcesPlugin.getWorkspace();
    }

    public static IWorkbenchWindow getActiveWorkbenchWindow()
    {
        return getDefault().getWorkbench().getActiveWorkbenchWindow();
    }
    
    
//    @SuppressWarnings("restriction")
//    void setupUpdatSite()
//    {
//       
//        try
//        {
//            final ProvisioningUI ui = ProvUIActivator.getDefault().getProvisioningUI();
//            IArtifactRepositoryManager artifactManager = ProvUI.getArtifactRepositoryManager(ui.getSession());
//            artifactManager.addRepository(new URI(UPDATE_SITE_URL));
//
//            IMetadataRepositoryManager metadataManager = ProvUI.getMetadataRepositoryManager(ui.getSession());
//            metadataManager.addRepository(new URI(UPDATE_SITE_URL));
//        }
//        catch (URISyntaxException e)
//        {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }

}
