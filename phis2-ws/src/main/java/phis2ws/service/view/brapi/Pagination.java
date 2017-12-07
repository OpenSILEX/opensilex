//**********************************************************************************************
//                                       Pagination.java 
//
// Author(s): Samuël Chérimont, Arnaud CHARLEROY 
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: december 2015
// Contact:arnaud.charleroy@supagro.inra.fr, anne.tireau@supagro.inra.fr, pascal.neveu@supagro.inra.fr
// Last modification date:  October, 2016
// Subject: A class which represente pagination in response form 
//***********************************************************************************************
package phis2ws.service.view.brapi;

/**
 * Pagination - Cette classe donne des détails sur la pagination et ses champs
 * sont utilisés pour permettre une pagination correcte des résultats
 *
 * @version1.0
 *
 * @author Samuël Cherimont
 * @date 03/12/2015
 */

public class Pagination {

    private Integer pageSize, currentPage, totalCount, totalPages;

    public Pagination() {
    }

    
    
    /**
     * Pagination() - Constructeur de la classe Pagination: Attribue une valeur
     * à chaque champ d'un objet Pagination.
     *
     * @param pageSize nombre d'éléments par page
     * @param currentPage page actuelle
     * @param sizeList nombre d'éléments total
     *
     * @date 03/12/2015
     * @update 08/2016
     */
    public Pagination(Integer pageSize, Integer currentPage, Integer sizeList) {
        if (pageSize <= 0) {
            this.pageSize = 1;
        } else {
            this.pageSize = pageSize;
        }
        if (currentPage <= 0) {
//            this.currentPage = 1; Modif AC * MAJ BRAPI (page 0 = 0 )
            this.currentPage = 0;
        } else {
            this.currentPage = currentPage;
        }
        /**
         * Si le nombre d'éléments total est inférieur au nombre d'éléments par
         * * page, attribuer la valeur du nombre d'éléments par page au nombre *
         * d'éléments (norme)
         * MAJ BRAPI  Modif AC * MAJ BRAPI si (pageSize > sizeList ) pageSize = sizeList
         */
//        if (sizeList < this.pageSize) {
//            totalCount = this.pageSize; 
//        } else {
            totalCount = sizeList;
//        }
        /**
         * Pour avoir le nombre de pages totales il faut diviser le nombre de
         * résutats par le nombre de résultats par page mais comme il s'agit de
         * deux entiers, il peut y avoir un reste à cette division et si il y en
         * a un alors il faut le prendre en compte et rajouter une page au
         * nombre total de pages
         */
//        System.err.println(totalCount);
//        System.err.println(pageSize);
        if (totalCount % this.pageSize == 0) {
            totalPages = totalCount / this.pageSize;
        } else {
            totalPages = (totalCount / this.pageSize) + 1;
        }
//        System.err.println(totalPages);
    }

    /**
     * Getteur du champ pageSize correspondant au nombre de résultats par page
     *
     * @return la valeur de pageSize
     * @date 03/12/2015
     */
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * Getteur du champ currentPage correspondant à la page actuelle
     *
     * @return la valeur de currentPage
     * @date 03/12/2015
     */
    public Integer getCurrentPage() {
        return currentPage;
    }

    /**
     * Getteur du champ totalCount correspondant au nombre total de resultats
     *
     * @return la valeur de totalCount
     * @date 03/12/2015
     */
    public Integer getTotalCount() {
        return totalCount;
    }

    /**
     * Getteur du champ totalPages correspondant au nombre total de pages
     *
     * @return la valeur de totalPages
     * @date 03/12/2015
     */
    public Integer getTotalPages() {
        return totalPages;
    }
    
    
}
