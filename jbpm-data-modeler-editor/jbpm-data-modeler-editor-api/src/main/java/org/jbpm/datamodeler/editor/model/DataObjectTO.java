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
public class DataObjectTO implements Serializable {

    /*
     * Data objects that was read form persistent status, .java files.
     */
    public static final Integer PERSISTENT = 0;


    /*
     * Data objects that was created in memory an was not saved to persistent .java file yet.
     */
    public static final Integer VOLATILE = 1;

    private String name;
    
    private String packageName;
    
    private String superClassName;
    
    private int status = VOLATILE;


    //Remembers the original name for the DataObject.
    //This value shouldn't be changed.
    private String originalClassName;

    private List<ObjectPropertyTO> properties = new ArrayList<ObjectPropertyTO>();

    public DataObjectTO() {
    }

    public DataObjectTO(String name) {
        this();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return ( (packageName != null && !"".equals(packageName)) ? packageName+"." : "") + getName();
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public List<ObjectPropertyTO> getProperties() {
        return properties;
    }

    public ObjectPropertyTO getProperty(String name) {
        for (ObjectPropertyTO property : properties) {
            if (property.getName().equals(name)) return property;
        }
        return null;
    }

    public void setProperties(List<ObjectPropertyTO> properties) {
        this.properties = properties;
    }

    public String getOriginalClassName() {
        return originalClassName;
    }

    public void setOriginalClassName(String originalClassName) {
        this.originalClassName = originalClassName;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public void setSuperClassName(String superClassName) {
        this.superClassName = superClassName;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isVolatile() {
        return getStatus() == VOLATILE;
    }
    
    public boolean isPersistent() {
        return getStatus() == PERSISTENT;
    }

    public boolean classNameChanged() {
        return isPersistent() && !getClassName().equals(getOriginalClassName());
    }

    public boolean packageNameChanged() {
        if (isPersistent()) {
            //extract package name.
            int index = getOriginalClassName().lastIndexOf(".");
            String originalPackageName = "";
            if (index > 0) {
                originalPackageName = getOriginalClassName().substring(0, index);
                return originalPackageName.equals(getPackageName());
            } else {
                return getPackageName() != null;
            }
        }
        return false;
    }

    /*

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataObjectTO that = (DataObjectTO) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }
    */

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}