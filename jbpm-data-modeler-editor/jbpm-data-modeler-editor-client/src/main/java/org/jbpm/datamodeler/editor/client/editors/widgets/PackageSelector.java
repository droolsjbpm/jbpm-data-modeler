/**
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.datamodeler.editor.client.editors.widgets;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import org.jbpm.datamodeler.editor.client.editors.DataModelerContext;
import org.jbpm.datamodeler.editor.client.util.DataModelerUtils;
import org.jbpm.datamodeler.editor.client.validation.ValidatorService;
import org.jbpm.datamodeler.editor.model.DataModelTO;
import org.jbpm.datamodeler.editor.model.DataObjectTO;
import org.uberfire.client.mvp.Command;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PackageSelector extends Composite {

    interface PackageSelectorUIBinder
            extends UiBinder<Widget, PackageSelector> {

    }

    @UiField
    ListBox packageList;

    @UiField
    Button newPackage;

    @Inject
    ValidatorService validatorService;

    @Inject
    NewPackagePopup newPackagePopup;

    private static PackageSelectorUIBinder uiBinder = GWT.create(PackageSelectorUIBinder.class);

    public static final String NOT_SELECTED = "NOT_SELECTED";
    public static final String DEFAULT_PACKAGE = "defaultpkg";

    private DataObjectTO dataObject;

    private DataModelerContext context;

    public PackageSelector() {
        initWidget(uiBinder.createAndBindUi(this));
        
        packageList.addItem("", NOT_SELECTED);
        packageList.addItem(DEFAULT_PACKAGE, DEFAULT_PACKAGE);
    }

    @PostConstruct
    private void init() {
        Command command = new Command() {
            @Override
            public void execute() {
                String newPackage = newPackagePopup.getPackageName();
                processNewPackage(newPackage);
            }
        };
        newPackagePopup.setAfterAddCommand(command);
    }

    private void processNewPackage(String newPackageName) {
        if (newPackageName != null && !"".equals(newPackageName.trim())) {
            boolean exists = false;
            newPackageName = newPackageName.trim();
            int count = packageList.getItemCount();
            if (count > 0) {
                for (int i = 0; i < count; i++) {
                    if ((exists = newPackageName.equals(packageList.getValue(i)))) break;
                }
            }
            if (exists) {
                packageList.setSelectedValue(newPackageName);
            } else {
                packageList.addItem(newPackageName, newPackageName);
                packageList.setSelectedValue(newPackageName);
                DomEvent.fireNativeEvent(Document.get().createChangeEvent(), packageList);
            }
        }
    }

    @UiHandler("newPackage")
    void createNewPackage(ClickEvent event) {
        newPackagePopup.show();
    }

    public void enableCreatePackage(boolean enable) {
        newPackage.setVisible(enable);
    }

    public ListBox getPackageList() {
        return packageList;
    }

    public DataModelerContext getContext() {
        return context;
    }

    public void setContext(DataModelerContext context) {
        this.context = context;
        initList();
    }

    private DataModelTO getDataModel() {
        return getContext() != null ? getContext().getDataModel() : null;
    }

    public DataObjectTO getDataObject() {
        return dataObject;
    }

    public void setDataObject(DataObjectTO dataObject) {
        this.dataObject = dataObject;
        initList();

        if (dataObject != null && dataObject.getPackageName() != null && !"".equals(dataObject.getPackageName())) {
            packageList.setSelectedValue(dataObject.getPackageName());
        } else {
            packageList.setSelectedValue(NOT_SELECTED);
        }
    }
            
    private void initList() {
        packageList.clear();
        List<String> packageNames = new ArrayList<String>();
        if (getDataModel() != null) {
            String packageName;
            for (String className : getContext().getHelper().getClassList()) {
                packageName = DataModelerUtils.getInstance().extractPackageName(className);
                if (packageName != null && !DEFAULT_PACKAGE.equals(packageName) && !packageNames.contains(packageName)) {
                    packageNames.add(packageName);
                }
            }
        }
        Collections.sort(packageNames);
        packageList.addItem("", NOT_SELECTED);
        packageList.addItem(DEFAULT_PACKAGE, DEFAULT_PACKAGE);
        for (String packageName : packageNames) {
            packageList.addItem(packageName, packageName);
        }
    }

}
