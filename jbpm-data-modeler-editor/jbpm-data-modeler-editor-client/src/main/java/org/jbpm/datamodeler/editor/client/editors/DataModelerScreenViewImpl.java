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

package org.jbpm.datamodeler.editor.client.editors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import org.jbpm.datamodeler.editor.client.editors.resources.i18n.Constants;
import org.jbpm.datamodeler.editor.client.editors.widgets.DataModelBrowser;
import org.jbpm.datamodeler.editor.client.editors.widgets.DataObjectBrowser;
import org.jbpm.datamodeler.editor.client.editors.widgets.ModelPropertiesEditor;
import org.jbpm.datamodeler.editor.events.*;
import org.jbpm.datamodeler.editor.model.PropertyTypeTO;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.List;

//@Dependent
public class DataModelerScreenViewImpl extends Composite
    implements DataModelerScreenPresenter.DataModelerScreenView {

    interface DataModelerScreenViewBinder
            extends
            UiBinder<Widget, DataModelerScreenViewImpl> {

    }

    private static DataModelerScreenViewBinder uiBinder = GWT.create(DataModelerScreenViewBinder.class);

    private DataModelerScreenPresenter presenter;

    @UiField
    SimplePanel browserPanel = new SimplePanel();

    @UiField
    SimplePanel dataObjectPanel = new SimplePanel();

    @UiField
    SimplePanel propertiesPanel = new SimplePanel();

    @Inject
    private ModelPropertiesEditor modelPropertiesEditor;

    @Inject
    private DataModelBrowser dataModelBrowser;

    @Inject
    private DataObjectBrowser dataObjectBrowser;

    private DataModelerContext context;

    public DataModelerScreenViewImpl() {
        initWidget( uiBinder.createAndBindUi( this ) );
    }

    @PostConstruct
    private void initUI() {
        browserPanel.add(dataModelBrowser);
        dataObjectPanel.add(dataObjectBrowser);
        propertiesPanel.add(modelPropertiesEditor);
    }

    @Override
    public void setContext(DataModelerContext context) {
        this.context = context;
        dataModelBrowser.setContext(context);
        dataObjectBrowser.setContext(context);
        modelPropertiesEditor.setContext(context);
    }

    @Override
    public void init(final DataModelerScreenPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setBaseTypes(List<PropertyTypeTO> baseTypes) {
        dataObjectBrowser.setBaseTypes(baseTypes);
        modelPropertiesEditor.setBaseTypes(baseTypes);
    }

    @Override
    public boolean confirmClose() {
        return Window.confirm( Constants.INSTANCE.modelEditor_discard_changes_message());
    }

    private void updateChangeStatus(DataModelerEvent event) {
        if (context != null && event.isFrom(context.getDataModel())) {
            context.setDirty(true);
        }
    }

    // event observers
    private void onDataObjectCreated(@Observes DataObjectCreatedEvent event) {
        updateChangeStatus(event);
    }

    private void onDataObjectChange(@Observes DataObjectChangeEvent event) {
        updateChangeStatus(event);
    }

    private void onDataObjectDeleted(@Observes DataObjectDeletedEvent event) {
        updateChangeStatus(event);
    }

    private void onDataObjectFieldCreated(@Observes DataObjectFieldCreatedEvent event) {
        updateChangeStatus(event);
    }
    
    private void onDataObjectFieldChange(@Observes DataObjectFieldChangeEvent event) {
        updateChangeStatus(event);
    }

    private void onDataObjectFieldDeleted(@Observes DataObjectFieldDeletedEvent event) {
        updateChangeStatus(event);
    }

}