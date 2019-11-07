//******************************************************************************
//                                RDAO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 27 août 2019
// Contact: Expression userEmail is undefined on line 6, column 15 in file:///home/charlero/GIT/GITHUB/phis-ws/phis2-ws/licenseheader.txt., anne.tireau@inra.fr, pascal.neveu@inra.fr
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
 *
 * @author charlero
 */
public class RDAO {
    static {
        OPENCPU_HOST = PropertiesFileManager.getConfigFileProperty("data_analysis_config", "opencpu.host");
    }
    private final static String OPENCPU_HOST;
    
    public RDAO() {
    }
    
    /**
     * 
     * @param packageName
     * @param functionName
     * @param parameters
     * @return 
     */
    public Response opencpuRFunctionProxyCall(String packageName, String functionName, String parameters){
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(OPENCPU_HOST);
        WebTarget opencpuCallWebTarget = webTarget.path("/ocpu/library/" + packageName + "/R/" + functionName + "/json")
                .queryParam("auto_unbox", "TRUE");
        return  opencpuCallWebTarget.request(MediaType.APPLICATION_JSON).post(Entity.json(parameters));
    }
    
}
