//******************************************************************************
//                           POSTResultsReturn.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: June 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.utils;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;

/**
 * POST return result.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class POSTResultsReturn {
    
    public final static String NEW_RESOURCES_CREATED_MESSAGE = "%d new resource(s) created";
    
    private Boolean resultState;
    private Boolean insertState;
    private Boolean dataState;
    
    /**
     * Status to return.
     */
    private Response.Status httpStatus;
    
    /**
     * Custom supplementary status.
     */
    public List statusList = new ArrayList<>();
    public Boolean AlreadyExist; 
    public String errorMsg;
    public List<String> createdResources = new ArrayList<>();
  
    public POSTResultsReturn() {
    }

    public POSTResultsReturn(Boolean resultState) {
        this.resultState = resultState;
    }

    public POSTResultsReturn(Boolean resultState, Response.Status httpStatus, String errorMsg) {
        this.resultState = resultState;
        this.httpStatus = httpStatus;
        this.errorMsg = errorMsg;
    }
    
    public POSTResultsReturn(Boolean resultState, Boolean insertState, Boolean dataState) {
        if(resultState){
            if(dataState){
                if(insertState == null){ // Inserted
                    httpStatus = Response.Status.ACCEPTED;    
                }else if(insertState){ // Inserted
                    httpStatus = Response.Status.CREATED;    
                }else{ // Failed to insert
                    httpStatus = Response.Status.INTERNAL_SERVER_ERROR;
                }
            }else{ // Wrong format or missing data
                  httpStatus = Response.Status.BAD_REQUEST;
            }
        }else{
            if(!dataState && ( insertState == null || insertState ) ){
                httpStatus = Response.Status.BAD_REQUEST;
            }else{
                httpStatus = Response.Status.INTERNAL_SERVER_ERROR;
            }
        }
        this.resultState = resultState;
        this.insertState = insertState;
        this.dataState = dataState;
    }
    
    
    public POSTResultsReturn(Boolean resultState, Boolean insertState, Boolean dataState, Response.Status httpStatus, List statusList, Boolean AlreadyExist, String errorMsg) {
        this.resultState = resultState;
        this.insertState = insertState;
        this.dataState = dataState;
        this.httpStatus = httpStatus;
        this.statusList = statusList;
        this.AlreadyExist = AlreadyExist;
        this.errorMsg = errorMsg;
    }

    public Boolean getResultState() {
        return resultState;
    }

    public void setResultState(Boolean resultState) {
        this.resultState = resultState;
    }

    public Boolean getInsertState() {
        return insertState;
    }

    public void setInsertState(Boolean insertState) {
        this.insertState = insertState;
    }

    public Boolean getDataState() {
        return dataState;
    }

    public void setDataState(Boolean dataState) {
        this.dataState = dataState;
    }

    public Response.Status getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Response.Status httpStatus) {
        this.httpStatus = httpStatus;
    }

    public List getStatusList() {
        return statusList;
    }

    public void setStatusList(List statusList) {
        this.statusList = statusList;
    }

    public Boolean getAlreadyExist() {
        return AlreadyExist;
    }

    public void setAlreadyExist(Boolean AlreadyExist) {
        this.AlreadyExist = AlreadyExist;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public List<String> getCreatedResources() {
        return createdResources;
    }

    public void setCreatedResources(List<String> createdResources) {
        this.createdResources = createdResources;
    }
    
    public void addCreatedResource(String createdResource) {
        this.createdResources.add(createdResource);
    }
}
