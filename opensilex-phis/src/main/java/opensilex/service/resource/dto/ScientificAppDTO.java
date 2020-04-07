//******************************************************************************
//                                ScientificAppDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 9 sept. 2019
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto;

import java.util.ArrayList;
import java.util.HashMap;
import opensilex.service.model.ScientificAppDescription;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.shinyProxy.ShinyProxyService;

/**
 * ScientificAppDTO
 Describe a Scientific application
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class ScientificAppDTO extends AbstractVerifiedClass{
    
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
    public String display_name;
    /**
     * Scientific application description
     */
    public String description;
    /**
     * Scientific application docker command line
     */
    public ArrayList<String> container_cmd;
    /**
     * Scientific application docker image
     */
    public String container_image;
    /**
     * Scientific application docker environmentVariable
     */
    public HashMap<String, Object> env_variables;
    public String application_url;
    
    /**
     * Docker internal network in order this appplication
     * will able to communicate with ShinyProxy
     */
    public static String container_network = ShinyProxyService.SHINYPROXY_NETWORK_ID; 

    public ScientificAppDTO(String uri, String display_name, String description, String sessionId) {
    }

    public ScientificAppDTO(ScientificAppDescription scientificAppDescription) {
        this.id = scientificAppDescription.getId();
        this.documentUri = scientificAppDescription.getDocumentUri();
        this.display_name = scientificAppDescription.displayName;
        this.description = scientificAppDescription.description;
                this.container_cmd = scientificAppDescription.containerCmd;
        this.container_image = scientificAppDescription.containerImageName;
        this.application_url = scientificAppDescription.applicationUrl;

    }

    

    @Override
    public Object createObjectFromDTO() throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
