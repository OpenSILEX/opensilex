/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.infrastructure.api;

import java.net.URI;
import java.util.List;
import org.opensilex.core.infrastructure.dal.InfrastructureModel;

/**
 *
 * @author vince
 */
class InfrastructureCreationDTO {
    
    private String name;
    
    private URI parent;
    
    private List<URI> children;
    
    private List<URI> devices;
    
    private List<URI> users;
    
    public InfrastructureModel newModel() {
        InfrastructureModel model = new InfrastructureModel();
        
        return model;
    }
}
