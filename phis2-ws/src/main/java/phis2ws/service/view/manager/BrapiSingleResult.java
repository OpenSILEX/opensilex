//******************************************************************************
//                                       BrapiSingleResult.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 24 sept. 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.manager;

import phis2ws.service.view.brapi.Pagination;

/**
 * This class provides the format of the result of each brapi service where there is only one element
 * @author Alice Boizet <alice.boizet@inra.fr>
 * @example
 * "result": {
    "data": {
      "defaultValue": null,
      "description": null,
      "name": "myTrait",
      "observationVariables": null,
      "traitDbId": "http://www.phenome-fppn.fr/platform/id/traits/t003",
      "traitId": null
    }
  }
 */
public class BrapiSingleResult<T> implements BrapiResult {
    //contains the returned object information
    T data;

    public BrapiSingleResult() {
    }
   
    public BrapiSingleResult(T data) {
        this.data = data;
    }
    
    public BrapiSingleResult(T data, Pagination pagination, boolean paginate) {
        this.data = data;
    }  
}
