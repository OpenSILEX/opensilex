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
import java.util.List;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.dao.manager.DAOSesame;
import phis2ws.service.dao.phis.ExperimentDao;
import phis2ws.service.dao.phis.UserDaoPhisBrapi;
import phis2ws.service.model.User;
import phis2ws.service.ontologies.Oeso;
import phis2ws.service.resources.dto.acquisitionSession.MetadataFileDTO;
import phis2ws.service.resources.dto.acquisitionSession.MetadataFilePhenomobileDTO;
import phis2ws.service.resources.dto.acquisitionSession.MetadataFileUAVDTO;
import phis2ws.service.utils.sparql.SPARQLQueryBuilder;
import phis2ws.service.view.model.phis.Experiment;
import phis2ws.service.view.model.phis.RadiometricTarget;
import phis2ws.service.view.model.phis.Sensor;
import phis2ws.service.view.model.phis.Vector;

/**
 * This class is a Data Access Object for the acquisition sessions.
 * It gets the metadata for the excel file used to define acquisition sessions.
 * @update [Arnaud Charleroy] 10 September, 2018 : minor fix on vector data gathering
 * @author Morgane Vidal <morgane.vidal@inra.fr>, Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class AcquisitionSessionDAOSesame extends DAOSesame<MetadataFileDTO> {
    
    final static Logger LOGGER = LoggerFactory.getLogger(AcquisitionSessionDAOSesame.class);
    
    //The rdf type of the vector concerned by the acquisition session. 
    //Used in the GET fileMetadata to generate the informations for the file of definition the acquisition session for 4P.
    //e.g. http://www.opensilex.org/vocabulary/oeso#UAV
    public String vectorRdfType;
    
    /**
     * Count the number of rows for the metadata file
     * @return The number of rows
     */
    public Integer countFileMetadataRows() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
        UriDaoSesame uriDaoSesame = new UriDaoSesame();
        //size of the higher list to generate all the FileMetadataDTO
        ArrayList<Integer> sizes = new ArrayList<>();
        
        //if the vector is an uav or a field robot, it has specific file metadata
        if (uriDaoSesame.isSubClassOf(vectorRdfType, Oeso.CONCEPT_FIELD_ROBOT.toString())
                || uriDaoSesame.isSubClassOf(vectorRdfType, Oeso.CONCEPT_UAV.toString())) {
            //Common metadata
            //1. get the number of group plots (just the experiments in this version)
            ExperimentDao experimentDAO = new ExperimentDao();
            sizes.add(experimentDAO.count());
            
            //2. get the number of pilots
            UserDaoPhisBrapi userDAO = new UserDaoPhisBrapi();
            sizes.add(userDAO.count());
            
            //3. platform
            sizes.add(1);
            
            //uav
            if (uriDaoSesame.isSubClassOf(vectorRdfType, Oeso.CONCEPT_UAV.toString())) {
                //3. get the number of cameras
                SensorDAOSesame sensorDAO = new SensorDAOSesame();
                sizes.add(sensorDAO.countCameras());

                //4. get the number of vectors
                VectorDAOSesame vectorDAO = new VectorDAOSesame();
                vectorDAO.rdfType = Oeso.CONCEPT_UAV.toString();
                sizes.add(vectorDAO.countUAVs());

                //5. get the number of radiometric targets
                RadiometricTargetDAOSesame radiometricTargetDAO = new RadiometricTargetDAOSesame();
                sizes.add(radiometricTargetDAO.count());
            }
        }
        
        return Collections.max(sizes);
    }
    
    /**
     * Get all the required informations to generate the hidden phis part of the excel file for 4P.
     * The content of the metadata depends of the acquisition session vector type.
     * @see AcquisitionSessionDAOSesame#allPaginateFileMetadata() 
     * @return the content of the metadata required.
     */
    private ArrayList<MetadataFileDTO> getFileMetadata() {
        ArrayList<MetadataFileDTO> fileMetadataList = new ArrayList<>();
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
        if (uriDaoSesame.isSubClassOf(vectorRdfType, Oeso.CONCEPT_FIELD_ROBOT.toString())
                || uriDaoSesame.isSubClassOf(vectorRdfType, Oeso.CONCEPT_UAV.toString())) {
            //Common metadata
            //1. get the group plot list with the alias, uri and species (just the experiments in this version)
            ExperimentDao experimentDAO = new ExperimentDao();
            experimentDAO.setPage(page);
            experimentDAO.setPageSize(pageSize);
            experiments = experimentDAO.getAllExperimentsForAcquisitionSessionFile();
            sizes.add(experiments.size());
            
            //2. get the pilots list
            UserDaoPhisBrapi userDAO = new UserDaoPhisBrapi();
            userDAO.setPage(page);
            userDAO.setPageSize(pageSize);
            users = userDAO.getAllUsersEmails();
            sizes.add(users.size());
            
            //3. platform
            installations.add(Oeso.PLATFORM_URI.toString());
            
            //Metadata for the uav
            if (uriDaoSesame.isSubClassOf(vectorRdfType, Oeso.CONCEPT_UAV.toString())) {
                //3. get the camera list
                SensorDAOSesame sensorDAO = new SensorDAOSesame();
                sensorDAO.setPage(page);
                sensorDAO.setPageSize(pageSize);
                sensors = sensorDAO.getCameras();
                sizes.add(sensors.size());

                //4. get the vectors list
                VectorDAOSesame vectorDAO = new VectorDAOSesame();
                vectorDAO.setPage(page);
                vectorDAO.setPageSize(pageSize);
                vectors = vectorDAO.getUAVs();
                sizes.add(vectors.size());

                //5. get the radiometric targets
                RadiometricTargetDAOSesame radiometricTargetDAO = new RadiometricTargetDAOSesame();
                radiometricTargetDAO.setPage(page);
                radiometricTargetDAO.setPageSize(pageSize);
                radiometricTargets = radiometricTargetDAO.allPaginate();
                sizes.add(radiometricTargets.size());
            }
        }
        
        //generates the file metadata list
        int maxListSize = Collections.max(sizes);
        //field robot metadata
        if (uriDaoSesame.isSubClassOf(vectorRdfType, Oeso.CONCEPT_FIELD_ROBOT.toString())) {
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
        } else if (uriDaoSesame.isSubClassOf(vectorRdfType, Oeso.CONCEPT_UAV.toString())) {
            //uav metadata
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
     * Get all the required informations to generate the hidden phis part of the excel file for 4P.
     * The content of the metadata depends of the acquisition session vector type.
     * @return the content of the metadata required. 
     *         null if no metadata for the given vector type
     */
    public ArrayList<MetadataFileDTO> allPaginateFileMetadata() {
        //Check if the rdf type is a subclass of vector
        UriDaoSesame uriDAOSesame = new UriDaoSesame();
        if (uriDAOSesame.isSubClassOf(vectorRdfType, Oeso.CONCEPT_VECTOR.toString())) {
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
}
