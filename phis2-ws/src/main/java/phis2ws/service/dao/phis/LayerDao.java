//**********************************************************************************************
//                                       LayerDao.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: August 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  September, 6 2017
// Subject: A DAO specific to product layers files
//***********************************************************************************************
package phis2ws.service.dao.phis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.dao.sesame.ScientificObjectDAOSesame;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.ontologies.Rdf;
import phis2ws.service.ontologies.Vocabulary;
import phis2ws.service.resources.dto.LayerDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.model.phis.ScientificObject;
import phis2ws.service.view.model.phis.Property;


public class LayerDao {
    
    /**
     * @param objectURI URI de l'objet auquel la couche correspond
     * @param objectType type de l'objet (ex : http://www.phenome-fppn.fr/Vocabulary/2017/Experiment)
     * @param depth true si la couche a tous les descendants de objectURI
     *              false si on elle n'a que les enfants directs
     * @param filePath le chemin du fichier geoJSON correspondant à la couche
     * @param children la liste des descendants de objectURI. Clé : URI, 
     *                 valeur : HashMap avec (clé : le type de l'attribut ex. type, valeur : sa valeur ex. http://...../Experiment)
     */
    
    final static Logger LOGGER = LoggerFactory.getLogger(LayerDao.class);
    
    public String objectURI;
    public String objectType;
    public String depth;
    public String filePath;
    public String fileWebPath;
    public HashMap<String, ScientificObject> children = new HashMap<>();
    
    private static final String LAYER_FILE_SERVER_DIRECTORY = PropertiesFileManager.getConfigFileProperty("service", "layerFileServerDirectory");
    private static final String LAYER_FILE_SERVER_ADDRESS = PropertiesFileManager.getConfigFileProperty("service", "layerFileServerAddress");
     
    /**
     * @throws java.sql.SQLException
     * @action récupère la liste des enfants de objectURI et met à jour 
     *         l'attribut children en conséquence
     * @param layerDTO 
     */
      public void searchAndUpdateChildren(LayerDTO layerDTO) throws SQLException {
        ScientificObjectDAOSesame agronomicalObjectDaoSesame = new ScientificObjectDAOSesame();
        ScientificObjectDAO agronomicalObject = new ScientificObjectDAO();
        
        HashMap<String, ScientificObject> childrendAgronomicalObjectDaoSesame = agronomicalObjectDaoSesame.searchChildren(layerDTO);
        
        ArrayList<String> childrenURIs = new ArrayList<>(childrendAgronomicalObjectDaoSesame.keySet());
        HashMap<String,String> childrenAgronomicalObjectsGeometries = agronomicalObject.getGeometries(childrenURIs);
        
        for (Entry<String, ScientificObject> child : childrendAgronomicalObjectDaoSesame.entrySet()) {
            ScientificObject ao = child.getValue();
            ao.setGeometry(childrenAgronomicalObjectsGeometries.get(child.getKey()));
            children.put(child.getKey(), ao);
        }
    }
    
