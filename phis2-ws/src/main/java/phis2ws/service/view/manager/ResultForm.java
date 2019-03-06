//**********************************************************************************************
//                                       ResultForm.java 
//
// Author(s): Samuël Chérimont, Arnaud Charleroy 
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: december 2015
// Contact:arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  October, 2016
// Subject: A class which represente response form 
//***********************************************************************************************
package phis2ws.service.view.manager;

import java.util.ArrayList;
import java.util.List;
import phis2ws.service.view.brapi.Metadata;
import phis2ws.service.view.brapi.Status;

/**
 * ResultForm - Classe abstraite qui sert de base aux ResultForm plus
 * spécifiques
 *
 * @version1.0
 *
 * @author Samuël Chérimont
 * @param <T> classe de spécification de la classe héritée
 * @see Result
 * @date 03/12/2015
 */
public class ResultForm<T> {

    protected Metadata metadata;
    protected Result result;

    /**
     * Initialize fields metadata and result
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     */
    public ResultForm(int pageSize, int currentPage, ArrayList<T> list, boolean paginate) {
        metadata = new Metadata(pageSize, currentPage, list.size());
        if (list.size() > 1) {
            result = new Result<T>(list, metadata.getPagination(), paginate);
        } else {
            result = new Result<T>(list);
        }
    }
    
    /**
     * Initialize fields metadata and result
     * @param pageSize results per page
     * @param currentPage current page
     * @param list results list
     * @param paginate 
     * @param totalCount number of result
     */
    public ResultForm(int pageSize, int currentPage, ArrayList<T> list, boolean paginate, int totalCount) {
        metadata = new Metadata(pageSize, currentPage, totalCount);
        if (list.size() > 1) {
            result = new Result<T>(list, metadata.getPagination(), paginate);
        } else {
            result = new Result<T>(list);
        }
    }
    /**
     * resultSize() - Récupère le nombre d'éléments de la liste data qui est
 un champ de result
     *
     * @return le nombre d'éléments de la liste
     * @see Result
     * @date 04/12/2015
     */
    public int resultSize() {
        return result.dataSize();
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Result getResult() {
        return result;
    }
    
    public void setStatus(List<Status> status){
        metadata.setStatus(status);
    }
}
