//******************************************************************************
//                                       BrapiMultiResult.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 24 sept. 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.manager;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;

/**
 * This class provides the format of Result when there is a list of several elements
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class BrapiMultiResult<T> {
    ArrayList<T> data;

    public BrapiMultiResult() {
        data = new ArrayList<>();
    }

    /**
     * Constructor with the pagination
     * @param pagination pagination to sort the list
     * @param paginate
     * @see copyList()
     */
    public BrapiMultiResult(ArrayList<T> list, Pagination pagination, boolean paginate) {
        if(paginate){
            this.data = list;
        }else{
            this.data = copyList(list, pagination);
        }       
    }

    /**
     * Sort a list using the wanted page and the number of results per page
     * @param list unsorted elements
     * @param pagination the informations of the paginations. This is used to sort the list.
     * @return the elements with the pagination
     */
    final ArrayList<T> copyList(ArrayList<T> list, Pagination pagination) {
        ArrayList<T> finalList = new ArrayList();
        if (pagination.getCurrentPage() > pagination.getTotalPages()) {
            return finalList;
        }
//        int i = (pagination.getCurrentPage() - 1) * pagination.getPageSize();
// Update : BreedingAPI page 0 instead of page 1 in metadata
        int i = (pagination.getCurrentPage()) * pagination.getPageSize();
        int tmp = i;
        while (i < list.size() && (i - tmp) < pagination.getPageSize()) {
            finalList.add(list.get(i));
            i++;
        }
        return finalList;
    };

    public int dataSize() {
        if (data != null) {
            return data.size();
        } else {
            return 0;
        }
    }

    public ArrayList<T> getData() {
        return data;
    }

    public void setData(ArrayList<T> data) {
        this.data = data;
    }
    
}
