//******************************************************************************
//                                       ResourceService.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 3 déc. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources;

import java.util.ArrayList;
import javax.ws.rs.core.Response;
import phis2ws.service.authentication.Session;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.manager.ResultForm;

/**
 * Class extended by all the resource services, which contains the common methods and attributes.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResourceService {
    //user session
    @SessionInject
    protected Session userSession;
    
    /**
     * Generic reponse for no result found
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
     * Generic method for sql error message
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
