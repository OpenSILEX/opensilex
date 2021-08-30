//******************************************************************************
//                        DataGetDTOCluster.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2021
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opensilex.core.data.api;

import io.swagger.annotations.ApiModelProperty;
import org.opensilex.core.data.dal.DataModel;

/**
 *
 * @author alexandre
 */
public class DataGetDTOCluster extends DataGetDTO {

    @ApiModelProperty(value = "ID of the cluster that the Data is associated", example = "2", required = false)
    private int clusterID;
    
    /**
     *
     * @param clusterID
     */
    public void setClusterID(int clusterID) {
        this.clusterID = clusterID;
    }
    
   public int getClusterID() {
        return clusterID;
    }
    
    public static DataGetDTOCluster getDtoClusterFromModel(DataModel model){
        DataGetDTOCluster dto = new DataGetDTOCluster();
        dto.fromModel(model);
        return dto;
    }

}
