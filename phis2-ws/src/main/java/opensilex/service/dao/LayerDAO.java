//******************************************************************************
//                                LayerDAO.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: Aug. 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import opensilex.service.dao.exception.DAODataErrorAggregateException;
import opensilex.service.dao.exception.DAOPersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import opensilex.service.PropertiesFileManager;
import opensilex.service.dao.manager.DAO;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.ontology.Rdf;
import opensilex.service.ontology.Oeso;
import opensilex.service.ontology.Rdfs;
import opensilex.service.resource.dto.LayerDTO;
import opensilex.service.utils.POSTResultsReturn;
import opensilex.service.view.brapi.Status;
import opensilex.service.model.ScientificObject;
import opensilex.service.model.Property;

/**
 * Layer DAO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class LayerDAO extends DAO<LayerDTO>{

    final static Logger LOGGER = LoggerFactory.getLogger(LayerDAO.class);
    
    public String objectURI;
    public String objectType;
    public String depth;
    public String filePath;
    public String fileWebPath;
    public HashMap<String, ScientificObject> children = new HashMap<>();
    
    private static final String LAYER_FILE_SERVER_DIRECTORY = PropertiesFileManager.getConfigFileProperty("service", "layerFileServerDirectory");
    private static final String LAYER_FILE_SERVER_ADDRESS = PropertiesFileManager.getConfigFileProperty("service", "layerFileServerAddress");
     
    /**
     * Searches and updates children.
     * @throws java.sql.SQLException
     * @param layerDTO 
     */
      public void searchAndUpdateChildren(LayerDTO layerDTO) throws SQLException {
        ScientificObjectRdf4jDAO agronomicalObjectDao = new ScientificObjectRdf4jDAO();
        ScientificObjectSQLDAO agronomicalObject = new ScientificObjectSQLDAO();
        
        HashMap<String, ScientificObject> scientificObjectChildren = agronomicalObjectDao.searchChildren(layerDTO);
        
        ArrayList<String> childrenURIs = new ArrayList<>(scientificObjectChildren.keySet());
        HashMap<String,String> childrenAgronomicalObjectsGeometries = agronomicalObject.getGeometries(childrenURIs);
        
        scientificObjectChildren.entrySet().forEach((child) -> {
            ScientificObject ao = child.getValue();
            ao.setGeometry(childrenAgronomicalObjectsGeometries.get(child.getKey()));
            children.put(child.getKey(), ao);
        });
    }
    
    /**
     * @param objectURI
     * @return the link to the GeoSON corresponding to the layer.
     */
    public String getObjectURILayerFilePath(String objectURI) {
        String[] splitUri = objectURI.split("/");
        String layerName = splitUri[splitUri.length-1];
        String filename = layerName + ".geojson";
        return LAYER_FILE_SERVER_DIRECTORY + "/" + filename;
    }
    
    public String getObjectURILayerFileWebPath(String objectURI) {
        String[] splitUri = objectURI.split("/");
        String layerName = splitUri[splitUri.length-1];
        String filename = layerName + ".geojson";
        return LAYER_FILE_SERVER_ADDRESS + "/" + filename;
    }
      
    /**
     * Gets types by URI, relation or concept.
     * @return a hash map with the properties and their type.
     */
    private HashMap<String, String> getTypesByURIRelationOrConcept() {
        HashMap<String, String> typesByRelationOrConcept = new HashMap<>();
        typesByRelationOrConcept.put(Oeso.CONCEPT_VARIETY.toString(), "variety");
        typesByRelationOrConcept.put(Oeso.CONCEPT_GENOTYPE.toString(), "genotype");
        typesByRelationOrConcept.put(Oeso.CONCEPT_SPECIES.toString(), "species");
        typesByRelationOrConcept.put(Oeso.RELATION_FROM_GENOTYPE.toString(), "genotype");
        typesByRelationOrConcept.put(Oeso.RELATION_HAS_VARIETY.toString(), "variety");
        typesByRelationOrConcept.put(Oeso.RELATION_HAS_SPECIES.toString(), "species");
        typesByRelationOrConcept.put(Oeso.RELATION_HAS_EXPERIMENT_MODALITIES.toString(), "experimentModalities");
        typesByRelationOrConcept.put(Oeso.RELATION_HAS_REPLICATION.toString(), "replication");
        typesByRelationOrConcept.put(Rdfs.RELATION_LABEL.toString(), "label");
        typesByRelationOrConcept.put(Rdf.RELATION_TYPE.toString(), "typeElement");
        
        return typesByRelationOrConcept;
    }
    /**
     * Generates the GeoSon file corresponding to the layer.
     * @param layerDTO
     * @return 
     * @throws java.io.IOException 
     */
    public POSTResultsReturn createLayerFile(LayerDTO layerDTO) throws IOException {
        POSTResultsReturn createLayerFile;
        List<Status> createStatusList = new ArrayList<>();
        List<String> createdResourcesFilesPaths = new ArrayList<>();
        boolean createLayerFileOk = true;
        try {
            //1. Get the descendant to put in the layer
            searchAndUpdateChildren(layerDTO);
            
            //2. Create file
            String[] splitUri = layerDTO.getObjectUri().split("/");
            String layerName = splitUri[splitUri.length-1];
            String filename = layerName + ".geojson";
            filePath = LAYER_FILE_SERVER_DIRECTORY + "/" + filename;
            
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath))) {
                //1. Write parent and file start
                //SILEX:todo
                //code to improve
                writer.write("{");
                writer.write("\"type\" : \"FeatureCollection\",");
                writer.write("\"features\" : [");
                int nbChild = 0;
                for (Entry<String, ScientificObject> child : children.entrySet()) {
                    // Write each child
                    writer.write("{\"type\" : \"Feature\",");

                    writer.write("\"geometry\" : " + child.getValue().getGeometry() + ",");
                    writer.write("\"properties\" : {");
                    writer.write("\"uri\" : \"" + child.getValue().getUri()+ "\",");
                    
                    //SILEX:conception
                    // A more generic way could be done (in URINamespaces, a HashMap with corespondancies URI type/relation --> property type ?)
                    // Add properties corresponding to the AO (vartiety, repetition, ...)
                    HashMap<String, String> typesByRelationOrConcept = getTypesByURIRelationOrConcept();
                    int nbProperties = 0;
                    for (Property property : child.getValue().getProperties()) {
                        // We deduct the name of the property from the type or relation of the concept
                         if (property.getRdfType() != null) {
                            writer.write("\"" + typesByRelationOrConcept.get(property.getRdfType()) + "\" : \"" + property.getValue() + "\"");
                        } else {
                            writer.write("\"" + typesByRelationOrConcept.get(property.getRelation()) + "\" : \"" + property.getValue() + "\"");
                        }
                        nbProperties ++;
                        if (nbProperties < child.getValue().getProperties().size()) {
                            writer.write(",");
                        }
                    }
                    //\SILEX:conception
                    
                    writer.write("}}");
                    nbChild++;
                    if (nbChild < children.size()) {
                        writer.write(",");
                    }
                }
                
                writer.write("]}");
                
                //SILEX:warning ///!\ To uncomment in PROD
//                java.nio.file.Path path = Paths.get(filePath);
//                UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
//                UserPrincipal up = lookupService.lookupPrincipalByName("www-data");
//                Files.setOwner(path, up); 
                //\SILEX:warning
                
                File f = new File(filePath);
                final Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-rw-r--");
                Files.setPosixFilePermissions(f.toPath(), perms);
                
                fileWebPath = LAYER_FILE_SERVER_ADDRESS + "/" + filename;
                
                createdResourcesFilesPaths.add(filePath);
                createStatusList.add(new Status("Resources created", StatusCodeMsg.INFO, createdResourcesFilesPaths.size() + " new resources created"));
                //\SILEX:test
                // The file is normally closed automatically
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(LayerDAO.class.getName()).log(Level.SEVERE, null, ex);
                createLayerFileOk = false;
                createStatusList.add(new Status("Error while create layer file", StatusCodeMsg.ERR, new StringBuilder().append(StatusCodeMsg.ERR).toString()));
            }
            
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(LayerDAO.class.getName()).log(Level.SEVERE, null, ex);
            createLayerFileOk = false;
            createStatusList.add(new Status("Error while create layer file", StatusCodeMsg.ERR, new StringBuilder().append(StatusCodeMsg.ERR).toString()));
        }
        
        createLayerFile = new POSTResultsReturn(createLayerFileOk, createLayerFileOk, createLayerFileOk);
        createLayerFile.statusList = createStatusList;
        return createLayerFile;
    }

    @Override
    public List<LayerDTO> create(List<LayerDTO> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(List<LayerDTO> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<LayerDTO> update(List<LayerDTO> objects) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public LayerDTO find(LayerDTO object) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public LayerDTO findById(String id) throws DAOPersistenceException, Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void validate(List<LayerDTO> objects) throws DAOPersistenceException, DAODataErrorAggregateException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void initConnection() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void closeConnection() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void startTransaction() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void commitTransaction() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void rollbackTransaction() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
