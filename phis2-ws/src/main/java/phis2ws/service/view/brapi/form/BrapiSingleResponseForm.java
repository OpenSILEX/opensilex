//******************************************************************************
//                                       BrapiSingleResponseForm.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 22 janv. 2019
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.form;

import phis2ws.service.view.brapi.BrapiMetadata;

/**
 * Allows the formating of the result of the request about any object T
 * it is used when there is only one element in the response
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class BrapiSingleResponseForm<T> {
    protected BrapiMetadata metadata;
    protected T result;
    
    /**
     * Initialize metadata and result fields when there are only one element
     * @param data list of results
     * @example 
     * {
        "metadata": {
          "pagination": {
            "pageSize": 20,
            "currentPage": 0,
            "totalCount": 3,
            "totalPages": 1
          },
          "status": [],
          "datafiles": []
        },
        "result": {
            {
              "defaultValue": null,
              "description": "",
              "name": "Leaf_Area_Index",
              "observationVariables": [
                "http://www.phenome-fppn.fr/platform/id/variables/v001"
              ],
              "traitDbId": "http://www.phenome-fppn.fr/platform/id/traits/t001",
              "traitId": null
            }
      }
     */
    public BrapiSingleResponseForm(T data) {
        metadata = new BrapiMetadata(0,0,0);
        result = data;
    }
    
    public BrapiMetadata getMetadata() {
        return metadata;
    }

    public T getResult() {
        return result;
    }        
    
}
