//******************************************************************************
//                                       AcquisitionSessionDAO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 30 August, 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.dao.sesame;

import java.util.ArrayList;
import java.util.Collections;
import org.codehaus.janino.Java;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.configuration.URINamespaces;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.dao.phis.ExperimentDao;
import phis2ws.service.dao.phis.UserDaoPhisBrapi;
import phis2ws.service.model.User;
import phis2ws.service.resources.dto.FileMetadataDTO;
import phis2ws.service.resources.dto.FileMetadataPhenomobileDTO;
import phis2ws.service.resources.dto.FileMetadataUAVDTO;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.model.phis.Experiment;
import phis2ws.service.view.model.phis.RadiometricTarget;
import phis2ws.service.view.model.phis.Sensor;
import phis2ws.service.view.model.phis.Vector;

/**
 * This class is a Data Access Object for the acquisition sessions.
 * It gets the metadata for the excel file used to define acquisition sessions.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class AcquisitionSessionDAOSesame extends DAOSesame<Object> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(AcquisitionSessionDAOSesame.class);
    
    //The rdf type of the vector concerned by the acquisition session. 
    //Used in the GET fileMetadata to generate the informations for the file of definition the acquisition session for 4P.
    //e.g. http://www.phenome-fppn.fr/vocabulary/2017#UAV
    public String vectorRdfType;
    
    //Triplestore relations
    private final static URINamespaces NAMESPACES = new URINamespaces();
    final static String TRIPLESTORE_CONCEPT_EXPERIMENT = NAMESPACES.getObjectsProperty("cExperiment");
    final static String TRIPLESTORE_CONCEPT_FIELD_ROBOT = NAMESPACES.getObjectsProperty("cFieldRobot");
    final static String TRIPLESTORE_CONCEPT_UAV = NAMESPACES.getObjectsProperty("cUAV");
    final static String TRIPLESTORE_CONCEPT_VECTOR = NAMESPACES.getObjectsProperty("cVector");
    final static String TRIPLESTORE_PLATFORM = NAMESPACES.getContextsProperty("pxPlatform");

    @Override
    protected SPARQLQueryBuilder prepareSearchQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Integer count() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Get all the required informations to generate the hidden phis part of the excel file for 4P.
     * The content of the metadata depends of the acquisition session vector type.
     * @see AcquisitionSessionDAOSesame#allPaginateFileMetadata() 
     * @return the content of the metadata required.
     */
    private ArrayList<FileMetadataDTO> getFileMetadata() {
        ArrayList<FileMetadataDTO> fileMetadataList = new ArrayList<>();
        UriDaoSesame uriDaoSesame = new UriDaoSesame();
        //size of the higher list to generate all the FileMetadataDTO
        ArrayList<Integer> sizes = new ArrayList<>();
        
        //Lists of metadata per concept
        ArrayList<Experiment> experiments = new ArrayList<>();
        ArrayList<String> installations = new ArrayList<>();
        ArrayList<User> users = new ArrayList<>();
        ArrayList<Sensor> sensors = new ArrayList<>();
        ArrayList<Vector> vectors = new ArrayList<>();
        ArrayList<RadiometricTarget> radiometricTargets = new ArrayList<>();
        
        //if the vector is an uav or a field robot, it has specific file metadata
        if (uriDaoSesame.isSubClassOf(vectorRdfType, TRIPLESTORE_CONCEPT_FIELD_ROBOT)
                || uriDaoSesame.isSubClassOf(vectorRdfType, TRIPLESTORE_CONCEPT_UAV)) {
            //Common metadata
            //1. get the group plot list with the alias, uri and species (just the experiments in this version)
            ExperimentDao experimentDAO = new ExperimentDao();
            experiments = experimentDAO.getAllExperimentsForAcquisitionSessionFile();
            sizes.add(experiments.size());
            
            //2. get the pilots list
            UserDaoPhisBrapi userDAO = new UserDaoPhisBrapi();
            users = userDAO.getAllUsersEmails();
            sizes.add(users.size());
            
            //3. platform
            installations.add(TRIPLESTORE_PLATFORM);
            
            //Metadata for the uav
            if (uriDaoSesame.isSubClassOf(vectorRdfType, TRIPLESTORE_CONCEPT_UAV)) {
                //3. get the camera list
                SensorDAOSesame sensorDAO = new SensorDAOSesame();
                sensors = sensorDAO.getCameras();
                sizes.add(sensors.size());

                //4. get the vectors list
                VectorDAOSesame vectorDAO = new VectorDAOSesame();
                vectors = vectorDAO.getUAVs();
                sizes.add(vectors.size());

                //5. get the radiometric targets
                RadiometricTargetDAOSesame radiometricTargetDAO = new RadiometricTargetDAOSesame();
                radiometricTargets = radiometricTargetDAO.getRadiometricTargets();
                sizes.add(radiometricTargets.size());
            }
        }
        
        //generates the file metadata list
        int maxListSize = Collections.max(sizes);
        //field robot metadata
        if (uriDaoSesame.isSubClassOf(vectorRdfType, TRIPLESTORE_CONCEPT_FIELD_ROBOT)) {
            for (int i = 0; i < maxListSize; i++) {
                FileMetadataPhenomobileDTO fileMetadata = new FileMetadataPhenomobileDTO();
                
                if (experiments.size() > i) {
                    fileMetadata.setGroupPlotUri(experiments.get(i).getUri());
                    fileMetadata.setGroupPlotAlias(experiments.get(i).getAlias());
                    fileMetadata.setGroupPlotSpecies(experiments.get(i).getCropSpecies());
                    fileMetadata.setGroupPlotType(TRIPLESTORE_CONCEPT_EXPERIMENT);
                }

                if (users.size() > i) {
                    fileMetadata.setPilot(users.get(i).getEmail());
                }
                
                fileMetadataList.add(fileMetadata);
            }
        } else if (uriDaoSesame.isSubClassOf(vectorRdfType, TRIPLESTORE_CONCEPT_UAV)) {
            //uav metadata
            for (int i = 0; i < maxListSize; i++) {
                FileMetadataUAVDTO fileMetadata = new FileMetadataUAVDTO();
                
                if (installations.size() > i) {
                    fileMetadata.setInstallation(TRIPLESTORE_PLATFORM);
                }
                
                if (experiments.size() > i) {
                    fileMetadata.setGroupPlotUri(experiments.get(i).getUri());
                    fileMetadata.setGroupPlotAlias(experiments.get(i).getAlias());
                    fileMetadata.setGroupPlotSpecies(experiments.get(i).getCropSpecies());
                    fileMetadata.setGroupPlotType(TRIPLESTORE_CONCEPT_EXPERIMENT);
                }

                if (users.size() > i) {
                    fileMetadata.setPilot(users.get(i).getEmail());
                }

                if (sensors.size() > i) {
                    fileMetadata.setCameraUri(sensors.get(i).getUri());
                    fileMetadata.setCameraAlias(sensors.get(i).getLabel());
                    fileMetadata.setCameraType(sensors.get(i).getRdfType());
                }
                
                if (vectors.size() > i) {
                    fileMetadata.setVectorUri(vectors.get(i).getUri());
                    fileMetadata.setVectorAlias(sensors.get(i).getLabel());
                    fileMetadata.setVectorType(sensors.get(i).getRdfType());
                }
                
                if (radiometricTargets.size() > i) {
                    fileMetadata.setRadiometricTargetUri(radiometricTargets.get(i).getUri());
                    fileMetadata.setRadiometricTargetAlias(radiometricTargets.get(i).getLabel());
                }
                
                fileMetadataList.add(fileMetadata);
            }
        }
        
        return fileMetadataList;
    }
    
    /**
     * Get all the required informations to generate the hidden phis part of the excel file for 4P.
     * The content of the metadata depends of the acquisition session vector type.
     * @return the content of the metadata required. 
     *         null if no metadata for the given vector type
     */
    public ArrayList<FileMetadataDTO> allPaginateFileMetadata() {
        //Check if the rdf type is a subclass of vector
        UriDaoSesame uriDAOSesame = new UriDaoSesame();
        if (uriDAOSesame.isSubClassOf(vectorRdfType, TRIPLESTORE_CONCEPT_VECTOR)) {
            return getFileMetadata();
        } else {
            return null;
        }
    }
}
