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

package org.jbpm.datamodeler.editor.client.handlers;

import com.google.gwt.core.client.Callback;
import com.google.gwt.user.client.ui.IsWidget;
import org.jboss.errai.bus.client.api.RemoteCallback;
import org.jboss.errai.ioc.client.api.Caller;
import org.jbpm.datamodeler.editor.client.editors.resources.i18n.Constants;
import org.jbpm.datamodeler.editor.client.type.DataModelResourceType;
import org.jbpm.datamodeler.editor.service.DataModelerService;
import org.kie.guvnor.commons.ui.client.handlers.DefaultNewResourceHandler;
import org.kie.guvnor.commons.ui.client.handlers.NewResourcePresenter;
import org.uberfire.backend.vfs.Path;
import org.uberfire.client.common.BusyPopup;
import org.uberfire.client.mvp.PlaceManager;
import org.uberfire.shared.mvp.PlaceRequest;
import org.uberfire.shared.mvp.impl.PathPlaceRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class NewDataModelHandler extends DefaultNewResourceHandler {

    @Inject
    private Caller<DataModelerService> modelerService;

    @Inject
    private PlaceManager placeManager;

    @Inject
    private DataModelResourceType resourceType;

    @Override
    public String getDescription() {
        return Constants.INSTANCE.modelEditor_newModel();
    }

    @Override
    public IsWidget getIcon() {
        return null;
    }

    @Override
    public void acceptPath(final Path path, final Callback<Boolean, Void> callback) {
        modelerService.call( new RemoteCallback<Path>() {
            @Override
            public void callback( final Path path ) {
                callback.onSuccess( path != null );
            }
        } ).resolveResourcePackage(path);
    }

    @Override
    public void create(final Path context,
                        final String baseFileName,
                        final NewResourcePresenter presenter) {

        BusyPopup.showMessage("Creating datamodel");
        modelerService.call( new RemoteCallback<Path>() {
            @Override
            public void callback( final Path path ) {                
                BusyPopup.close();
                presenter.complete();
                notifySuccess();
                final PlaceRequest place = new PathPlaceRequest(path, "DataModelEditor");
                placeManager.goTo(place);
            }
        } ).createModel(context, buildFileName(resourceType, baseFileName));
    }

}
