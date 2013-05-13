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


import org.jbpm.datamodeler.editor.model.AnnotationDefinitionTO;
import org.jbpm.datamodeler.editor.model.DataModelTO;

import java.util.Map;

public class DataModelerContext {

    private DataModelTO dataModel;

    private DataModelHelper helper;

    Map<String, AnnotationDefinitionTO> annotationDefinitions;

    private boolean dirty = false;

    public DataModelerContext(DataModelTO dataModel, Map<String, AnnotationDefinitionTO> annotationDefinitions) {
        this(dataModel);
        this.annotationDefinitions = annotationDefinitions;
    }

    public DataModelerContext(DataModelTO dataModel) {
        this.dataModel = dataModel;
        helper = new DataModelHelper(dataModel);
    }

    public DataModelTO getDataModel() {
        return dataModel;
    }

    public void setDataModel(DataModelTO dataModel) {
        this.dataModel = dataModel;
    }

    public DataModelHelper getHelper() {
        return helper;
    }

    public void setHelper(DataModelHelper helper) {
        this.helper = helper;
    }

    public Map<String, AnnotationDefinitionTO> getAnnotationDefinitions() {
        return annotationDefinitions;
    }

    public void setAnnotationDefinitions(Map<String, AnnotationDefinitionTO> annotationDefinitions) {
        this.annotationDefinitions = annotationDefinitions;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
