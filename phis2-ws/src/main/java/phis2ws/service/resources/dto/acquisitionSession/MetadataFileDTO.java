//******************************************************************************
//                                       FileMetadataDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 30 August, 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.acquisitionSession;

import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;

/**
 * Represents the JSON return by the web service used to generate the acquisition 
 * session excel file for the 4P platform. There is an excel format for each vector type. 
 * The FileMetadataDTO class has the basic informations contained for the acquisition sessions of all types of vectors.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public abstract class MetadataFileDTO extends AbstractVerifiedClass {
    @Override
    public Object createObjectFromDTO() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
