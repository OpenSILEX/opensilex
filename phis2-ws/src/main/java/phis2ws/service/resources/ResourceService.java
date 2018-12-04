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
 * 
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResourceService {
    //user session
    @SessionInject
    protected Session userSession;
    
    //The max value of the pageSize coming from the service property pageSizeMax.
    final static int MAX_PAGE_SIZE = 150000;
    
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
}
