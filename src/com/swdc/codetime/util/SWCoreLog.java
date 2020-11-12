package com.swdc.codetime.util;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.swdc.codetime.CodeTimeActivator;

public class SWCoreLog
{
    private SWCoreLog()
    {
    }

    public static void log(IStatus status)
    {
        ResourcesPlugin.getPlugin().getLog().log(status);
    }

    public static void logErrorMessage(String message)
    {
        log(new Status(IStatus.ERROR, CodeTimeActivator.PLUGIN_ID, IStatus.ERROR, message, null));
    }

    public static void logInfoMessage(String message)
    {
        log(new Status(IStatus.INFO, CodeTimeActivator.PLUGIN_ID, IStatus.INFO, message, null));
    }

    public static void logWarnningMessage(String message)
    {
        log(new Status(IStatus.WARNING, CodeTimeActivator.PLUGIN_ID, IStatus.WARNING, message, null));
    }

    public static void logWarnning(Throwable e)
    {
        if (e instanceof InvocationTargetException)
        {
            e = ((InvocationTargetException) e).getTargetException();
        }

        IStatus status = null;

        if (e instanceof CoreException)
        {
            status = ((CoreException) e).getStatus();
        }
        else
        {
            status = new Status(IStatus.WARNING, CodeTimeActivator.PLUGIN_ID, IStatus.OK, e.getMessage(), e);
        }
        log(status);
    }

    public static void logException(Throwable e, final String title, String message)
    {
        if (e instanceof InvocationTargetException)
        {
            e = ((InvocationTargetException) e).getTargetException();
        }
        IStatus status = null;
        if (e instanceof CoreException)
        {
            status = ((CoreException) e).getStatus();
        }
        else
        {
            if (message == null)
                message = e.getMessage();
            if (message == null)
                message = e.toString();
            status = new Status(IStatus.ERROR, CodeTimeActivator.PLUGIN_ID, IStatus.OK, message, e);
        }
        ResourcesPlugin.getPlugin().getLog().log(status);
    }

    public static void logException(Throwable e)
    {
        logException(e, null, null);
    }

    public static void log(Throwable e)
    {
        if (e instanceof InvocationTargetException)
        {
            e = ((InvocationTargetException) e).getTargetException();
        }

        IStatus status = null;

        if (e instanceof CoreException)
        {
            status = ((CoreException) e).getStatus();
        }
        else
        {
            status = new Status(IStatus.ERROR, CodeTimeActivator.PLUGIN_ID, IStatus.OK, e.getMessage(), e);
        }

        log(status);
    }

	public static void error(String string, Exception e) {
         logErrorMessage(string);
         logException(e);
	}
}
