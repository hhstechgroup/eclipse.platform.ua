/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.help.ui.internal.search;

import java.util.*;

import org.eclipse.help.internal.base.*;
import org.eclipse.help.internal.workingset.*;
import org.eclipse.help.ui.*;
import org.eclipse.help.ui.internal.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;


/**
 * Local Help participant in the federated search.
 */
public class LocalHelpPage extends RootScopePage{
    private Button searchAll;
    private Button searchSelected;
    private CheckboxTreeViewer tree;
    private ITreeContentProvider treeContentProvider;
    private ILabelProvider elementLabelProvider;
    private boolean firstCheck;
    private WorkingSet workingSet;

    /* (non-Javadoc)
     * @see org.eclipse.help.ui.ISearchScopePage#init(java.lang.String, java.lang.String)
     */
    public void init(String engineId, String scopeSetName) {
        super.init(engineId, scopeSetName);
        if (scopeSetName != null)
            workingSet = BaseHelpSystem.getWorkingSetManager().getWorkingSet(scopeSetName);
    }
    /**
     * Default constructor.
     */
    public LocalHelpPage() {
        firstCheck = true;
    }

    /* (non-Javadoc)
     * @see org.eclipse.help.ui.RootScopePage#createScopeContents(org.eclipse.swt.widgets.Composite)
     */
    protected Control createScopeContents(Composite parent) {

        Font font = parent.getFont();
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

        searchAll = new Button(composite, SWT.RADIO);
        searchAll.setText(HelpUIResources.getString("selectAll")); //$NON-NLS-1$
        GridData gd = new GridData();
        searchAll.setLayoutData(gd);
        searchAll.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                tree.getTree().setEnabled(false);
//                searchQueryData.setBookFiltering(false);
            }
        });
        
        searchSelected = new Button(composite, SWT.RADIO);
        searchSelected.setText(HelpUIResources.getString("selectWorkingSet")); //$NON-NLS-1$
        gd = new GridData();
        searchSelected.setLayoutData(gd);
        searchSelected.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                tree.getTree().setEnabled(true);
//              searchQueryData.setBookFiltering(false);
            }
        });
      
        if (workingSet == null)
            searchAll.setSelection(true);
        else
            searchSelected.setSelection(true);
        
        Label label = new Label(composite, SWT.WRAP);
        label.setFont(font);
        label.setText(HelpUIResources.getString("WorkingSetContent")); //$NON-NLS-1$
        gd = new GridData(GridData.GRAB_HORIZONTAL
                | GridData.HORIZONTAL_ALIGN_FILL
                | GridData.VERTICAL_ALIGN_CENTER);
        label.setLayoutData(gd);

        tree = new CheckboxTreeViewer(composite, SWT.BORDER | SWT.H_SCROLL
                | SWT.V_SCROLL);
        gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
        gd.heightHint = convertHeightInCharsToPixels(15);
        tree.getControl().setLayoutData(gd);
        tree.getControl().setFont(font);

        treeContentProvider = new HelpWorkingSetTreeContentProvider();
        tree.setContentProvider(treeContentProvider);

        elementLabelProvider = new HelpWorkingSetElementLabelProvider();
        tree.setLabelProvider(elementLabelProvider);

        tree.setUseHashlookup(true);

        tree.setInput(BaseHelpSystem.getWorkingSetManager().getRoot());

        tree.addCheckStateListener(new ICheckStateListener() {
            public void checkStateChanged(CheckStateChangedEvent event) {
                handleCheckStateChange(event);
            }
        });

        tree.addTreeListener(new ITreeViewerListener() {
            public void treeCollapsed(TreeExpansionEvent event) {
            }
            public void treeExpanded(TreeExpansionEvent event) {
                final Object element = event.getElement();
                if (tree.getGrayed(element) == false)
                    BusyIndicator.showWhile(getShell().getDisplay(),
                            new Runnable() {
                                public void run() {
                                    setSubtreeChecked(element, tree
                                            .getChecked(element), false);
                                }
                            });
            }
        });
        tree.getTree().setEnabled(workingSet != null);
        initializeCheckedState();

        // Set help for the page
        //WorkbenchHelp.setHelp(tree, "help_workingset_page");   
        return composite;
    }

    private void initializeCheckedState() {
        if (workingSet == null)
            return;

        BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
            public void run() {
                Object[] elements = workingSet.getElements();
                tree.setCheckedElements(elements);
                for (int i = 0; i < elements.length; i++) {
                    Object element = elements[i];
                    if (isExpandable(element))
                        setSubtreeChecked(element, true, true);
                    updateParentState(element, true);
                }
            }
        });
    }

    boolean isExpandable(Object element) {
        return treeContentProvider.hasChildren(element);
    }

    void updateParentState(Object child, boolean baseChildState) {
        if (child == null)
            return;

        Object parent = treeContentProvider.getParent(child);
        if (parent == null)
            return;

        boolean allSameState = true;
        Object[] children = null;
        children = treeContentProvider.getChildren(parent);

        for (int i = children.length - 1; i >= 0; i--) {
            if (tree.getChecked(children[i]) != baseChildState
                    || tree.getGrayed(children[i])) {
                allSameState = false;
                break;
            }
        }

        tree.setGrayed(parent, !allSameState);
        tree.setChecked(parent, !allSameState || baseChildState);

        updateParentState(parent, baseChildState);
    }

    void setSubtreeChecked(Object parent, boolean state,
            boolean checkExpandedState) {

        Object[] children = treeContentProvider.getChildren(parent);
        for (int i = children.length - 1; i >= 0; i--) {
            Object element = children[i];
            if (state) {
                tree.setChecked(element, true);
                tree.setGrayed(element, false);
            } else
                tree.setGrayChecked(element, false);
            if (isExpandable(element))
                setSubtreeChecked(element, state, checkExpandedState);
        }
    }

    private void findCheckedElements(java.util.List checkedResources,
            Object parent) {
        Object[] children = treeContentProvider.getChildren(parent);
        for (int i = 0; i < children.length; i++) {
            if (tree.getGrayed(children[i]))
                findCheckedElements(checkedResources, children[i]);
            else if (tree.getChecked(children[i]))
                checkedResources.add(children[i]);
        }
    }

    void handleCheckStateChange(final CheckStateChangedEvent event) {
        BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
            public void run() {
                Object element = event.getElement();
                boolean state = event.getChecked();
                tree.setGrayed(element, false);
                if (isExpandable(element))
                    setSubtreeChecked(element, state, state);
                // only check subtree if state is set to true

                updateParentState(element, state);
//                validateInput();
            }
        });
    }    
       
    public WorkingSet getWorkingSet() {
        ArrayList elements = new ArrayList(10);
        findCheckedElements(elements, tree.getInput());
        if (workingSet == null) {
            workingSet = new WorkingSet(
                    getScopeSetName(),
                    (AdaptableHelpResource[]) elements
                            .toArray(new AdaptableHelpResource[elements.size()]));
        } else {
            workingSet.setName(getScopeSetName());
            workingSet.setElements((AdaptableHelpResource[]) elements
                    .toArray(new AdaptableHelpResource[elements.size()]));
        }
        return workingSet;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.IPreferencePage#performOk()
     */
    public boolean performOk() {
        if (searchSelected.getSelection())
            BaseHelpSystem.getWorkingSetManager().addWorkingSet(getWorkingSet());
        else 
            BaseHelpSystem.getWorkingSetManager().removeWorkingSet(getWorkingSet());
        
        getPreferenceStore().setValue(getEngineId(), getScopeSetName());
        return super.performOk();
    }
}