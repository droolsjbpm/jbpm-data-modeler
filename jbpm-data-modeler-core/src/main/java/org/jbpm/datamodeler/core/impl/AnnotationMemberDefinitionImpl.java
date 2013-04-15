package org.jbpm.datamodeler.core.impl;

import org.jbpm.datamodeler.core.AnnotationMemberDefinition;

public class AnnotationMemberDefinitionImpl implements AnnotationMemberDefinition {

    private String name;

    private String shortDescription;

    private String description;
    
    private String className;
    
    private Object defaultValue;

    public AnnotationMemberDefinitionImpl(String name, String className, String shortDescription, String description) {
        this(name, className, null, shortDescription, description);
    }

    public AnnotationMemberDefinitionImpl(String name, String className, Object defaultValue, String shortDescription, String description) {
        this.name = name;
        this.className = className;
        this.defaultValue = defaultValue;
        this.shortDescription = shortDescription;
        this.description = description;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Object defaultValue() {
        return defaultValue;
    }

    @Override
    public boolean isArray() {

        //TODO check this
        return getClassName() != null && className.endsWith("[]");
    }
}