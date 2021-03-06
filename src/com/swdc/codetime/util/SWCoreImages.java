package com.swdc.codetime.util;

import java.net.URL;

import javax.swing.ImageIcon;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

import com.swdc.codetime.CodeTimeActivator;

public class SWCoreImages
{
    private SWCoreImages()
    {
    }

    public final static String          ICONS_PATH      = "icons/"; //$NON-NLS-1$
    private final static ImageRegistry  PLUGIN_REGISTRY = new ImageRegistry();
    public static final ImageDescriptor DESC_SW_ICON    = create(ICONS_PATH, "sw.png");
    
    public static Image findImage(String name) {
    	return getImage(ImageDescriptor.createFromURL(makeImageURL(ICONS_PATH, name)));
    }

    public static ImageDescriptor create(String prefix, String name)
    {
        return ImageDescriptor.createFromURL(makeImageURL(prefix, name));
    }

    private static URL makeImageURL(String prefix, String name)
    {
        String path = "$nl$/" + prefix + name; //$NON-NLS-1$
    	// String path = "platform:/plugin/som.swdc.codetime/" + prefix + name;
        return FileLocator.find(CodeTimeActivator.getDefault().getBundle(), new Path(path), null);
    }

    public static Image getImage(ImageDescriptor desc)
    {
        String key = String.valueOf(desc.hashCode());
        Image image = PLUGIN_REGISTRY.get(key);
        if (image == null)
        {
            image = desc.createImage();
            PLUGIN_REGISTRY.put(key, image);
        }
        return image;
    }
    
    public static ImageIcon getImageIcon(String name) {
    	return new ImageIcon(makeImageURL(ICONS_PATH, name));
    }
    
}
