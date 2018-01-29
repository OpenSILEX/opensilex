//**********************************************************************************************
//                                       ConceptsResourceService.java 
//
// Author(s): Eloan LAGIER
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: Decembre 11, 2017
// Contact: eloan.lagier@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  Decembre 11, 2017
// Subject: A class which contains methods to automatically check the attributes
//          of a class, from rules defined by user.
//          Contains the list of the elements which might be send by the Client
//          to save the database
//***********************************************************************************************
package phis2ws.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.view.model.phis.Instance;


public class InstanceDTO extends AbstractVerifiedClass{
    final static Logger LOGGER = LoggerFactory.getLogger(InstanceDTO.class);
    private String limit;
    private String page;
    private String deep;
    private String uri;
    private String rdfType;
    

    
    @Override
    public Map rules() {
        Map<String, Boolean> rules = new HashMap<>();
        rules.put(uri, Boolean.TRUE);
        rules.put(page, Boolean.TRUE);
        rules.put(deep, Boolean.TRUE);
        rules.put(limit, Boolean.TRUE);
        return rules;
    }

    @Override
    public Instance createObjectFromDTO() {
        Instance instance = new Instance();
        instance.setType(rdfType);
        instance.setUri(uri);
        return instance;
    }
    
    
   // @ApiModelProperty(example = "http://www.phenome-fppn.fr/vocabulary/2017#ScientificDocument")
    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }
    
   // @ApiModelProperty(example = "http://www.phenome-fppn.fr/phenovia/documents/document90fb96ace2894cdb9f4575173d8ed4c9")
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
    
} //
