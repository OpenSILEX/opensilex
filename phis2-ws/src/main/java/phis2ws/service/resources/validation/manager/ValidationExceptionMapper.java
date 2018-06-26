//******************************************************************************
//                                       ValidationExceptionMapper.java
//
// Author(s): Arnaud Charleroy
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 25 juin 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  25 juin 2018
// Subject: Class that catches validation error on resources services parameters
// Contains the validation error form
//******************************************************************************
package phis2ws.service.resources.validation.manager;

import java.util.ArrayList;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.resources.TokenResourceService;
import phis2ws.service.utils.JsonConverter;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.ResponseFormGET;

/**
 *
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
@Produces(MediaType.APPLICATION_JSON)
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<javax.validation.ValidationException> {

     final static Logger LOGGER = LoggerFactory.getLogger(ValidationExceptionMapper.class);
    
    @Override
    public Response toResponse(javax.validation.ValidationException e) {
        ArrayList<Status> statusList = new ArrayList<>();
        for (ConstraintViolation<?> constraintViolation : ((ConstraintViolationException) e).getConstraintViolations()) {
            statusList.add(new Status(StatusCodeMsg.BAD_DATA_FORMAT, StatusCodeMsg.ERR, constraintViolation.getPropertyPath() + " " + constraintViolation.getMessage()));
         LOGGER.debug(JsonConverter.ConvertToJson(constraintViolation));
        }
       
        ResponseFormGET validationResponse = new ResponseFormGET(statusList);
        return Response.status(Response.Status.BAD_REQUEST).entity(validationResponse).build();
    }
}
