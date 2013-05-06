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

import com.github.gwtbootstrap.client.ui.CellTable;
import com.github.gwtbootstrap.client.ui.TooltipCellDecorator;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.*;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jbpm.datamodeler.editor.client.editors.resources.i18n.Constants;
import org.jbpm.datamodeler.editor.client.editors.resources.images.ImagesResources;
import org.jbpm.datamodeler.editor.client.validation.ValidatorCallback;
import org.jbpm.datamodeler.editor.client.validation.ValidatorService;
import org.jbpm.datamodeler.editor.events.*;
import org.jbpm.datamodeler.editor.model.DataModelTO;
import org.jbpm.datamodeler.editor.model.DataObjectTO;
import org.uberfire.client.common.ErrorPopup;
import org.uberfire.client.workbench.widgets.events.NotificationEvent;

import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class DataModelBrowser extends Composite {

    interface DataModelBrowserUIBinder
            extends UiBinder<Widget, DataModelBrowser> {

    };

    private static DataModelBrowserUIBinder uiBinder = GWT.create(DataModelBrowserUIBinder.class);

    @UiField
    VerticalPanel mainPanel;
    
    @UiField Label modelName;

    @UiField(provided = true)
    CellTable<DataObjectTO> dataObjectsTable = new CellTable<DataObjectTO>(1000, GWT.<CellTable.SelectableResources>create(CellTable.SelectableResources.class));

    @UiField
    com.github.gwtbootstrap.client.ui.Button newEntityButton;

    @Inject
    private ValidatorService validatorService;

    @Inject
    private Event<NotificationEvent> notification;

    SingleSelectionModel<DataObjectTO> selectionModel = new SingleSelectionModel<DataObjectTO>();

    private DataModelTO dataModel;

    private ListDataProvider<DataObjectTO> dataObjectsProvider = new ListDataProvider<DataObjectTO>();

    private List<DataObjectTO> dataObjects = new ArrayList<DataObjectTO>();

    private DataModelEditorPresenter modelEditorPresenter;

    private boolean skipNextOnChange = false;

    @Inject
    private Event<DataModelerEvent> dataModelerEvent;

    @Inject
    private NewDataObjectPopup newDataObjectPopup;

    public DataModelBrowser() {
        initWidget(uiBinder.createAndBindUi(this));

        modelName.setText(Constants.INSTANCE.modelBrowser_modelUnknown());

        dataObjectsProvider.setList(dataObjects);
        dataObjectsTable.setEmptyTableWidget( new com.github.gwtbootstrap.client.ui.Label(Constants.INSTANCE.modelBrowser_emptyTable()));

        //Init delete column
        ClickableImageResourceCell clickableImageResourceCell = new ClickableImageResourceCell(true);
        final TooltipCellDecorator<ImageResource> decorator = new TooltipCellDecorator<ImageResource>(clickableImageResourceCell);
        decorator.setText(Constants.INSTANCE.modelBrowser_action_deleteDataObject());

        final Column<DataObjectTO, ImageResource> deleteDataObjectColumnImg = new Column<DataObjectTO, ImageResource>(decorator) {
            @Override
            public ImageResource getValue( final DataObjectTO global ) {
                return ImagesResources.INSTANCE.Delete();
            }
        };

        deleteDataObjectColumnImg.setFieldUpdater( new FieldUpdater<DataObjectTO, ImageResource>() {
            public void update( final int index,
                                final DataObjectTO object,
                                final ImageResource value ) {
                deleteDataObject(object, index);
            }
        } );

        dataObjectsTable.addColumn(deleteDataObjectColumnImg);
        dataObjectsTable.setColumnWidth(deleteDataObjectColumnImg, 20, Style.Unit.PX);

        //Init data object column
        final TextColumn<DataObjectTO> dataObjectColumn = new TextColumn<DataObjectTO>() {

            @Override
            public void render(Cell.Context context, DataObjectTO object, SafeHtmlBuilder sb) {
                SafeHtml startDiv = new SafeHtml() {
                    @Override
                    public String asString() {
                        return "<div style=\"cursor: pointer;\">";
                    }
                };
                SafeHtml endDiv = new SafeHtml() {
                    @Override
                    public String asString() {
                        return "</div>";
                    }
                };

                sb.append(startDiv);
                super.render(context, object, sb);
                sb.append(endDiv);
            }

            @Override
            public String getValue( final DataObjectTO dataObject) {
                return dataObject.getName();
            }
        };
        dataObjectColumn.setSortable(true);
        dataObjectsTable.addColumn(dataObjectColumn, Constants.INSTANCE.modelBrowser_columnName());


        /*

        ColumnSortEvent.ListHandler<ObjectPropertyTO> propertyNameColHandler = new ColumnSortEvent.ListHandler<ObjectPropertyTO>(dataObjectPropertiesProvider.getList());
        propertyNameColHandler.setComparator(propertyNameColumn, new ObjectPropertyComparator("name"));
        dataObjectPropertiesTable.addColumnSortHandler(propertyNameColHandler);
         */
        ColumnSortEvent.ListHandler<DataObjectTO> dataObjectNameColHandler = new ColumnSortEvent.ListHandler<DataObjectTO>(dataObjectsProvider.getList());
        dataObjectNameColHandler.setComparator(dataObjectColumn, new DataObjectComparator());
        dataObjectsTable.addColumnSortHandler(dataObjectNameColHandler);
        dataObjectsTable.getColumnSortList().push(dataObjectColumn);

        //Init the selection model
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {

            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if (!skipNextOnChange) {
                    DataObjectTO selectedObjectTO = ((SingleSelectionModel<DataObjectTO>)dataObjectsTable.getSelectionModel()).getSelectedObject();
                    notifyObjectSelected(selectedObjectTO);
                }
                skipNextOnChange = false;
            }
        });

        dataObjectsTable.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.BOUND_TO_SELECTION);
        dataObjectsTable.setSelectionModel(selectionModel);

        dataObjectsProvider.addDataDisplay(dataObjectsTable);
        dataObjectsProvider.refresh();

        newEntityButton.setIcon(IconType.PLUS_SIGN);
    }

    public DataModelTO getDataModel() {
        return dataModel;
    }

    public void setDataModel(DataModelTO dataModel) {
        this.dataModel = dataModel;
        this.dataObjects = dataModel.getDataObjects();
        modelName.setText(dataModel.getName());
//        newDataObjectPopup.setDataModel(getDataModel());


        //We create a new selection model due to a bug found in GWT when we change e.g. from one data object with 9 rows
        // to one with 3 rows and the table was sorted.
        //Several tests has been done and the final workaround (not too bad) we found is to
        // 1) sort the table again
        // 2) create a new selection model
        // 3) populate the table with new items
        // 3) select the first row
        //dataObjectPropertiesTable.getColumnSortList().push(dataObjectPropertiesTable.getColumn(0));


        SingleSelectionModel selectionModel2 = new SingleSelectionModel<DataObjectTO>();
        dataObjectsTable.setSelectionModel(selectionModel2);

        selectionModel2.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {

            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                if (!skipNextOnChange) {
                    DataObjectTO selectedObjectTO = ((SingleSelectionModel<DataObjectTO>)dataObjectsTable.getSelectionModel()).getSelectedObject();
                    notifyObjectSelected(selectedObjectTO);
                }
                skipNextOnChange = false;
            }
        });

        ArrayList<DataObjectTO> sortBuffer = new ArrayList<DataObjectTO>();
        sortBuffer.addAll(dataObjects);
        Collections.sort(sortBuffer, new DataObjectComparator());

        dataObjectsProvider.getList().clear();
        dataObjectsProvider.getList().addAll(sortBuffer);
        dataObjectsProvider.flush();
        dataObjectsProvider.refresh();

        dataObjectsTable.getColumnSortList().push(new ColumnSortList.ColumnSortInfo(dataObjectsTable.getColumn(1), true));

        if (dataObjects.size() > 0) {
            dataObjectsTable.setKeyboardSelectedRow(0);
            selectionModel2.setSelected(sortBuffer.get(0), true);
        }

        //set the first row selected again. Sounds crazy, but's part of the workaround, don't remove this line.
        if (dataObjects.size() > 0) {
            dataObjectsTable.setKeyboardSelectedRow(0);
        }

    }

    public DataModelEditorPresenter getModelEditorPresenter() {
        return modelEditorPresenter;
    }

    public void setModelEditorPresenter(DataModelEditorPresenter modelEditorPresenter) {
        this.modelEditorPresenter = modelEditorPresenter;
    }

    @UiHandler("newEntityButton")
    void newEntityClick( ClickEvent event ) {
        newDataObjectPopup.setDataModel(getDataModel());
        newDataObjectPopup.show();
    }

    public void selectDataObject(DataObjectTO dataObject) {
        int index = dataObjectsProvider.getList().indexOf(dataObject);
        if (index >= 0) {
            //when the selected object is set programatically an onSelectionChange event is produced
            //but we want to avoid this redundant (in this case) event because the object has already been selected.
            skipNextOnChange = true;
            ((SingleSelectionModel<DataObjectTO>)dataObjectsTable.getSelectionModel()).setSelected(dataObject, true);
            //selectionModel.setSelected(dataObject, true);
            dataObjectsTable.setKeyboardSelectedRow(index);
        }
    }

    private void addDataObject(DataObjectTO dataObject) {
        dataObjectsProvider.getList().add(dataObject);
        dataObjectsProvider.flush();
        dataObjectsProvider.refresh();

        int index = dataObjectsProvider.getList().size();
        index = index > 0 ? (index-1) : 0;

        dataObjectsTable.setKeyboardSelectedRow(index);
    }

    private void deleteDataObject(final DataObjectTO dataObjectTO, final int index) {

        validatorService.canDeleteDataObject(dataObjectTO, getDataModel(), new ValidatorCallback() {
            @Override
            public void onFailure() {
                ErrorPopup.showMessage("The data object with identifier: " + dataObjectTO.getName() + " cannot be deleted because it is still referenced within the model.");
            }

            @Override
            public void onSuccess() {
                getDataModel().removeDataObject(dataObjectTO);

                dataObjectsProvider.getList().remove(index);
                dataObjectsProvider.flush();
                dataObjectsProvider.refresh();

                notifyObjectDeleted(dataObjectTO);
            }
        });
    }

    //Event Observers

    private void onDataObjectCreated(@Observes DataObjectCreatedEvent event) {
        if (event.isFrom(getDataModel())) {
            addDataObject(event.getCurrentDataObject());
        }
    }

    private void onDataObjectChange(@Observes DataObjectChangeEvent event) {

        if (event.isFrom(getDataModel())) {
            if ("name".equals(event.getPropertyName())) {
                //by now we only need to refresh the row if the name changed
                int row = 0;

                for (DataObjectTO dataObjectTO : dataObjectsProvider.getList()) {
                    if (event.getCurrentDataObject() == dataObjectTO) {
                        dataObjectsTable.redrawRow(row);
                        break;
                    }
                    row++;
                }
            }
        }
    }

    private void onDataObjectSelected(@Observes DataObjectSelectedEvent event) {
        if (event.isFrom(getDataModel())) {
            if (event.isFrom(DataModelerEvent.DATA_OBJECT_BROWSER) || event.isFrom(DataModelerEvent.DATA_MODEL_BREAD_CRUMB)) {
                //It's a data object selection in another related widget, select the object in the browser
                //but don't fire selection event.
                skipNextOnChange = true;
                selectDataObject(event.getCurrentDataObject());
            }
        }
    }

    //Event notifications

    private void notifyObjectSelected(DataObjectTO selectedObjectTO) {
        dataModelerEvent.fire(new DataObjectSelectedEvent(DataModelerEvent.DATA_MODEL_BROWSER, getDataModel(), selectedObjectTO));
    }

    private void notifyObjectDeleted(DataObjectTO dataObject) {
        getDataModel().getHelper().dataObjectDeleted(dataObject.getClassName());
        dataModelerEvent.fire(new DataObjectDeletedEvent(DataModelerEvent.DATA_MODEL_BROWSER, getDataModel(), dataObject));
        notification.fire(new NotificationEvent(Constants.INSTANCE.modelEditor_notification_dataObject_deleted(dataObject.getName())));
    }

}