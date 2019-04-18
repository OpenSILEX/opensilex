//******************************************************************************
//                                Result.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: 14 Dec. 2015
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.result;

import opensilex.service.view.brapi.Pagination;
import java.util.ArrayList;

/**
 * Abstract class which provides a model to format the web service returns to be
 * BrAPI compatible.
 * @see Pagination
 * @see https://brapi.docs.apiary.io
 * @param <T> The type of the objects of the data list attribute.
 * @author Samuël Chérimont, Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class Result<T> {

    ArrayList<T> data;

    public Result() {
        data = new ArrayList<>();
    }

    /**
     * Constructor used when there is only one result and does not need pagination.
     * @param list a list with one or no element
     * @date 14/12/2015
     */
    public Result(ArrayList<T> list) {
        this.data = new ArrayList();
        if(list.size() > 0){
            data.add(list.get(0));
        }
        
    }

    /**
     * Constructor with the pagination, for the list with more than one element.
     * @param list the elements
     * @param pagination pagination to sort the list
     * @param paginate
     * @see copyList()
     * @date 14/12/2015
     */
    public Result(ArrayList<T> list, Pagination pagination, boolean paginate) {
        if(paginate){
            this.data = list;
        }else{
            this.data = copyList(list, pagination);
        }
       
    }

    /**
     * Sorts a list using the wanted page and the number of results per page.
     * @param list unsorted elements
     * @param pagination the information of the pagination. This is used to sort the list.
     * @return the elements with the pagination
     */
     final ArrayList<T> copyList(ArrayList<T> list, Pagination pagination) {
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
    };

    /**
     * Gets the number of elements from the data field.
     * @return the number of elements of the data field. 
     *         0 if the list has not been initialized.
     */
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
