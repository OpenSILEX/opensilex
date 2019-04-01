//**********************************************************************************************
//                                       POSTResultsReturn.java 
//
// Author(s): Arnaud Charleroy 
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: June 2016
// Contact:arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  October, 2016
// Subject: A class which collect results, error, and http status from an insertion
//***********************************************************************************************
package phis2ws.service.utils;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;

/**
 * Représentes les résults d'un envoi en POST
 * @author Arnaud Charleroy
 */
public class POSTResultsReturn {
    private Boolean resultState; // Le résultat de insert et data (bon si les 2 snt ok et faux sinon)
    private Boolean insertState; // Si l'insertion a été réussie
    private Boolean dataState; //état des données (si elles sont correctes)
    private Response.Status httpStatus; //status qui sera renvoyé au client avec l'insertion
    public List statusList = new ArrayList<>(); //Si je veux rajouter d'autres status
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
