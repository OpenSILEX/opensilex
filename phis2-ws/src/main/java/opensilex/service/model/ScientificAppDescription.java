//******************************************************************************
//                                ScientificAppDescription.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 9 sept. 2019
// Contact: Expression userEmail is undefined on line 6, column 15 in file:///home/charlero/GIT/GITHUB/phis-ws/phis2-ws/licenseheader.txt., anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import com.google.gson.Gson;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.DatatypeConverter;
import opensilex.service.PropertiesFileManager;

/**
 *
 * @author charlero
 */
public class ScientificAppDescription {

    private String id;
    private String documentUri;
    private Boolean extractDockerFilesState;
    public String display_name;
    public String description;
    public ArrayList<String> container_cmd;
    public String container_image;
    public HashMap<String, Object> env_variables;
    public String application_url;

    public ScientificAppDescription(String uri, String display_name, String description, String sessionId) {
        this.documentUri = uri;
        setId(uri);
        this.display_name = display_name;
        this.description = description;
        this.container_image = "opensilex/shinyproxy-" + this.id;
        this.env_variables = new HashMap<>();
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
        this.application_url = "http://" + shinyHost + ":" + shinyPort + "/app/" + this.id
                + "?wsUrl=http://" + webServiceHost + webServiceBasePath + "/" + token;
        

        
    }

    public Map<String, Object> convertToYamlFormatMap() {
        Map<String, Object> shinyAppDescriptionMap = new HashMap<>();
        shinyAppDescriptionMap.put("id", this.id);
        shinyAppDescriptionMap.put("display_name", this.display_name);
        shinyAppDescriptionMap.put("description", this.description);
        if (this.container_cmd != null) {
            shinyAppDescriptionMap.put("container_cmd", this.container_cmd);
        }
        shinyAppDescriptionMap.put("container_image", this.container_image);
        if (!this.env_variables.isEmpty()) {
            shinyAppDescriptionMap.put("container-env", this.env_variables);
        }
        return shinyAppDescriptionMap;
    }

    public void addEnVariable(String key, Object value) {
        this.env_variables.put(key, value);
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
