package com.swdc.codetime.managers;

import java.net.URL;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import com.swdc.codetime.util.SWCoreImages;
import com.swdc.codetime.util.SWCoreLog;

public class InfoPreferencePage extends PreferencePage implements IWorkbenchPreferencePage
{

    public void init(IWorkbench workbench)
    {
        noDefaultAndApplyButton();
    }

    @Override
    protected Control createContents(Composite parent)
    {

        Composite base = new Composite(parent, SWT.NULL);
        base.setLayout(new GridLayout());
        base.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Composite summary = new Composite(base, SWT.NULL);
        summary.setLayout(new GridLayout(2, false));
        summary.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Label ejIcon = new Label(summary, SWT.NONE);
        ejIcon.setAlignment(SWT.RIGHT);
        Label lblInfo = new Label(summary, SWT.NONE);
        ejIcon.setImage(SWCoreImages.getImage(SWCoreImages.DESC_SW_ICON));

        new Label(summary, SWT.NONE);
        Link link = new Link(summary, SWT.NONE);
        link.setText("<A>www.software.com</A>");
        link.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent evet)
            {
                try
                {
                    PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL("https://www.software.com/eclipse"));
                }
                catch (Exception e)
                {
                    SWCoreLog.log(e);
                }
            }
        });

        lblInfo.setText("Software \n" + "Copyright (c) 2018\n");
        return base;
    }

}