      /**
       * 
       * @param objectURI
       * @return le lien vers le fichier geojson correspondant à la couche
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
     * @action uilisé pour la façon de nommer les propriétés dans le geojson correspondant à la couche
     * @return une hashmap avec les noms des propriétés en fonction de l'uri de la relation ou du concept
     */
    private HashMap<String, String> getTypesByURIRelationOrConcept() {
        HashMap<String, String> typesByRelationOrConcept = new HashMap<>();
        typesByRelationOrConcept.put(Vocabulary.CONCEPT_VARIETY.toString(), "variety");
        typesByRelationOrConcept.put(Vocabulary.CONCEPT_GENOTYPE.toString(), "genotype");
        typesByRelationOrConcept.put(Vocabulary.CONCEPT_SPECIES.toString(), "species");
        typesByRelationOrConcept.put(Vocabulary.RELATION_FROM_GENOTYPE.toString(), "genotype");
        typesByRelationOrConcept.put(Vocabulary.RELATION_FROM_VARIETY.toString(), "variety");
        typesByRelationOrConcept.put(Vocabulary.RELATION_FROM_SPECIES.toString(), "species");
        typesByRelationOrConcept.put(Vocabulary.RELATION_HAS_EXPERIMENT_MODALITIES.toString(), "experimentModalities");
        typesByRelationOrConcept.put(Vocabulary.RELATION_HAS_REPETITION.toString(), "repetition");
        typesByRelationOrConcept.put(Vocabulary.RELATION_HAS_ALIAS.toString(), "alias");
        typesByRelationOrConcept.put(Rdf.RELATION_TYPE.toString(), "typeElement");
        
        return typesByRelationOrConcept;
    }
      /**
       * @action génère le fichier geojson correspondant à la couche
       * @param layerDTO
       * @return 
       */
    public POSTResultsReturn createLayerFile(LayerDTO layerDTO) throws IOException {
        POSTResultsReturn createLayerFile;
        List<Status> createStatusList = new ArrayList<>();
        List<String> createdResourcesFilesPaths = new ArrayList<>();
        boolean createLayerFileOk = true;
        try {
            //1. on récupère tous les descendants à mettre dans la couche
            searchAndUpdateChildren(layerDTO);
            
            //2. on crée le fichier
            String[] splitUri = layerDTO.getObjectUri().split("/");
            String layerName = splitUri[splitUri.length-1];
            String filename = layerName + ".geojson";
            filePath = LAYER_FILE_SERVER_DIRECTORY + "/" + filename;
            
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath))) {
                //1. On écrit le parent et le début du fichier
                //SILEX:test
                //code à revoir et à mieux écrire plus tard...
                writer.write("{");
                writer.write("\"type\" : \"FeatureCollection\",");
                writer.write("\"features\" : [");
                int nbChild = 0;
                for (Entry<String, ScientificObject> child : children.entrySet()) {
                    //On écrit chaque enfant
                    writer.write("{\"type\" : \"Feature\",");

                    writer.write("\"geometry\" : " + child.getValue().getGeometry() + ",");
                    writer.write("\"properties\" : {");
                    writer.write("\"uri\" : \"" + child.getValue().getUri()+ "\",");
                    
                    //SILEX:conception
                    //Il faudrait trouver une façon plus générique de le faire 
                    // (dans URINamespaces, une HashMap avec les correspondances uri type/relation --> type de la prop ?)
                    //Ajout des propriétés associées à l'AO (vartiety, repetition, ...)
                    HashMap<String, String> typesByRelationOrConcept = getTypesByURIRelationOrConcept();
                    int nbProperties = 0;
                    for (Property property : child.getValue().getProperties()) {
                        //On déduit le nom de la propriété du type de relation ou de concept
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
                
                java.nio.file.Path path = Paths.get(filePath);
                UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
                UserPrincipal up = lookupService.lookupPrincipalByName("www-data");
//                Files.setOwner(path, up); ///!\ À décommenter en prod

                File f = new File(filePath);
                final Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-rw-r--");
                Files.setPosixFilePermissions(f.toPath(), perms);
                
                fileWebPath = LAYER_FILE_SERVER_ADDRESS + "/" + filename;
                
                createdResourcesFilesPaths.add(filePath);
                createStatusList.add(new Status("Resources created", StatusCodeMsg.INFO, createdResourcesFilesPaths.size() + " new resources created"));
                //\SILEX:test
                //Le fichier est normalement automatiquement fermé
            } catch (IOException ex) {
                java.util.logging.Logger.getLogger(LayerDao.class.getName()).log(Level.SEVERE, null, ex);
                createLayerFileOk = false;
                createStatusList.add(new Status("Error while create layer file", StatusCodeMsg.ERR, new StringBuilder().append(StatusCodeMsg.ERR).toString()));
            }
            
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(LayerDao.class.getName()).log(Level.SEVERE, null, ex);
            createLayerFileOk = false;
            createStatusList.add(new Status("Error while create layer file", StatusCodeMsg.ERR, new StringBuilder().append(StatusCodeMsg.ERR).toString()));
        }
        
        createLayerFile = new POSTResultsReturn(createLayerFileOk, createLayerFileOk, createLayerFileOk);
        createLayerFile.statusList = createStatusList;
        return createLayerFile;
    }
}
