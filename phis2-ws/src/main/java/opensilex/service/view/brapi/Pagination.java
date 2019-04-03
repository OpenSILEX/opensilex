//******************************************************************************
//                            Pagination.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: 3 Dec. 2015
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.view.brapi;

/**
 * Pagination model.
 * @author Samuël Cherimont
 */
public class Pagination {

    private Integer pageSize;
    private Integer currentPage;
    private Integer totalCount;
    private Integer totalPages;

    public Pagination() {
    }

    /**
     * Constructor.
     * @param pageSize
     * @param currentPage
     * @param listSize
     */
    public Pagination(Integer pageSize, Integer currentPage, Integer listSize) {
        if (pageSize <= 0) {
            this.pageSize = 1;
        } else {
            this.pageSize = pageSize;
        }
        if (currentPage <= 0) {
            this.currentPage = 0;
        } else {
            this.currentPage = currentPage;
        }
        
        totalCount = listSize;
        
        /**
         * Add a page if the the total number of elements divided by the page
         * size has a rest.
         */
        if (totalCount % this.pageSize == 0) {
            totalPages = totalCount / this.pageSize;
        } else {
            totalPages = (totalCount / this.pageSize) + 1;
        }
    }
    
    public Integer getPageSize() {
        return pageSize;
    }
    
    public Integer getCurrentPage() {
        return currentPage;
    }
    
    public Integer getTotalCount() {
        return totalCount;
    }
    
    public Integer getTotalPages() {
        return totalPages;
    }
}
