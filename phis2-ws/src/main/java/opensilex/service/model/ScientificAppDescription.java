//******************************************************************************
//                                ScientificAppDescription.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 9 sept. 2019
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import com.google.gson.Gson;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.DatatypeConverter;
import opensilex.service.PropertiesFileManager;
import opensilex.service.shinyProxy.ShinyProxyService;

/**
 * ScientificAppDescription
 * Describe a Scientific application
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class ScientificAppDescription {
    
    /**
     * Scientific application unique id in ShinyProxy
     */
    private String id;
    /**
     * Rdf documentUri
     */
    private String documentUri;
    /**
     * Scientific application displayed name
     */
    public String displayName;
    /**
     * Scientific application description
     */
    public String description;
    /**
     * Scientific application docker command line
     */
    public ArrayList<String> containerCmd;
    /**
     * Scientific application docker image
     */
    public String containerImageName;
    /**
     * Scientific application docker environmentVariable
     */
    public HashMap<String, Object> environmentVariables;
    
    /**
     * Scientific application url
     */
    public String applicationUrl;
    
    /**
     * If document as been successfully extracted to dicker apps dir
     */
    private Boolean extractDockerFilesState;
    
    /**
     * Docker internal network in order this appplication
     * will able to communicate with ShinyProxy
     */
    public static String containerNetwork = ShinyProxyService.SHINYPROXY_NETWORK_ID; 

    public ScientificAppDescription(String uri, String display_name, String description, String sessionId) {
        this.documentUri = uri;
        setId(uri);
        this.displayName = display_name;
        this.description = description;
        this.containerImageName = "opensilex/shinyproxy-" + this.id;
        this.environmentVariables = new HashMap<>();
        final String shinyHost = PropertiesFileManager.
                getConfigFileProperty("data_analysis_config", "shinyproxy.host");
        final String shinyPort = PropertiesFileManager.
                getConfigFileProperty("data_analysis_config", "shinyproxy.port");
        final String webServiceHost = PropertiesFileManager.
                getConfigFileProperty("service", "host");
        final String webServiceBasePath = PropertiesFileManager.
                getConfigFileProperty("service", "basePath");
        
        String token = "";
        if(sessionId != null){
            token = "&token=" + sessionId;
        }
        this.applicationUrl = "http://" + shinyHost + ":" + shinyPort + "/app/" + this.id
                + "?wsUrl=http://" + webServiceHost + webServiceBasePath + "/" + token;
    }

    /**
     * Convert objet into a map can be transform in shiny proxy yaml description file
     * @return map
     */
    public Map<String, Object> convertToYamlFormatMap() {
        Map<String, Object> shinyAppDescriptionMap = new HashMap<>();
        shinyAppDescriptionMap.put("id", this.id);
        shinyAppDescriptionMap.put("display_name", this.displayName);
        shinyAppDescriptionMap.put("description", this.description);
        if (this.containerCmd != null) {
            shinyAppDescriptionMap.put("container_cmd", this.containerCmd);
        }
        shinyAppDescriptionMap.put("container_image", this.containerImageName);
        if (!this.environmentVariables.isEmpty()) {
            shinyAppDescriptionMap.put("container-env", this.environmentVariables);
        }
        shinyAppDescriptionMap.put("container-network", ScientificAppDescription.containerNetwork);

        return shinyAppDescriptionMap;
    }

    public void addEnVariable(String key, Object value) {
        this.environmentVariables.put(key, value);
    }

    public String getDocumentUri() {
        return documentUri;
    }

    public String getId() {
        return id;
    }

    public Boolean getExtractDockerFilesState() {
        return extractDockerFilesState;
    }

    public void setExtractDockerFilesState(Boolean extractDockerFilesState) {
        this.extractDockerFilesState = extractDockerFilesState;
    }

    public final void setId(String documentUri) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(documentUri.getBytes());
            byte[] digest = md.digest();
            String idHash = DatatypeConverter
                    .printHexBinary(digest).toLowerCase();
            this.id = idHash;
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(ScientificAppDescription.class.getName()).log(Level.SEVERE, null, ex);

        }
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this); //To change body of generated methods, choose Tools | Templates.
    }

}
