/* Copyright (C) 2013-2015 BMW Group
 * Author: Manfred Bathelt (manfred.bathelt@bmw.de)
 * Author: Juergen Gehring (juergen.gehring@bmw.de)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package de.bmw.yamaica.ide.ui.internal.dialogs;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;

import de.bmw.yamaica.common.ui.utils.ExtendedStringFieldEditor;
import de.bmw.yamaica.ide.ui.internal.Activator;
import de.bmw.yamaica.ide.ui.internal.preferences.Preferences;

public class ProjectSettingsPage extends WizardPage
{
    private IPreferenceStore baseStore = de.bmw.yamaica.common.ui.Preferences.getPreferenceStore();
    private IPreferenceStore store     = Activator.getDefault().getPreferenceStore();
    private ExtendedStringFieldEditor importDirectoryFieldEditor, targetDirectoryFieldEditor;
    private BooleanFieldEditor        createEditorLinkFieldEditor;

    ProjectSettingsPage(IWorkbench workbench, IStructuredSelection structuredSelection)
    {
        super("yamaica import page");

        setTitle("yamaica Project Settings");
        setMessage("Define the yamaica project settings.");
    }

    @Override
    public void createControl(Composite parent)
    {
        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.marginWidth = gridLayout.marginHeight = 0;
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(gridLayout);

        Group projectSettings = new Group(composite, SWT.NONE);
        projectSettings.setText("Project settings");
        projectSettings.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        importDirectoryFieldEditor = new ExtendedStringFieldEditor(de.bmw.yamaica.common.ui.Preferences.IMPORT_FOLDER, "Import folder:",
                projectSettings);
        importDirectoryFieldEditor.setPreferenceStore(baseStore);
        importDirectoryFieldEditor.setPage(this);
        importDirectoryFieldEditor.setValidationPattern(ExtendedStringFieldEditor.getWorkbenchPathPattern());
        importDirectoryFieldEditor.setErrorMessage("String is not a valid workbench path.");
        importDirectoryFieldEditor.setPropertyChangeListener(new IPropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent event)
            {
                boolean b = validatePage();
                setPageComplete(b);
            }
        });
        importDirectoryFieldEditor.fillIntoGrid(projectSettings, 2);
        importDirectoryFieldEditor.load();

        targetDirectoryFieldEditor = new ExtendedStringFieldEditor(de.bmw.yamaica.common.ui.Preferences.TARGET_FOLDER, "Target folder:",
                projectSettings);
        targetDirectoryFieldEditor.setPreferenceStore(baseStore);
        targetDirectoryFieldEditor.setPage(this);
        targetDirectoryFieldEditor.setValidationPattern(ExtendedStringFieldEditor.getWorkbenchPathPattern());
        targetDirectoryFieldEditor.setErrorMessage("String is not a valid workbench path.");
        targetDirectoryFieldEditor.setPropertyChangeListener(new IPropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent event)
            {
                boolean b = validatePage();
                setPageComplete(b);
            }
        });
        targetDirectoryFieldEditor.fillIntoGrid(projectSettings, 2);
        targetDirectoryFieldEditor.load();

        createEditorLinkFieldEditor = new BooleanFieldEditor(Preferences.CREATE_YAMAICA_EDITOR_LINK, "Create yamaica editor link",
                projectSettings);
        createEditorLinkFieldEditor.setPreferenceStore(store);
        createEditorLinkFieldEditor.setPage(this);
        createEditorLinkFieldEditor.fillIntoGrid(projectSettings, 2);
        createEditorLinkFieldEditor.load();

        projectSettings.setLayout(new GridLayout(2, false));

        setControl(composite);
    }

    String getImportDirectoryPath()
    {
        if (null != importDirectoryFieldEditor)
        {
            return importDirectoryFieldEditor.getStringValue();
        }

        return baseStore.getString(de.bmw.yamaica.common.ui.Preferences.IMPORT_FOLDER);
    }

    String getTargetDirectoryPath()
    {
        if (null != targetDirectoryFieldEditor)
        {
            return targetDirectoryFieldEditor.getStringValue();
        }

        return baseStore.getString(de.bmw.yamaica.common.ui.Preferences.TARGET_FOLDER);
    }

    boolean getCreateEditorLink()
    {
        if (null != createEditorLinkFieldEditor)
        {
            return createEditorLinkFieldEditor.getBooleanValue();
        }

        return store.getBoolean(Preferences.CREATE_YAMAICA_EDITOR_LINK);
    }

    private boolean validatePage()
    {
        return importDirectoryFieldEditor.isValid() && targetDirectoryFieldEditor.isValid();
    }
}
