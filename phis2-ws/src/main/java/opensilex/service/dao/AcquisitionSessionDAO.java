//******************************************************************************
//                           AcquisitionSessionDAO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 30 Aug. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.dao.manager.Rdf4jDAO;
import opensilex.service.model.User;
import opensilex.service.ontology.Oeso;
import opensilex.service.resource.dto.acquisitionSession.MetadataFileUAVDTO;
import opensilex.service.resource.dto.acquisitionSession.MetadataFilePhenomobileDTO;
import opensilex.service.resource.dto.acquisitionSession.MetadataFileDTO;
import opensilex.service.model.Experiment;
import opensilex.service.model.RadiometricTarget;
import opensilex.service.model.Sensor;
import opensilex.service.model.Vector;

/**
 * Acquisition session DAO.
 * @update [Arnaud Charleroy] 10 September 2018: minor fix on vector data gathering
 * @author Morgane Vidal <morgane.vidal@inra.fr>, Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class AcquisitionSessionDAO extends Rdf4jDAO<MetadataFileDTO> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(AcquisitionSessionDAO.class);
    
    /**
     * The RDF type of the vector concerned by the acquisition session. 
     * Used in the GET fileMetadata to generate the information for the file of 
     * definition the acquisition session for 4P.
     * @example http://www.opensilex.org/vocabulary/oeso#UAV
     */
    public String vectorRdfType;
    
    /**
     * Counts the number of rows of the metadata file.
     * @return The number of rows
     */
    public Integer countFileMetadataRows() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        UriDAO uriDao = new UriDAO();
        //size of the higher list to generate all the FileMetadataDTO
        ArrayList<Integer> sizes = new ArrayList<>();
        
        //if the vector is an uav or a field robot, it has specific file metadata
        if (uriDao.isSubClassOf(vectorRdfType, Oeso.CONCEPT_FIELD_ROBOT.toString())
                || uriDao.isSubClassOf(vectorRdfType, Oeso.CONCEPT_UAV.toString())) {
            //Common metadata
            //1. get the number of group plots (just the experiments in this version)
            ExperimentSQLDAO experimentDAO = new ExperimentSQLDAO();
            sizes.add(experimentDAO.count());
            
            //2. get the number of pilots
            UserDAO userDAO = new UserDAO();
            sizes.add(userDAO.count());
            
            //3. platform
            sizes.add(1);
            
            //UAV
            if (uriDao.isSubClassOf(vectorRdfType, Oeso.CONCEPT_UAV.toString())) {
                //3. get the number of cameras
                SensorDAO sensorDAO = new SensorDAO();
                sizes.add(sensorDAO.countCameras());

                //4. get the number of vectors
                VectorDAO vectorDAO = new VectorDAO();
                vectorDAO.rdfType = Oeso.CONCEPT_UAV.toString();
                sizes.add(vectorDAO.countUAVs());

                //5. get the number of radiometric targets
                RadiometricTargetDAO radiometricTargetDAO = new RadiometricTargetDAO();
                sizes.add(radiometricTargetDAO.count());
            }
        }
        
        return Collections.max(sizes);
    }
    
    /**
     * Gets all the required information to generate the hidden PHIS part of the excel file for 4P.
     * The content of the metadata depends of the acquisition session vector type.
     * @see AcquisitionSessionDAO#allPaginateFileMetadata() 
     * @return the content of the metadata required.
     */
    private ArrayList<MetadataFileDTO> getFileMetadata() {
        ArrayList<MetadataFileDTO> fileMetadataList = new ArrayList<>();
        UriDAO uriDao = new UriDAO();
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
        if (uriDao.isSubClassOf(vectorRdfType, Oeso.CONCEPT_FIELD_ROBOT.toString())
                || uriDao.isSubClassOf(vectorRdfType, Oeso.CONCEPT_UAV.toString())) {
            //Common metadata
            //1. get the group plot list with the alias, uri and species (just the experiments in this version)
            ExperimentSQLDAO experimentDAO = new ExperimentSQLDAO();
            experimentDAO.setPage(page);
            experimentDAO.setPageSize(pageSize);
            experiments = experimentDAO.getAllExperimentsForAcquisitionSessionFile();
            sizes.add(experiments.size());
            
            //2. get the pilots list
            UserDAO userDAO = new UserDAO();
            userDAO.setPage(page);
            userDAO.setPageSize(pageSize);
            users = userDAO.getAllUsersEmails();
            sizes.add(users.size());
            
            //3. platform
            installations.add(Oeso.PLATFORM_URI.toString());
            
            //Metadata for the uav
            if (uriDao.isSubClassOf(vectorRdfType, Oeso.CONCEPT_UAV.toString())) {
                //3. get the camera list
                SensorDAO sensorDAO = new SensorDAO();
                sensorDAO.setPage(page);
                sensorDAO.setPageSize(pageSize);
                sensors = sensorDAO.getCameras();
                sizes.add(sensors.size());

                //4. get the vectors list
                VectorDAO vectorDAO = new VectorDAO();
                vectorDAO.setPage(page);
                vectorDAO.setPageSize(pageSize);
                vectors = vectorDAO.getUAVs();
                sizes.add(vectors.size());

                // 5. get the radiometric targets
                RadiometricTargetDAO radiometricTargetDAO = new RadiometricTargetDAO();
                radiometricTargetDAO.setPage(page);
                radiometricTargetDAO.setPageSize(pageSize);
                radiometricTargets = radiometricTargetDAO.allPaginate();
                sizes.add(radiometricTargets.size());
            }
        }
        
        // generates the file metadata list
        int maxListSize = Collections.max(sizes);
        // field robot metadata
        if (uriDao.isSubClassOf(vectorRdfType, Oeso.CONCEPT_FIELD_ROBOT.toString())) {
            for (int i = 0; i < maxListSize; i++) {
                MetadataFilePhenomobileDTO fileMetadata = new MetadataFilePhenomobileDTO();
                
                if (experiments.size() > i) {
                    fileMetadata.setGroupPlotUri(experiments.get(i).getUri());
                    fileMetadata.setGroupPlotAlias(experiments.get(i).getAlias());
                    fileMetadata.setGroupPlotSpecies(experiments.get(i).getCropSpecies());
                    fileMetadata.setGroupPlotType(Oeso.CONCEPT_EXPERIMENT.toString());
                }

                if (users.size() > i) {
                    fileMetadata.setPilot(users.get(i).getEmail());
                }
                
                fileMetadataList.add(fileMetadata);
            }
        } else if (uriDao.isSubClassOf(vectorRdfType, Oeso.CONCEPT_UAV.toString())) {
            // UAV metadata
            for (int i = 0; i < maxListSize; i++) {
                MetadataFileUAVDTO fileMetadata = new MetadataFileUAVDTO();
                
                if (installations.size() > i) {
                    fileMetadata.setInstallation(Oeso.PLATFORM_URI.toString());
                }
                
                if (experiments.size() > i) {
                    fileMetadata.setGroupPlotUri(experiments.get(i).getUri());
                    fileMetadata.setGroupPlotAlias(experiments.get(i).getAlias());
                    fileMetadata.setGroupPlotSpecies(experiments.get(i).getCropSpecies());
                    fileMetadata.setGroupPlotType(Oeso.CONCEPT_EXPERIMENT.toString());
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
                    fileMetadata.setVectorAlias(vectors.get(i).getLabel());
                    fileMetadata.setVectorType(vectors.get(i).getRdfType());
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
     * Gets all the required information to generate the hidden PHIS part of the excel file for 4P.
     * The content of the metadata depends of the acquisition session vector type.
     * @return the content of the metadata required. 
     *         null if no metadata for the given vector type
     */
    public ArrayList<MetadataFileDTO> allPaginateFileMetadata() {
        // Check if the rdf type is a subclass of vector
        UriDAO uriDao = new UriDAO();
        if (uriDao.isSubClassOf(vectorRdfType, Oeso.CONCEPT_VECTOR.toString())) {
            return getFileMetadata();
        } else {
            return null;
        }
    }

    @Override
    public List<MetadataFileDTO> create(List<MetadataFileDTO> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<MetadataFileDTO> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<MetadataFileDTO> update(List<MetadataFileDTO> objects) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public MetadataFileDTO find(MetadataFileDTO object) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public MetadataFileDTO findById(String id) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<MetadataFileDTO> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
