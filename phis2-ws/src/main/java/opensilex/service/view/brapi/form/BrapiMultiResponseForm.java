//******************************************************************************
//                          BrapiMultiResponseForm.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 25 Sept. 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.view.brapi.form;

import java.util.ArrayList;
import opensilex.service.view.brapi.BrapiMetadata;
import opensilex.service.result.BrapiMultiResult;

/**
 * Allows the formatting of the result of the request about any object.
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class BrapiMultiResponseForm<T> {
    protected BrapiMetadata metadata;
    protected BrapiMultiResult result;
    
    /**
     * Initializes metadata and result fields when there are several elements.
     * @param pageSize the number of results per page
     * @param currentPage the requested page
     * @param data list of results
     * @param paginate true : already paginate
     *                 false : needs pagination
     * @example 
     * {
     *  "metadata": {
     *    "pagination": {
     *      "pageSize": 20,
     *      "currentPage": 0,
     *      "totalCount": 3,
     *      "totalPages": 1
     *    },
     *    "status": [],
     *    "datafiles": []
     *  },
     *  "result": {
     *    "data": [
     *      {
     *        "defaultValue": null,
     *        "description": "",
     *        "name": "Leaf_Area_Index",
     *        "observationVariables": [
     *          "http://www.phenome-fppn.fr/platform/id/variables/v001"
     *        ],
     *        "traitDbId": "http://www.phenome-fppn.fr/platform/id/traits/t001",
     *        "traitId": null
     *      },
     *      {
     *        "defaultValue": null,
     *        "description": "",
     *        "name": "NDVI",
     *        "observationVariables": [
     *          "http://www.phenome-fppn.fr/platform/id/variables/v002"
     *        ],
     *        "traitDbId": "http://www.phenome-fppn.fr/platform/id/traits/t002",
     *        "traitId": null
     *      },
     *      {
     *        "defaultValue": null,
     *        "description": null,
     *        "name": "myTrait",
     *        "observationVariables": null,
     *        "traitDbId": "http://www.phenome-fppn.fr/platform/id/traits/t003",
     *        "traitId": null
     *      }
     *    ]
     *  }
     *}
     */
    public BrapiMultiResponseForm(int pageSize, int currentPage, ArrayList<T> data, boolean paginate) {
        metadata = new BrapiMetadata(pageSize, currentPage, data.size());
        result = new BrapiMultiResult(data,  metadata.getPagination(), paginate);
    }  

    public BrapiMetadata getMetadata() {
        return metadata;
    }

    public BrapiMultiResult getResult() {
        return result;
    }        
}
