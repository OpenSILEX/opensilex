//******************************************************************************
//                                RDAO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 27 août 2019
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.dao;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import opensilex.service.PropertiesFileManager;

/**
 * RDAO
 * Interact with an OpenCPUServer 
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class RDAO {
    static {
        OPENCPU_HOST = PropertiesFileManager.getConfigFileProperty("data_analysis_config", "opencpu.host");
    }
    /**
     * OpenCPU server address
     */
    private final static String OPENCPU_HOST;
    
    public RDAO() {
    }
    
    /**
     * Call a statistical function from R with OpenCPU and retrieve json data result 
     * @param packageName name of the R package
     * @param functionName name of the function
     * @param parameters list of the function parameters
     * @return json object
     */
    public Response opencpuRFunctionProxyCall(String packageName, String functionName, String parameters){
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(OPENCPU_HOST);
        WebTarget opencpuCallWebTarget = webTarget.path("/ocpu/library/" + packageName + "/R/" + functionName + "/json")
                .queryParam("auto_unbox", "TRUE");
        return  opencpuCallWebTarget.request(MediaType.APPLICATION_JSON).post(Entity.json(parameters));
    }
    
}
