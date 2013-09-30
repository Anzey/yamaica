/* Copyright (C) 2013 BMW Group
 * Author: Manfred Bathelt (manfred.bathelt@bmw.de)
 * Author: Juergen Gehring (juergen.gehring@bmw.de)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package de.bmw.yamaica.base.ui.dialogs;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardExportResourcesPage;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import de.bmw.yamaica.base.core.resourceproperties.IResourcePropertyStore;
import de.bmw.yamaica.base.core.resourceproperties.YamaicaXmlModel;
import de.bmw.yamaica.base.ui.utils.ActionRunEvent;
import de.bmw.yamaica.base.ui.utils.ActionRunListener;
import de.bmw.yamaica.base.ui.utils.ResourceComparator;
import de.bmw.yamaica.base.ui.utils.ResourceExtensionFilter;
import de.bmw.yamaica.base.ui.utils.ViewerToolBar;

public abstract class YamaicaWizardExportPage extends WizardExportResourcesPage implements ICheckStateListener, ActionRunListener
{
    protected IWorkbench               workbench;
    protected IStructuredSelection     structuredSelection;
    protected boolean                  restrictWizardPage                = false;

    // widgets
    protected ViewerToolBar            viewerToolBar;
    protected YamaicaCheckedTreeViewer resourceSelectionTreeViewer;
    protected Combo                    destinationNameField;
    protected Button                   destinationBrowseButton;
    protected Button                   overwriteExistingFilesCheckbox;

    protected String[]                 fileExtensions                    = new String[0];
    protected ResourceExtensionFilter  extensionFilter;

    // dialog store id constants
    protected static final String      STORE_DESTINATION_NAMES_ID        = "YamaicaWizardExportPage.STORE_DESTINATION_NAMES_ID";       //$NON-NLS-1$
    protected static final String      STORE_OVERWRITE_EXISTING_FILES_ID = "YamaicaWizardExportPage.STORE_OVERWRITE_EXISTING_FILES_ID"; //$NON-NLS-1$
    protected static final String      STORE_SHOW_ALL_FILES_ID           = "YamaicaWizardImportPage.STORE_SHOW_ALL_FILES_ID";          //$NON-NLS-1$

    public YamaicaWizardExportPage(IWorkbench workbench, IStructuredSelection structuredSelection, String name)
    {
        super(name, structuredSelection);

        this.workbench = workbench;
        this.structuredSelection = structuredSelection;
    }

    @Override
    public void createControl(Composite parent)
    {
        initializeDialogUnits(parent);

        String[] fileExtensions = getFileExtensions();

        if (null != fileExtensions)
        {
            this.fileExtensions = fileExtensions;
            this.extensionFilter = new ResourceExtensionFilter(fileExtensions);
        }

        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
        composite.setFont(parent.getFont());

        createSourceGroup(composite);
        createDestinationGroup(composite);
        createOptionsGroup(composite);

        restoreResourceSpecificationWidgetValues(); // ie.- local
        restoreWidgetValues(); // ie.- subclass hook

        updateWidgetEnablements();
        setPageComplete(determinePageCompletion());
        setErrorMessage(null); // should not initially have error message

        setControl(composite);

        destinationNameField.setFocus();

        // TODO Help
        // PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), "");
    }

    protected void createSourceGroup(Composite parent)
    {
        createResourceSelectionGroup(parent);
    }

    protected void createResourceSelectionGroup(Composite parent)
    {
        GridData data = new GridData(GridData.FILL_BOTH);
        data.heightHint = 240;

        viewerToolBar = new ViewerToolBar(parent, SWT.BORDER, ViewerToolBar.DRILL_DOWN | ViewerToolBar.SELECT | ViewerToolBar.FILTER);
        viewerToolBar.setLayoutData(data);
        viewerToolBar.setFilterText("Filter File Extensions");

        resourceSelectionTreeViewer = new YamaicaCheckedTreeViewer(viewerToolBar, SWT.NONE);
        resourceSelectionTreeViewer.setContentProvider(new WorkbenchContentProvider());
        resourceSelectionTreeViewer.setLabelProvider(new WorkbenchLabelProvider());
        resourceSelectionTreeViewer.setUseHashlookup(true);
        resourceSelectionTreeViewer.addCheckStateListener(this);
        resourceSelectionTreeViewer.setComparator(new ResourceComparator());
        resourceSelectionTreeViewer.addFilter(extensionFilter);

        viewerToolBar.addActionRunListener(this);
        viewerToolBar.setViewer(resourceSelectionTreeViewer);

        Object viewerInput = ResourcesPlugin.getWorkspace().getRoot();

        if (restrictWizardPage && !structuredSelection.isEmpty())
        {
            Object firstSelectedElement = structuredSelection.getFirstElement();

            if (firstSelectedElement instanceof IResource)
            {
                // We have to check if all initially selected files are inside the import or inside the target folder.
                // If all initially selected files are inside of one of these folder we can restrict the tree viewer
                // to this container.
                IProject project = ((IResource) firstSelectedElement).getProject();

                YamaicaXmlModel model = YamaicaXmlModel.acquireInstance(project, this);
                IResourcePropertyStore store = model.getResourcePropertyStore(project);
                // TODO
                // String importFolder = store.getProperty(IResourcePropertyStore.IMPORT_FOLDER, Preferences.getPreferenceProvider()
                // .getDefaultString(IResourcePropertyStore.IMPORT_FOLDER));
                // String targetFolder = store.getProperty(IResourcePropertyStore.TARGET_FOLDER, Preferences.getPreferenceProvider()
                // .getDefaultString(IResourcePropertyStore.TARGET_FOLDER));
                String importFolder = store.getProperty(IResourcePropertyStore.IMPORT_FOLDER);
                String targetFolder = store.getProperty(IResourcePropertyStore.TARGET_FOLDER);
                YamaicaXmlModel.releaseInstance(project, this);

                IResource importContainer = project.findMember(importFolder);
                IResource targetContainer = project.findMember(targetFolder);

                IPath importPath = null != importContainer ? importContainer.getFullPath() : new Path("");
                IPath targetPath = null != targetContainer ? targetContainer.getFullPath() : new Path("");

                boolean allFilesInsideImportFolder = true;
                boolean allFilesInsideTargetFolder = true;

                for (Object selectedElement : structuredSelection.toArray())
                {
                    if (selectedElement instanceof IResource)
                    {
                        IPath path = ((IResource) selectedElement).getFullPath();

                        if (allFilesInsideImportFolder && !importPath.isPrefixOf(path))
                        {
                            allFilesInsideImportFolder = false;
                        }

                        if (allFilesInsideTargetFolder && !targetPath.isPrefixOf(path))
                        {
                            allFilesInsideTargetFolder = false;
                        }
                    }
                }

                if (allFilesInsideImportFolder && null != importContainer && importContainer instanceof IContainer)
                {
                    viewerInput = importContainer;
                }
                else if (allFilesInsideTargetFolder && null != targetContainer && targetContainer instanceof IContainer)
                {
                    viewerInput = targetContainer;
                }
            }
        }

        resourceSelectionTreeViewer.setInput(viewerInput);

        for (Object selectedObject : structuredSelection.toArray())
        {
            resourceSelectionTreeViewer.expandToLevel(selectedObject, 0);
            resourceSelectionTreeViewer.setChecked(selectedObject, true);
        }
    }

    @Override
    protected void createDestinationGroup(Composite parent)
    {
        Font font = parent.getFont();

        GridLayout layout = new GridLayout(3, false);
        layout.marginWidth = 0;
        layout.marginHeight = 10;

        // destination specification group
        Composite destinationSelectionGroup = new Composite(parent, SWT.NONE);
        destinationSelectionGroup.setLayout(layout);
        destinationSelectionGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
        destinationSelectionGroup.setFont(font);

        Label destinationLabel = new Label(destinationSelectionGroup, SWT.NONE);
        destinationLabel.setText("To director&y:");
        destinationLabel.setFont(font);

        // destination name entry field
        destinationNameField = new Combo(destinationSelectionGroup, SWT.SINGLE | SWT.BORDER);
        destinationNameField.addListener(SWT.Modify, this);
        destinationNameField.addListener(SWT.Selection, this);
        GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
        data.widthHint = SIZING_TEXT_FIELD_WIDTH;
        destinationNameField.setLayoutData(data);
        destinationNameField.setFont(font);

        // destination browse button
        destinationBrowseButton = new Button(destinationSelectionGroup, SWT.PUSH);
        destinationBrowseButton.setText("Bro&wse...");
        destinationBrowseButton.addListener(SWT.Selection, this);
        destinationBrowseButton.setFont(font);
        setButtonLayoutData(destinationBrowseButton);
    }

    @Override
    protected void createOptionsGroup(Composite parent)
    {
        super.createOptionsGroup(parent);
    }

    @Override
    protected void createOptionsGroupButtons(Group optionsGroup)
    {
        Font font = optionsGroup.getFont();

        // overwrite... checkbox
        overwriteExistingFilesCheckbox = new Button(optionsGroup, SWT.CHECK | SWT.LEFT);
        overwriteExistingFilesCheckbox.setText("&Overwrite existing files without warning");
        overwriteExistingFilesCheckbox.setFont(font);
    }

    protected IContainer getSourceContainer()
    {
        return (IContainer) resourceSelectionTreeViewer.getInput();
    }

    @Override
    protected List<IResource> getSelectedResources()
    {
        List<IResource> resources = new LinkedList<IResource>();

        if (null != resourceSelectionTreeViewer)
        {
            for (Object element : resourceSelectionTreeViewer.getCheckedElements())
            {
                if (element instanceof IFile)
                {
                    resources.add((IFile) element);
                }
            }
        }

        return resources;
    }

    public boolean finish()
    {
        File destinationDirectory = new File(getDestinationValue());

        if (!ensureTargetIsValid(destinationDirectory))
        {
            return false;
        }

        // Save dirty editors if possible but do not stop if not all are saved
        saveDirtyEditors();
        // about to invoke the operation so save our state
        saveWidgetValues();

        IRunnableWithProgress exporter = getExporter();

        if (null == exporter)
        {
            return false;
        }

        try
        {
            getContainer().run(true, true, exporter);

            return true;
        }
        catch (InterruptedException e)
        {

        }
        catch (InvocationTargetException e)
        {
            displayErrorDialog(e.getTargetException());
        }
        finally
        {
            // IStatus status = exporter.getStatus();
            //
            // if (!status.isOK())
            // {
            // ErrorDialog.openError(getContainer().getShell(), DataTransferMessages.DataTransfer_exportProblems,
            // null, // no special message
            // status);
            // return false;
            // }
        }

        return false;
    }

    @Override
    public void handleEvent(Event event)
    {
        if (event.widget == destinationBrowseButton)
        {
            DirectoryDialog dialog = new DirectoryDialog(getContainer().getShell(), SWT.SAVE | SWT.SHEET);
            dialog.setMessage("Select a directory to export to.");
            dialog.setText("Export to Directory");
            dialog.setFilterPath(getDestinationValue());
            String selectedDirectoryName = dialog.open();

            if (selectedDirectoryName != null)
            {
                setErrorMessage(null);
                destinationNameField.setText(selectedDirectoryName);
            }
        }

        updatePageCompletion();
    }

    @Override
    public void checkStateChanged(CheckStateChangedEvent event)
    {
        BusyIndicator.showWhile(getShell().getDisplay(), new Runnable()
        {
            @Override
            public void run()
            {
                updateWidgetEnablements();
            }
        });
    }

    @Override
    public void preActionRun(ActionRunEvent e)
    {

    }

    @Override
    public void postActionRun(ActionRunEvent e)
    {
        BusyIndicator.showWhile(getShell().getDisplay(), new Runnable()
        {
            @Override
            public void run()
            {
                updateWidgetEnablements();
            }
        });
    }

    @Override
    protected void restoreWidgetValues()
    {
        IDialogSettings settings = getDialogSettings();

        if (null != settings)
        {
            if (null != destinationNameField)
            {
                String[] directoryNames = settings.getArray(STORE_DESTINATION_NAMES_ID);

                if (null != directoryNames)
                {
                    destinationNameField.setText(directoryNames[0]);

                    for (int i = 0; i < directoryNames.length; i++)
                    {
                        destinationNameField.add(directoryNames[i]);
                    }
                }
            }

            // radio buttons and checkboxes
            if (null != viewerToolBar)
            {
                viewerToolBar.setFilterEnabled(!settings.getBoolean(STORE_SHOW_ALL_FILES_ID));
            }

            if (null != overwriteExistingFilesCheckbox)
            {
                overwriteExistingFilesCheckbox.setSelection(settings.getBoolean(STORE_OVERWRITE_EXISTING_FILES_ID));
            }
        }
    }

    @Override
    protected void saveWidgetValues()
    {
        // update directory names history
        IDialogSettings settings = getDialogSettings();

        if (null != settings)
        {
            String[] directoryNames = settings.getArray(STORE_DESTINATION_NAMES_ID);

            if (null == directoryNames)
            {
                directoryNames = new String[0];
            }

            directoryNames = addToHistory(directoryNames, getDestinationValue());
            settings.put(STORE_DESTINATION_NAMES_ID, directoryNames);

            // radio buttons and checkboxes
            if (null != viewerToolBar)
            {
                settings.put(STORE_SHOW_ALL_FILES_ID, !viewerToolBar.isFilterEnabled());
            }

            if (null != overwriteExistingFilesCheckbox)
            {
                settings.put(STORE_OVERWRITE_EXISTING_FILES_ID, overwriteExistingFilesCheckbox.getSelection());
            }
        }
    }

    @Override
    protected boolean validateSourceGroup()
    {
        // there must be some resources selected for Export
        boolean isValid = true;

        if (getSelectedResources().size() == 0)
        {
            setErrorMessage("There are no resources currently selected for export.");

            isValid = false;
        }
        else
        {
            setErrorMessage(null);
        }

        return super.validateSourceGroup() && isValid;
    }

    @Override
    protected boolean validateDestinationGroup()
    {
        String destinationValue = getDestinationValue();

        if (destinationValue.length() == 0)
        {
            setMessage("Please enter a destination directory.");

            return false;
        }

        String conflictingContainer = getConflictingContainerNameFor(destinationValue);

        if (null == conflictingContainer)
        {
            // no error message, but warning may exists
            String threatenedContainer = getOverlappingProjectName(destinationValue);

            if (null == threatenedContainer)
            {
                setMessage(null);
            }
            else
            {
                setMessage(NLS.bind("The project {0} may be damaged after this operation", threatenedContainer), WARNING);
            }

        }
        else
        {
            setErrorMessage(NLS.bind("Destination directory conflicts with location of {0}.", conflictingContainer));
            destinationNameField.setFocus();

            return false;
        }

        return true;
    }

    @Override
    protected boolean validateOptionsGroup()
    {
        return super.validateOptionsGroup();
    }

    private boolean ensureDirectoryExists(File directory)
    {
        if (!directory.exists())
        {
            if (!queryYesNoQuestion("Target directory does not exist.  Would you like to create it?"))
            {
                return false;
            }

            if (!directory.mkdirs())
            {
                displayErrorDialog("Target directory could not be created.");
                destinationNameField.setFocus();

                return false;
            }
        }

        return true;
    }

    private boolean ensureTargetIsValid(File targetDirectory)
    {
        if (targetDirectory.exists() && !targetDirectory.isDirectory())
        {
            displayErrorDialog("Target directory already exists as a file.");
            destinationNameField.setFocus();

            return false;
        }

        return ensureDirectoryExists(targetDirectory);
    }

    protected String getDestinationValue()
    {
        return destinationNameField.getText().trim();
    }

    private String getConflictingContainerNameFor(String targetDirectory)
    {
        IPath rootPath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
        IPath testPath = new Path(targetDirectory);

        // cannot export into workspace root
        if (testPath.equals(rootPath))
            return rootPath.lastSegment();

        // Are they the same?
        if (testPath.matchingFirstSegments(rootPath) == rootPath.segmentCount())
        {
            String firstSegment = testPath.removeFirstSegments(rootPath.segmentCount()).segment(0);

            if (!Character.isLetterOrDigit(firstSegment.charAt(0)))
                return firstSegment;
        }

        return null;
    }

    private String getOverlappingProjectName(String targetDirectory)
    {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        URI testURI = new File(targetDirectory).toURI();

        IContainer[] containers = root.findContainersForLocationURI(testURI);

        if (containers.length > 0)
        {
            return containers[0].getProject().getName();
        }

        return null;
    }

    @Override
    public void setWizard(IWizard newWizard)
    {
        if (newWizard instanceof YamaicaWizard)
        {
            restrictWizardPage = ((YamaicaWizard) newWizard).restrictWizard;
        }

        super.setWizard(newWizard);
    }

    protected abstract String[] getFileExtensions();

    protected abstract IRunnableWithProgress getExporter();
}