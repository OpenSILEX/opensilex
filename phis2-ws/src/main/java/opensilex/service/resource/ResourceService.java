//******************************************************************************
//                                       ResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 3 Dec. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource;

import java.util.ArrayList;
import javax.ws.rs.core.Response;
import opensilex.service.PropertiesFileManager;
import opensilex.service.authentication.Session;
import opensilex.service.documentation.StatusCodeMsg;
import opensilex.service.injection.SessionInject;
import opensilex.service.view.brapi.Status;
import opensilex.service.result.ResultForm;

/**
 * Resource service mother class.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResourceService {
    
    // The default language of the application
    protected static final String DEFAULT_LANGUAGE = PropertiesFileManager.getConfigFileProperty("service", "defaultLanguage");
    
    // User session
    @SessionInject
    protected Session userSession;
    
    /**
     * Generic response for no result found.
     * @param getResponse
     * @param insertStatusList
     * @return the Response with the error message
     */
    protected Response noResultFound(ResultForm getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status(StatusCodeMsg.NO_RESULTS, StatusCodeMsg.INFO, "No result found."));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }

    /**
     * Generic method for SQL error message.
     * @param getResponse
     * @param insertStatusList
     * @return the Response with the error message
     */
    protected Response sqlError(ResultForm getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("SQL error", StatusCodeMsg.ERR, "can't fetch result"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(getResponse).build();
    }
}
