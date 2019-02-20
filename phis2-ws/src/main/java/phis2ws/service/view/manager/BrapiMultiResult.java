//******************************************************************************
//                                       BrapiMultiResult.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 24 sept. 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.manager;

import java.util.ArrayList;
import phis2ws.service.view.brapi.BrapiPagination;

/**
 * This class provides the format of Result when there is a list of several elements
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class BrapiMultiResult<T>{
    ArrayList<T> data;

    public BrapiMultiResult() {
        data = new ArrayList<>();
    }

    /**
     * Constructor with the pagination
     * @param pagination pagination to sort the list
     * @param paginate
     * @see copyList()
     * @example 
     * "result": {
        "data": [
          {
            "defaultValue": null,
            "description": "",
            "name": "Leaf_Area_Index",
            "observationVariables": [
              "http://www.phenome-fppn.fr/platform/id/variables/v001"
            ],
            "traitDbId": "http://www.phenome-fppn.fr/platform/id/traits/t001",
            "traitId": null
          },
          {
            "defaultValue": null,
            "description": "",
            "name": "NDVI",
            "observationVariables": [
              "http://www.phenome-fppn.fr/platform/id/variables/v002"
            ],
            "traitDbId": "http://www.phenome-fppn.fr/platform/id/traits/t002",
            "traitId": null
          }
         ]
        }
     */
    public BrapiMultiResult(ArrayList<T> list, BrapiPagination pagination, boolean paginate) {
        if(paginate){
            this.data = list;
        }else{
            this.data = copyList(list, pagination);
        }       
    }

    /**
     * Sort a list using the wanted page and the number of results per page
     * @param list unsorted elements
     * @param pagination the information of the pagination. This is used to sort the list.
     * @return the elements with the pagination
     */
    final ArrayList<T> copyList(ArrayList<T> list, BrapiPagination pagination) {
        ArrayList<T> finalList = new ArrayList();
        if (pagination.getCurrentPage() > pagination.getTotalPages()) {
            return finalList;
        }
        int i = (pagination.getCurrentPage()) * pagination.getPageSize();
        int tmp = i;
        while (i < list.size() && (i - tmp) < pagination.getPageSize()) {
            finalList.add(list.get(i));
            i++;
        }
        return finalList;
    }

     public ArrayList<T> getData() {
        return data;
    }

    public void setData(ArrayList<T> data) {
        this.data = data;
    }
    
}
