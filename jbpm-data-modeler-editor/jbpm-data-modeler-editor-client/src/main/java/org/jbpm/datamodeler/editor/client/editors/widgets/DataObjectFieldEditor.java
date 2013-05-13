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

import com.github.gwtbootstrap.client.ui.CheckBox;
import com.github.gwtbootstrap.client.ui.ListBox;
import com.github.gwtbootstrap.client.ui.TextArea;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.*;
import org.jboss.errai.bus.client.api.RemoteCallback;
import org.jboss.errai.ioc.client.api.Caller;
import org.jbpm.datamodeler.editor.client.editors.DataModelerContext;
import org.jbpm.datamodeler.editor.client.editors.DataModelerErrorCallback;
import org.jbpm.datamodeler.editor.client.editors.widgets.ErrorPopup;
import org.jbpm.datamodeler.editor.client.editors.widgets.PackageSelector;
import org.jbpm.datamodeler.editor.client.util.DataModelerUtils;
import org.jbpm.datamodeler.editor.client.validation.ValidatorCallback;
import org.jbpm.datamodeler.editor.client.validation.ValidatorService;
import org.jbpm.datamodeler.editor.events.DataModelerEvent;
import org.jbpm.datamodeler.editor.events.DataObjectFieldChangeEvent;
import org.jbpm.datamodeler.editor.events.DataObjectFieldSelectedEvent;
import org.jbpm.datamodeler.editor.model.*;
import org.jbpm.datamodeler.editor.service.DataModelerService;

import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.*;

public class DataObjectFieldEditor extends Composite {

    interface DataObjectFieldEditorUIBinder
            extends UiBinder<Widget, DataObjectFieldEditor> {

    };

    private static DataObjectFieldEditorUIBinder uiBinder = GWT.create(DataObjectFieldEditorUIBinder.class);

    @UiField
    Label titleLabel;

    @UiField
    TextBox name;

    @UiField
    TextBox label;

    @UiField
    TextArea description;

    @UiField
    ListBox typeSelector;

    @UiField
    CheckBox equalsSelector;

    @UiField
    CheckBox requiredSelector;

    @UiField
    Label positionLabel;

    @UiField
    TextBox positionText;

    @Inject
    Event<DataModelerEvent> dataModelerEventEvent;

    @Inject
    private ValidatorService validatorService;

    private DataObjectFieldEditorErrorPopup ep = new DataObjectFieldEditorErrorPopup();

    private Map<String, AnnotationDefinitionTO> annotationDefinitions = new HashMap<String, AnnotationDefinitionTO>();

    private DataObjectTO dataObject;

    private ObjectPropertyTO objectField;

    private DataModelerContext context;

    private List<String> baseTypes;

