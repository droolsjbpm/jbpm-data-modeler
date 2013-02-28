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

package org.jbpm.datamodeler.editor.model;

import org.jboss.errai.common.client.api.annotations.Portable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@Portable
public class DataModelTO implements Serializable {
    
    private String name;

    private List<DataObjectTO> dataObjects = new ArrayList<DataObjectTO>();

    public DataModelTO() {
    }

    public DataModelTO(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<DataObjectTO> getDataObjects() {
        return dataObjects;
    }

    public void setDataObjects(List<DataObjectTO> dataObjects) {
        this.dataObjects = dataObjects;
    }
}

