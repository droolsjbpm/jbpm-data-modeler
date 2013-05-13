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

package org.jbpm.datamodeler.driver.impl;


import org.jbpm.datamodeler.codegen.GenerationContext;
import org.jbpm.datamodeler.codegen.GenerationEngine;
import org.jbpm.datamodeler.codegen.GenerationListener;
import org.jbpm.datamodeler.commons.NamingUtils;
import org.jbpm.datamodeler.core.AnnotationDefinition;
import org.jbpm.datamodeler.core.DataModel;
import org.jbpm.datamodeler.core.DataObject;
import org.jbpm.datamodeler.core.ObjectProperty;
import org.jbpm.datamodeler.core.impl.ModelFactoryImpl;
import org.jbpm.datamodeler.driver.AnnotationDriver;
import org.jbpm.datamodeler.driver.FileChangeDescriptor;
import org.jbpm.datamodeler.driver.ModelDriver;
import org.jbpm.datamodeler.driver.ModelDriverException;
import org.jbpm.datamodeler.driver.impl.annotations.*;
import org.kie.commons.io.IOService;
import org.kie.commons.java.nio.file.Path;
import org.kie.guvnor.datamodel.model.Annotation;
import org.kie.guvnor.datamodel.model.ModelField;
import org.kie.guvnor.datamodel.oracle.ProjectDataModelOracle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DataModelOracleDriver implements ModelDriver {
    
    private static final Logger logger = LoggerFactory.getLogger(DataModelOracleDriver.class);

    private List<AnnotationDefinition> configuredAnnotations = new ArrayList<AnnotationDefinition>();

    private Map<String, AnnotationDriver> annotationDrivers = new HashMap<String, AnnotationDriver>();

    public static DataModelOracleDriver getInstance() {
        return new DataModelOracleDriver();
    }

    protected DataModelOracleDriver() {
        AnnotationDefinition annotationDefinition = DescriptionAnnotationDefinition.getInstance();
        configuredAnnotations.add(annotationDefinition);
        annotationDrivers.put(annotationDefinition.getClassName(), new DefaultOracleAnnotationDriver());

        annotationDefinition = EqualsAnnotationDefinition.getInstance();
        configuredAnnotations.add(annotationDefinition);
        annotationDrivers.put(annotationDefinition.getClassName(), new DefaultOracleAnnotationDriver());

        annotationDefinition = LabelAnnotationDefinition.getInstance();
        configuredAnnotations.add(annotationDefinition);
        annotationDrivers.put(annotationDefinition.getClassName(), new DefaultOracleAnnotationDriver());

        annotationDefinition = RoleAnnotationDefinition.getInstance();
        configuredAnnotations.add(annotationDefinition);
        annotationDrivers.put(annotationDefinition.getClassName(), new DefaultOracleAnnotationDriver());

        annotationDefinition = PositionAnnotationDefinition.getInstance();
        configuredAnnotations.add(annotationDefinition);
        annotationDrivers.put(annotationDefinition.getClassName(), new DefaultOracleAnnotationDriver());
                
    }

    @Override
    public List<AnnotationDefinition> getConfiguredAnnotations() {
        return configuredAnnotations;
    }

    @Override
    public AnnotationDefinition getConfiguredAnnotation(String annotationClassName) {
        for (AnnotationDefinition annotationDefinition : configuredAnnotations) {
            if (annotationClassName.equals(annotationDefinition.getClassName())) return annotationDefinition;
        }
        return null;
    }

    @Override
    public AnnotationDriver getAnnotationDriver(String annotationClassName) {
        return annotationDrivers.get(annotationClassName);
    }

    @Override
    public List<FileChangeDescriptor> generateModel(DataModel dataModel, IOService ioService, Path root) throws Exception {

        GenerationContext generationContext = new GenerationContext(dataModel);
        OracleGenerationListener generationListener = new OracleGenerationListener(ioService, root);
        generationContext.setGenerationListener(generationListener);

        GenerationEngine generationEngine = GenerationEngine.getInstance();
        generationEngine.generate(generationContext);
        return generationListener.getFileChanges();
    }

    @Override
    public DataModel createModel() {
        return ModelFactoryImpl.getInstance().newModel();
    }

    public DataModel loadModel(ProjectDataModelOracle oracleDataModel) throws ModelDriverException {

        DataModel dataModel = createModel();

        logger.debug("Adding oracleDataModel: " + oracleDataModel + " to dataModel: " + dataModel);
        
        String[] factTypes = oracleDataModel.getFactTypes();
        
        if (factTypes != null && factTypes.length > 0) {
            for (int i = 0; i < factTypes.length; i++) {
                //skip .drl declared fact types.
                if (isDataObject(oracleDataModel, factTypes[i])) {
                    addFactType(dataModel, oracleDataModel, factTypes[i]);
                }
            }
        } else {
            logger.debug("oracleDataModel hasn't defined fact types");
        }
        return dataModel;
    }

    private void addFactType(DataModel dataModel, ProjectDataModelOracle oracleDataModel, String factType) throws ModelDriverException {

        String packageName = NamingUtils.getInstance().extractPackageName(factType);
        String className = NamingUtils.getInstance().extractClassName(factType);
        String superClass = oracleDataModel.getSuperType(factType);

        logger.debug("Adding factType: " + factType + ", to dataModel: " + dataModel + ", from oracleDataModel: " + oracleDataModel);
        DataObject dataObject = dataModel.addDataObject(factType);
        dataObject.setSuperClassName(superClass);

        //process type annotations
        Set<Annotation> typeAnnotations = oracleDataModel.getTypeAnnotations(factType);
        if (typeAnnotations != null) {
            for (Annotation annotation : typeAnnotations) {
                addFactTypeAnnotation(dataObject, annotation);
            }
        }
        
        Map<String, ModelField[]> fields = oracleDataModel.getModelFields();
        if (fields != null) {
            ModelField[] factFields = fields.get(factType);
            ModelField field;
            ObjectProperty property;
            Map<String, Set<Annotation>> typeFieldsAnnotations = oracleDataModel.getTypeFieldsAnnotations(factType);
            Set<Annotation> fieldAnnotations;
            if (factFields != null && factFields.length > 0) {
                for (int j = 0; j < factFields.length ; j++) {
                    field = factFields[j];
                    if (isLoadableField(field)) {
                        
                        if (field.getType().equals("Collection")) {
                            //particular processing for collection types
                            //read the correction bag and item classes.
                            String bag = oracleDataModel.getFieldClassName(factType, field.getName());
                            String itemsClass = oracleDataModel.getParametricFieldType(factType, field.getName());
                            property = dataObject.addProperty(field.getName(), itemsClass, true, bag);

                        } else {
                            property = dataObject.addProperty(field.getName(), getFieldType(oracleDataModel, packageName, field.getClassName()));
                        }

                        //process property annotations
                        if (typeFieldsAnnotations != null && (fieldAnnotations = typeFieldsAnnotations.get(field.getName())) != null) {
                            for (Annotation fieldAnnotation : fieldAnnotations) {
                                addFieldAnnotation(dataObject, property, fieldAnnotation);
                            }
                        }
                    }
                }
            }
        } else {
            logger.debug("No fields for factTye: " + factType);
        }
    }

    private void addFactTypeAnnotation(DataObject dataObject, Annotation annotationToken) throws ModelDriverException {
        org.jbpm.datamodeler.core.Annotation annotation = createAnnotation(annotationToken);
        if (annotation != null) dataObject.addAnnotation(annotation);
    }

    private void addFieldAnnotation(DataObject dataObject, ObjectProperty property, Annotation annotationToken) throws ModelDriverException {
        org.jbpm.datamodeler.core.Annotation annotation = createAnnotation(annotationToken);
        if (annotation != null) property.addAnnotation(annotation);
    }

    private org.jbpm.datamodeler.core.Annotation createAnnotation(Annotation annotationToken) throws ModelDriverException {

        AnnotationDefinition annotationDefinition = getConfiguredAnnotation(annotationToken.getQualifiedTypeName());
        org.jbpm.datamodeler.core.Annotation annotation = null;

        if (annotationDefinition != null) {
            AnnotationDriver annotationDriver = getAnnotationDriver(annotationDefinition.getClassName());
            if (annotationDriver != null) {
                annotation = annotationDriver.buildAnnotation(annotationDefinition, annotationToken);
            } else {
                logger.warn("AnnotationDriver for annotation: " + annotationToken.getQualifiedTypeName() + " is not configured for this driver");
            }
        } else {
            logger.warn("Annotation: " + annotationToken.getQualifiedTypeName() + " is not configured for this driver.");
        }
        return annotation;
    }

    private String getFieldType(ProjectDataModelOracle oracleDataModel, String packageName, String fieldType) {
        String primitiveClass = NamingUtils.getInstance().getClassForPrimitiveTypeId(fieldType);
        if (primitiveClass != null) return primitiveClass;
        return fieldType;
    }

    /**
     * True if the given fact type is a DataObject.
     */
    private boolean isDataObject(ProjectDataModelOracle oracleDataModel, String factType) {
        return !oracleDataModel.isDeclaredType(factType);
    }

    /**
     * Indicates if this field should be loaded or not.
     * Some fields like a filed with name "this" shouldn't be loaded.
     */
    private boolean isLoadableField(ModelField field) {
        return !"this".equals(field.getName());
    }

    static class OracleGenerationListener implements GenerationListener {

        org.kie.commons.java.nio.file.Path output;
        
        IOService ioService;

        List<FileChangeDescriptor> fileChanges = new ArrayList<FileChangeDescriptor>();

        public OracleGenerationListener(IOService ioService, org.kie.commons.java.nio.file.Path output) {
            this.ioService = ioService;
            this.output = output;
        }

        @Override
        public void assetGenerated(String fileName, String content) {

            String subDir;
            org.kie.commons.java.nio.file.Path subDirPath;
            org.kie.commons.java.nio.file.Path destFilePath;
            StringTokenizer dirNames;

            subDirPath = output;
            int index = fileName.lastIndexOf("/");
            if (index == 0) {
                //the file names was provided in the form /SomeFile.java
                fileName = fileName.substring(1, fileName.length());
            } else if (index > 0) {
                //the file name was provided in the most common form /dir1/dir2/SomeFile.java
                String dirNamesPath = fileName.substring(0, index);
                fileName = fileName.substring(index+1, fileName.length());
                dirNames = new StringTokenizer(dirNamesPath, "/");
                while (dirNames.hasMoreElements()) {
                    subDir = dirNames.nextToken();
                    subDirPath = subDirPath.resolve(subDir);
                    if (!ioService.exists(subDirPath)) {
                        ioService.createDirectory(subDirPath);
                    }
                }
            }

            //the last subDirPath is the directory to crate the file.
            destFilePath = subDirPath.resolve(fileName);
            boolean exists = ioService.exists(destFilePath);

            ioService.write(destFilePath, content);

            if (!exists) {
                if (logger.isDebugEnabled()) logger.debug("Genertion listener created a new file: " + destFilePath);
                fileChanges.add(new FileChangeDescriptor(destFilePath, FileChangeDescriptor.ADD));
            } else {
                if (logger.isDebugEnabled()) logger.debug("Generation listener modified file: " + destFilePath);
                fileChanges.add(new FileChangeDescriptor(destFilePath, FileChangeDescriptor.UPDATE));
            }
        }

        public List<FileChangeDescriptor> getFileChanges() {
            return fileChanges;
        }
    }
}