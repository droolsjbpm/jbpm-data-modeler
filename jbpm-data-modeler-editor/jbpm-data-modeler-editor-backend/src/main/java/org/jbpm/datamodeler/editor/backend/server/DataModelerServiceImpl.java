/*
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.datamodeler.editor.backend.server;

import org.jboss.errai.bus.server.annotations.Service;
import org.jbpm.datamodeler.commons.NamingUtils;
import org.jbpm.datamodeler.commons.file.FileUtils;
import org.jbpm.datamodeler.core.AnnotationDefinition;
import org.jbpm.datamodeler.core.DataModel;
import org.jbpm.datamodeler.core.PropertyType;
import org.jbpm.datamodeler.core.impl.PropertyTypeFactoryImpl;
import org.jbpm.datamodeler.driver.FileChangeDescriptor;
import org.jbpm.datamodeler.driver.impl.DataModelOracleDriver;
import org.jbpm.datamodeler.editor.model.AnnotationDefinitionTO;
import org.jbpm.datamodeler.editor.model.DataModelTO;
import org.jbpm.datamodeler.editor.model.DataObjectTO;
import org.jbpm.datamodeler.editor.model.PropertyTypeTO;
import org.jbpm.datamodeler.editor.service.DataModelerService;
import org.jbpm.datamodeler.editor.service.ServiceException;
import org.jbpm.datamodeler.validation.ValidationUtils;
import org.kie.commons.io.IOService;
import org.kie.commons.java.nio.IOException;
import org.kie.commons.java.nio.file.Files;
import org.kie.guvnor.datamodel.events.InvalidateDMOProjectCacheEvent;
import org.kie.guvnor.datamodel.oracle.ProjectDataModelOracle;
import org.kie.guvnor.datamodel.service.DataModelService;
import org.kie.guvnor.project.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.backend.server.util.Paths;
import org.uberfire.backend.vfs.Path;
import org.uberfire.client.workbench.widgets.events.ChangeType;
import org.uberfire.client.workbench.widgets.events.ResourceBatchChangesEvent;
import org.uberfire.client.workbench.widgets.events.ResourceChange;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;

@Service
@ApplicationScoped
public class DataModelerServiceImpl implements DataModelerService {

    private static final Logger logger = LoggerFactory.getLogger(DataModelerServiceImpl.class);

    private static final String MAIN_JAVA_PATH = "src/main/java";
    private static final String MAIN_RESOURCES_PATH = "src/main/resources";
    private static final String TEST_JAVA_PATH        = "src/test/java";
    private static final String TEST_RESOURCES_PATH   = "src/test/resources";

    private static final String DEFAULT_GUVNOR_PKG = "defaultpkg";

    @Inject
    @Named("ioStrategy")
    IOService ioService;

    @Inject
    private Paths paths;

    @Inject
    private ProjectService projectService;

    @Inject
    private DataModelService dataModelService;

    @Inject
    private Event<InvalidateDMOProjectCacheEvent> invalidateDMOProjectCache;

    @Inject
    private Event<ResourceBatchChangesEvent> resourceBatchChangesEvent;


    public DataModelerServiceImpl() {
    }

    @Override
    public Path createModel(Path context, String fileName) {

        //TODO remove this method if the model file is no longer created
        return context;
    }

    @Override
    public DataModelTO loadModel(Path path) {

        if (logger.isDebugEnabled()) logger.debug("Loading data model from path: " + path);

        DataModel dataModel = null;
        Path projectPath = null;

        try {
            projectPath = projectService.resolveProject(path);
            if (logger.isDebugEnabled()) logger.debug("Current project path is: " + projectPath);

            ProjectDataModelOracle projectDataModelOracle = dataModelService.getProjectDataModel(projectPath);

            DataModelOracleDriver driver = DataModelOracleDriver.getInstance();
            dataModel = driver.loadModel(projectDataModelOracle);

            //Objects read from persistent .java format are tagged as PERSISTENT objects
            DataModelTO dataModelTO = DataModelerServiceHelper.getInstance().domain2To(dataModel, DataObjectTO.PERSISTENT);

            //TODO remove this guarrada
            dataModelTO.setExternalClasses(Arrays.asList("java.lang.ref.PhantomReference", "java.util.regex.Matcher"));
            return dataModelTO;

        } catch (Exception e) {
            logger.error("Data model couldn't be loaded, path: " + path + ", projectPath: " + projectPath + ".", e);
            throw new ServiceException("Data model couldn't be loaded, path: " + path + ", projectPath: " + projectPath + ".", e);
        }
    }

    @Override
    public void saveModel(DataModelTO dataModel, final Path path) {
        
        try {

            //get the path to project root directory (the main pom.xml directory) and calculate
            //the java sources path
            Path projectPath = projectService.resolveProject(path);

            //ensure java sources directory exists.
            org.kie.commons.java.nio.file.Path javaPath = ensureProjectJavaPath(paths.convert(projectPath));

            //clean the files that needs to be deleted prior to model generation.
            List<ResourceChange> localChanges = cleanupFiles(dataModel, javaPath);

            //convert to domain model
            DataModel dataModelDomain = DataModelerServiceHelper.getInstance().to2Domain(dataModel);

            //invalidate ProjectDataModelOracle for this project.
            invalidateDMOProjectCache.fire( new InvalidateDMOProjectCacheEvent( projectPath ) );
            
            DataModelOracleDriver driver = DataModelOracleDriver.getInstance();
            List<FileChangeDescriptor> driverChanges = driver.generateModel(dataModelDomain, ioService, javaPath);

            //clean empty java directories
            cleanupEmptyDirs(javaPath);

            notifyFileChanges(localChanges, driverChanges);

        } catch (Exception e) {
            logger.error("An error was produced during data model generation, dataModel: " + dataModel + ", path: " + path, e);
            throw new ServiceException("Data model: " + dataModel.getName() + ", couldn't be generated due to the following error. " + e);
        }
    }

    @Override
    public List<PropertyTypeTO> getBasePropertyTypes() {
        List<PropertyTypeTO> types = new ArrayList<PropertyTypeTO>();

        for (PropertyType baseType : PropertyTypeFactoryImpl.getInstance().getBasePropertyTypes()) {
            types.add(new PropertyTypeTO(baseType.getName(), baseType.getClassName()));
        }
        return types;
    }

    @Override
    public Path resolveProject(Path path) {
        return projectService.resolveProject(path);
    }

    @Override
    public Map<String, Boolean> evaluateIdentifiers(String[] identifiers) {
        Map<String, Boolean> result = new HashMap<String, Boolean>(identifiers.length);
        if (identifiers != null && identifiers.length > 0) {
            for (String s : identifiers) {
                result.put(s, ValidationUtils.isJavaIdentifier(s));
            }
        }
        return result;
    }

    @Override
    public Map<String, AnnotationDefinitionTO> getAnnotationDefinitions() {
        Map<String, AnnotationDefinitionTO> annotations = new HashMap<String, AnnotationDefinitionTO>();
        List<AnnotationDefinition> annotationDefinitions = DataModelOracleDriver.getInstance().getConfiguredAnnotations();
        AnnotationDefinitionTO annotationDefinitionTO;
        DataModelerServiceHelper serviceHelper = DataModelerServiceHelper.getInstance();

        for (AnnotationDefinition annotationDefinition : annotationDefinitions) {
            annotationDefinitionTO = serviceHelper.domain2To(annotationDefinition);
            annotations.put(annotationDefinitionTO.getClassName(), annotationDefinitionTO);
        }
        return annotations;
    }

    @Override
    public Path resolveResourcePackage(final Path resource) {

        //TODO this method should be moved to the ProjectService class
        //Null resource paths cannot resolve to a Project
        if ( resource == null ) {
            return null;
        }

        //If Path is not within a Project we cannot resolve a package
        final Path projectRoot = projectService.resolveProject(resource);
        if ( projectRoot == null ) {
            return null;
        }

        //The Path must be within a Project's src/main/resources or src/test/resources path
        boolean resolved = false;
        org.kie.commons.java.nio.file.Path path = paths.convert( resource ).normalize();
        final org.kie.commons.java.nio.file.Path srcResourcesPath = paths.convert( projectRoot ).resolve(MAIN_RESOURCES_PATH);
        final org.kie.commons.java.nio.file.Path testResourcesPath = paths.convert( projectRoot ).resolve( TEST_RESOURCES_PATH );

        if ( path.startsWith( srcResourcesPath ) ) {
            resolved = true;
        } else if ( path.startsWith( testResourcesPath ) ) {
            resolved = true;
        }
        if ( !resolved ) {
            return null;
        }

        //If the Path is already a folder simply return it
        if ( Files.isDirectory(path) ) {
            return resource;
        }

        path = path.getParent();

        return paths.convert( path );
    }

    private void notifyFileChanges(List<ResourceChange> localChanges, List<FileChangeDescriptor> driverChanges) {

        Set<ResourceChange> batchChanges = new HashSet<ResourceChange>();
        batchChanges.addAll(localChanges);

        for (FileChangeDescriptor driverChange : driverChanges) {
            switch (driverChange.getAction()) {
                case FileChangeDescriptor.ADD:
                    logger.debug("Notifying file created: " + driverChange.getPath());
                    batchChanges.add(new ResourceChange(ChangeType.ADD, paths.convert(driverChange.getPath())));
                    break;
                case FileChangeDescriptor.DELETE:
                    logger.debug("Notifying file deleted: " + driverChange.getPath());
                    batchChanges.add(new ResourceChange(ChangeType.DELETE, paths.convert(driverChange.getPath())));
                    break;
                case FileChangeDescriptor.UPDATE:
                    logger.debug("Notifying file updated: " + driverChange.getPath());
                    batchChanges.add(new ResourceChange(ChangeType.UPDATE, paths.convert(driverChange.getPath())));
                    break;
            }
        }
        if (batchChanges.size() > 0) {
            resourceBatchChangesEvent.fire(new ResourceBatchChangesEvent(batchChanges));
        }
    }

    /**
     * This auxiliary method deletes the files that belongs to data objects that was removed in memory.
     *
     */
    private List<ResourceChange> cleanupFiles(DataModelTO dataModel, org.kie.commons.java.nio.file.Path javaPath) {

        List<DataObjectTO> currentObjects = dataModel.getDataObjects();
        List<DataObjectTO> deletedObjects = dataModel.getDeletedDataObjects();
        List<ResourceChange> fileChanges = new ArrayList<ResourceChange>();
        org.kie.commons.java.nio.file.Path filePath;

        //process deleted persistent objects.
        for (DataObjectTO dataObject : deletedObjects) {
            if (dataObject.isPersistent()) {
                filePath = calculateFilePath(dataObject.getOriginalClassName(), javaPath);
                if (dataModel.getDataObjectByClassName(dataObject.getOriginalClassName()) != null) {
                    //TODO check if we need to have this level of control or instead we remove this file directly.
                    //very particular case a persistent object was deleted in memory and a new one with the same name
                    //was created. At the end we will have a file update instead of a delete.

                    //do nothing, the file generator will notify that the file changed.
                    //fileChanges.add(new FileChangeDescriptor(paths.convert(filePath), FileChangeDescriptor.UPDATE));
                } else {
                    fileChanges.add(new ResourceChange(ChangeType.DELETE, paths.convert(filePath)));
                    ioService.delete(filePath);
                }
            }
        }

        //process package or class name changes for persistent objects.
        for (DataObjectTO dataObject : currentObjects) {
            if (dataObject.isPersistent() && dataObject.classNameChanged()) {
                //if the className changes the old file needs to be removed
                filePath = calculateFilePath(dataObject.getOriginalClassName(), javaPath);

                if (dataModel.getDataObjectByClassName(dataObject.getOriginalClassName()) != null) {
                    //TODO check if we need to have this level of control or instead we remove this file directly.
                    //very particular case of change, a persistent object changes the name to the name of another
                    //object. A kind of name swapping...

                    //do nothing, the file generator will notify that the file changed.
                    //fileChanges.add(new FileChangeDescriptor(paths.convert(filePath), FileChangeDescriptor.UPDATE));
                } else {
                    fileChanges.add(new ResourceChange(ChangeType.DELETE, paths.convert(filePath)));
                    ioService.delete(filePath);
                }
            }
        }

        return  fileChanges;
    }

    private void cleanupEmptyDirs(org.kie.commons.java.nio.file.Path pojectPath) {
        FileUtils fileUtils = FileUtils.getInstance();
        List<String> deleteableFiles = new ArrayList<String>();
        deleteableFiles.add(".gitignore");
        fileUtils.cleanEmptyDirectories(ioService, pojectPath, false, deleteableFiles);
    }

    private org.kie.commons.java.nio.file.Path existsProjectJavaPath(org.kie.commons.java.nio.file.Path projectPath) {
        org.kie.commons.java.nio.file.Path javaPath = projectPath.resolve("src").resolve("main").resolve("java");
        if (ioService.exists(javaPath)) {
            return javaPath;
        }
        return null;
    }

    private org.kie.commons.java.nio.file.Path ensureProjectJavaPath(org.kie.commons.java.nio.file.Path projectPath) {
        org.kie.commons.java.nio.file.Path javaPath = projectPath.resolve("src");
        if (!ioService.exists(javaPath)) {
            javaPath = ioService.createDirectory(javaPath);
        }
        javaPath = javaPath.resolve("main");
        if (!ioService.exists(javaPath)) {
            javaPath = ioService.createDirectory(javaPath);
        }
        javaPath = javaPath.resolve("java");
        if (!ioService.exists(javaPath)) {
            javaPath = ioService.createDirectory(javaPath);
        }

        return javaPath;
    }

    /**
     * Given a className calculates the path to the java file allocating the corresponding pojo.
     *
     */
    private org.kie.commons.java.nio.file.Path calculateFilePath(String className, org.kie.commons.java.nio.file.Path javaPath) {

        String name = NamingUtils.getInstance().extractClassName(className);
        String packageName = NamingUtils.getInstance().extractPackageName(className);
        org.kie.commons.java.nio.file.Path filePath = javaPath;

        if (packageName != null) {
            List<String> packageNameTokens = NamingUtils.getInstance().tokenizePackageName(packageName);
            for (String token : packageNameTokens) {
                filePath = filePath.resolve(token);
            }
        }

        filePath = filePath.resolve(name + ".java");
        return filePath;
    }

    private List<Path> calculateProjectPackages(IOService ioService, Path path) throws IOException {
            
        Collection<FileUtils.ScanResult> scanResults;
        List<Path> results = new ArrayList<Path>();

        FileUtils fileUtils = FileUtils.getInstance();

        Path projectHome = projectService.resolveProject(path);
        org.kie.commons.java.nio.file.Path javaPath = existsProjectJavaPath(paths.convert(projectHome));
        if (javaPath != null) {
            scanResults = fileUtils.scanDirectories(ioService, javaPath, false, true);
            for (FileUtils.ScanResult scanResult : scanResults) {
                results.add(paths.convert(scanResult.getFile()));
            }

        }
        return results;
    }

}