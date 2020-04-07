//******************************************************************************
//                               BrapiPagination.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 25 Sept. 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.view.brapi;

/**
 * BrAPI Pagination model.
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
public class BrapiPagination {
    private Integer pageSize;
    private Integer currentPage;
    private Integer totalCount;
    private Integer totalPages;

    public BrapiPagination() {
    }

    public BrapiPagination(Integer pageSize, Integer currentPage, Integer totalCount) {
        this.pageSize = pageSize;
        this.currentPage = currentPage;
        this.totalCount = totalCount;
        
        if (pageSize != 0) {
            if (totalCount % this.pageSize == 0) {
                totalPages = totalCount / this.pageSize;
            } else {
                totalPages = (totalCount / this.pageSize) + 1;
            }
        } else {
            totalPages = 0;
        }
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }
}