    public DataObjectFieldEditor() {
        initWidget(uiBinder.createAndBindUi(this));

        typeSelector.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                typeChanged(event);
            }
        });
    }

    public DataObjectTO getDataObject() {
        return dataObject;
    }

    public void setDataObject(DataObjectTO dataObject) {
        this.dataObject = dataObject;
    }

    public ObjectPropertyTO getObjectField() {
        return objectField;
    }

    public void setObjectField(ObjectPropertyTO objectField) {
        this.objectField = objectField;
    }

    public DataModelerContext getContext() {
        return context;
    }

    public void setContext(DataModelerContext context) {
        this.context = context;
        initTypeList();
    }

    private DataModelTO getDataModel() {
        return getContext().getDataModel();
    }

    public void setBaseTypes(List<PropertyTypeTO> baseTypes) {
        this.baseTypes = new ArrayList<String>(baseTypes.size());
        for (PropertyTypeTO baseType : baseTypes) {
            this.baseTypes.add(baseType.getClassName());
        }
    }

    // Event notifications

    private void notifyFieldChange(String memberName, Object oldValue, Object newValue) {
        getContext().getHelper().dataModelChanged();
        dataModelerEventEvent.fire(new DataObjectFieldChangeEvent(DataModelerEvent.DATA_OBJECT_FIELD_EDITOR, getDataModel(), getDataObject(), getObjectField(), memberName, oldValue, newValue));
    }

    // Event observers
    private void onFieldSelected(@Observes DataObjectFieldSelectedEvent event) {
        if (event.isFrom(getDataModel())) {
            loadDataObjectField(event.getCurrentDataObject(), event.getCurrentField());
        }
    }

    private void loadDataObjectField(DataObjectTO dataObject, ObjectPropertyTO objectField) {
        clean();
        if (dataObject != null && objectField != null) {
            setDataObject(dataObject);
            setObjectField(objectField);

            name.setText(getObjectField().getName());

            AnnotationTO annotation = objectField.getAnnotation(AnnotationDefinitionTO.LABEL_ANNOTATION);
            if (annotation != null) {
                label.setText( (String) annotation.getValue(AnnotationDefinitionTO.VALUE_PARAM) );
            }

            annotation = objectField.getAnnotation(AnnotationDefinitionTO.DESCRIPTION_ANNOTATION);
            if (annotation != null) {
                description.setText( (String) annotation.getValue(AnnotationDefinitionTO.VALUE_PARAM));
            }

            String type = getObjectField().getClassName() + (getObjectField().isMultiple() ? DataModelerUtils.MULTIPLE:"");
            typeSelector.setSelectedValue(type);

            annotation = objectField.getAnnotation(AnnotationDefinitionTO.EQUALS_ANNOTATION);
            if (annotation != null) {
                equalsSelector.setValue(Boolean.TRUE);
            }

            annotation = objectField.getAnnotation(AnnotationDefinitionTO.POSITION_ANNOTATON);
            if (annotation != null) {
                positionText.setText( (String) annotation.getValue(AnnotationDefinitionTO.VALUE_PARAM));
            }
        }
    }

    // TODO listen to DataObjectFieldDeletedEvent?

    // Event handlers
    @UiHandler("name")
    void nameChanged(ValueChangeEvent<String> event) {
        // Set widgets to errorpopup for styling purposes etc.
        ep.setTitleWidget(titleLabel);
        ep.setValueWidget(name);

        final String oldValue = getObjectField().getName();
        final String newValue = name.getValue();

        // In case an invalid name (entered before), was corrected to the original value, don't do anything but reset the label style
        if (oldValue.equalsIgnoreCase(newValue)) {
            titleLabel.setStyleName(null);
            return;
        }

        validatorService.isValidIdentifier(newValue, new ValidatorCallback() {
            @Override
            public void onFailure() {
                ep.showMessage("Invalid data attribute identifier: " + newValue + " is not a valid Java identifier");
            }

            @Override
            public void onSuccess() {
                validatorService.isUniqueAttributeName(newValue, getDataObject(), new ValidatorCallback() {
                    @Override
                    public void onFailure() {
                        ep.showMessage("An object attribute with identifier: " + newValue + " already exists in the data object.");
                    }

                    @Override
                    public void onSuccess() {
                        titleLabel.setStyleName(null);
                        objectField.setName(newValue);
                        notifyFieldChange("name", oldValue, newValue);
                    }
                });
            }
        });
    }

    @UiHandler("label")
    void labelChanged(final ValueChangeEvent<String> event) {
        final String _label = label.getValue();
        AnnotationTO annotation = getObjectField().getAnnotation(AnnotationDefinitionTO.LABEL_ANNOTATION);

        if (annotation != null) {
            if ( _label != null && !"".equals(_label) ) annotation.setValue(AnnotationDefinitionTO.VALUE_PARAM, _label);
            else getObjectField().removeAnnotation(annotation);
        } else {
            if ( _label != null && !"".equals(_label) ) {
                getObjectField().addAnnotation(getContext().getAnnotationDefinitions().get(AnnotationDefinitionTO.LABEL_ANNOTATION), AnnotationDefinitionTO.VALUE_PARAM, _label );
            }
        }
    }

    @UiHandler("description")
    void descriptionChanged(final ValueChangeEvent<String> event) {
        final String _description = description.getValue();
        AnnotationTO annotation = getObjectField().getAnnotation(AnnotationDefinitionTO.DESCRIPTION_ANNOTATION);

        if (annotation != null) {
            if ( _description != null && !"".equals(_description) ) annotation.setValue(AnnotationDefinitionTO.VALUE_PARAM, _description);
            else getObjectField().removeAnnotation(annotation);
        } else {
            if ( _description != null && !"".equals(_description) ) {
                getObjectField().addAnnotation(getContext().getAnnotationDefinitions().get(AnnotationDefinitionTO.DESCRIPTION_ANNOTATION), AnnotationDefinitionTO.VALUE_PARAM, _description );
            }
        }
    }

    private void typeChanged(ChangeEvent event) {
        String oldValue = getObjectField().getClassName();
        String type = typeSelector.getValue();
        int i = type.lastIndexOf(DataModelerUtils.MULTIPLE);
        if (i >= 0) {
            type = type.substring(0, i);
            getObjectField().setMultiple(true);
        } else {
            getObjectField().setMultiple(false);
        }
        getObjectField().setClassName(type);

        // Un-reference former type reference and set the new one
        if (!baseTypes.contains(type)) {
            getContext().getHelper().dataObjectUnReferenced(oldValue, getDataObject().getClassName());
            getContext().getHelper().dataObjectReferenced(type, getDataObject().getClassName());
        }
        notifyFieldChange("className", oldValue, type);
    }

    @UiHandler("equalsSelector")
    void equalsChanged(final ClickEvent event) {
        final Boolean setEquals = equalsSelector.getValue();
        AnnotationTO annotation = getObjectField().getAnnotation(AnnotationDefinitionTO.EQUALS_ANNOTATION);

        if (annotation != null && !setEquals) getObjectField().removeAnnotation(annotation);
        else if (annotation == null && setEquals) getObjectField().addAnnotation(new AnnotationTO(getContext().getAnnotationDefinitions().get(AnnotationDefinitionTO.EQUALS_ANNOTATION)));
    }

    @UiHandler("positionText")
    void positionChanged(final ValueChangeEvent<String> event) {
        // Set widgets to errorpopup for styling purposes etc.
        ep.setTitleWidget(positionLabel);
        ep.setValueWidget(positionText);


        AnnotationTO annotation = getObjectField().getAnnotation(AnnotationDefinitionTO.POSITION_ANNOTATON);
        String oldPosition = "";
        if (annotation != null) {
            oldPosition = annotation.getValue(AnnotationDefinitionTO.VALUE_PARAM).toString();
        }
        final String newPosition = positionText.getValue();

        // In case an invalid position (entered before), was corrected to the original value, don't do anything but reset the label style
        if (oldPosition.equalsIgnoreCase(newPosition)) {
            positionLabel.setStyleName(null);
            return;
        }

        validatorService.isValidPosition(newPosition, new ValidatorCallback() {
            @Override
            public void onFailure() {
                ep.showMessage("Illegal position specified, should be zero or a positive integer");
            }

            @Override
            public void onSuccess() {
                AnnotationTO annotation = getObjectField().getAnnotation(AnnotationDefinitionTO.POSITION_ANNOTATON);

                if (annotation != null) {
                    if ( newPosition != null && !"".equals(newPosition) ) annotation.setValue(AnnotationDefinitionTO.VALUE_PARAM, newPosition);
                    else getObjectField().removeAnnotation(annotation);
                } else {
                    if ( newPosition != null && !"".equals(newPosition) ) {
                        getObjectField().addAnnotation(getContext().getAnnotationDefinitions().get(AnnotationDefinitionTO.POSITION_ANNOTATON), AnnotationDefinitionTO.VALUE_PARAM, newPosition );
                    }
                }
            }
        });


    }

    private void initTypeList() {
        typeSelector.clear();

        SortedSet<String> typeNames = new TreeSet<String>();
        if (getDataModel() != null) {
            // First add all base types, ordered
            for (String baseType : baseTypes) {
                typeNames.add(baseType);
            }
            DataModelerUtils dmu = DataModelerUtils.getInstance();
            for (String typeName : typeNames) {
                String _typeName = dmu.extractClassName(typeName);
                typeSelector.addItem(_typeName, typeName);
//                _typeName = _typeName + DataModelerUtils.MULTIPLE;
//                typeSelector.addItem(_typeName, _typeName);
            }

            // Second add all model types, ordered
            typeNames.clear();
            for (DataObjectTO dataObject : getDataModel().getDataObjects()) {
                String className = dataObject.getClassName();
                typeNames.add(className);
                typeNames.add(className + DataModelerUtils.MULTIPLE);
            }
            for (String typeName : typeNames) {
                typeSelector.addItem(typeName, typeName);
            }

            // Then add all external types, ordered
            typeNames.clear();
            for (String extClass : getDataModel().getExternalClasses()) {
                typeNames.add(DataModelerUtils.EXTERNAL_PREFIX + extClass);
                typeNames.add(DataModelerUtils.EXTERNAL_PREFIX + extClass + DataModelerUtils.MULTIPLE);
            }
            for (String typeName : typeNames) {
                typeSelector.addItem(typeName, typeName);
            }
        }
    }

    private void clean() {
        titleLabel.setStyleName(null);
        name.setText(null);
        label.setText(null);
        description.setText(null);
        typeSelector.setSelectedValue(null);
        equalsSelector.setValue(Boolean.FALSE);
        positionLabel.setStyleName(null);
        positionText.setText(null);
    }

    // TODO extract this to parent widget to avoid duplicate code
    private class DataObjectFieldEditorErrorPopup extends ErrorPopup {
        private Widget titleWidget;
        private Widget valueWidget;
        private DataObjectFieldEditorErrorPopup() {
            setAfterCloseEvent(new Command() {
                @Override
                public void execute() {
                    titleWidget.setStyleName("text-error");
                    if (valueWidget instanceof Focusable) ((FocusWidget)valueWidget).setFocus(true);
                    if (valueWidget instanceof ValueBoxBase) ((ValueBoxBase)valueWidget).selectAll();
                    clearWidgets();
                }
            });
        }
        private void setTitleWidget(Widget titleWidget){this.titleWidget = titleWidget;titleWidget.setStyleName(null);}
        private void setValueWidget(Widget valueWidget){this.valueWidget = valueWidget;}
        private void clearWidgets() {
            titleWidget = null;
            valueWidget = null;
        }
    }
}