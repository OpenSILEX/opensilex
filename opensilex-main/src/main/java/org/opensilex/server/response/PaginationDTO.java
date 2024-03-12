//******************************************************************************
//                          PaginationDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.response;

/**
 * Response metadata Pagination DTO.
 *
 * @see org.opensilex.server.response.MetadataDTO
 * @author Samuël Cherimont
 */
public class PaginationDTO {

    /**
     * number of elements in the current returned page.
     */
    private final long pageSize;

    /**
     * index number of the current returned page.
     */
    private final long currentPage;

    /**
     * total number of elements available in the super set (unpaged).
     */
    private final long totalCount;

    /**
     * total number of pages available (total count / requested page size).
     */
    private final long totalPages;

    /**
     * Indicate if the count query was used with a limit on the number of element to count.
     * This can be done for performance reason, in order to not iterate each document to count, when this number becomes high
     */
    private final long limitCount;

    private boolean hasNextPage;

    /**
     * Empty constructor assume no pagination.
     */
    public PaginationDTO() {
        this.pageSize = 0;
        this.currentPage = 0;
        this.totalCount = 0;
        this.totalPages = 0;
        this.limitCount = 0;
        this.hasNextPage = false;
    }

    /**
     * Constructor.
     *
     * @param pageSize Number of items per page
     * @param currentPage Current page index (0 based)
     * @param totalCount Total item count
     */
    public PaginationDTO(long pageSize, long currentPage, long totalCount) {
        this(pageSize, currentPage, totalCount, 0);
    }

    public PaginationDTO(long pageSize, long currentPage, long totalCount, long limitCount) {
        this.pageSize = pageSize;
        this.currentPage = currentPage;
        this.totalCount = totalCount;

        // Add a page if the total number of elements divided by the page
        if (pageSize == 0) {
            totalPages = 0;
        } else {
            if (totalCount % this.pageSize == 0) {
                totalPages = totalCount / this.pageSize;
            } else {
                totalPages = (totalCount / this.pageSize) + 1;
            }
        }
        this.limitCount = limitCount;

        // there is a next page if the last page is not reached
        this.hasNextPage = totalPages > currentPage;
    }

    public PaginationDTO(long pageSize, long currentPage, boolean hasNextPage) {
        this.pageSize = pageSize;
        this.currentPage = currentPage;
        this.hasNextPage = hasNextPage;
        this.totalCount = 0;
        this.totalPages = 0;
        this.limitCount = 0;
    }

    /**
     * Getter for page size.
     *
     * @return page size
     */
    public long getPageSize() {
        return pageSize;
    }

    /**
     * Getter for current page.
     *
     * @return current page
     */
    public long getCurrentPage() {
        return currentPage;
    }

    /**
     * Getter for total count.
     *
     * @return total count
     */
    public long getTotalCount() {
        return totalCount;
    }

    /**
     * Getter for total pages.
     *
     * @return total pages
     */
    public long getTotalPages() {
        return totalPages;
    }

    public long getLimitCount() {
        return limitCount;
    }

    public boolean getHasNextPage() {
        return hasNextPage;
    }

    public void setHasNextPage(boolean hasNextPage) {
        this.hasNextPage = hasNextPage;
    }
}
