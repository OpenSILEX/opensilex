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
     * number of elements in the current returned page
     */
    private final long pageSize;

    /**
     * index number of the current returned page
     */
    private final long currentPage;

    /**
     * total number of elements available in the super set (unpaged)
     */
    private final long totalCount;

    /**
     * total number of pages available (total count / requested page size)
     */
    private final long totalPages;

    /**
     * Empty constructor assume no pagination
     */
    public PaginationDTO() {
        this.pageSize = 0;
        this.currentPage = 0;
        this.totalCount = 0;
        this.totalPages = 0;
    }

    /**
     * Constructor.
     *
     * @param pageSize Number of items per page
     * @param currentPage Current page index (0 based)
     * @param totalCount Total item count
     */
    public PaginationDTO(long pageSize, long currentPage, long totalCount) {
        this.pageSize = pageSize;
        this.currentPage = currentPage;
        this.totalCount = totalCount;

        // Add a page if the the total number of elements divided by the page
        if (pageSize == 0) {
            totalPages = 0;
        } else {
            if (totalCount % this.pageSize == 0) {
                totalPages = totalCount / this.pageSize;
            } else {
                totalPages = (totalCount / this.pageSize) + 1;
            }
        }
    }

    public long getPageSize() {
        return pageSize;
    }

    public long getCurrentPage() {
        return currentPage;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public long getTotalPages() {
        return totalPages;
    }
}
