/*
 * *****************************************************************************
 *                         MetadataDTO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 04/07/2024 17:38
 * Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr, gabriel.besombes@inrae.fr
 * *****************************************************************************
 */
package org.opensilex.server.response;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * Response metadata DTO.
 * See Brapi for details:
 * https://brapi.docs.apiary.io/#introduction/structure-of-the-response-object:/the-metadata-key
 *
 * Produced JSON:
 * {
 *     pagination: {
 *         pageSize: ... page size of the result if paginated request or 0
 *         currentPage: ... current page of the result if paginated request or 0
 *         totalCount: ... total element count if paginated request or 0
 *         totalPages: ... total pages count if paginated request or 0
 *     },
 *     status: [
 *         ... array of custom status as {message: "status message", messageType: ERROR|DEBUG|WARN|INFO}
 *     ],
 *     datafiles: [
 *         ... array of URI in case of resource creation
 *     ]
 * }
 * </pre>
 *
 * @see org.opensilex.server.response.PaginationDTO
 * @see org.opensilex.server.response.StatusDTO
 * @see org.opensilex.server.response.JsonResponse
 * @author Samuël Chérimont
 * @author Arnaud Charleroy - Oct. 2016: BrAPI datafiles update
 * @author Vincent Migot - Nov. 2019: Refactor according to BrAPI v1.3
 */
public class MetadataDTO {

    /**
     * PaginationDTO section.
     */
    private final PaginationDTO pagination;

    /**
     * Array of custom status message.
     */
    private final List<StatusDTO> status = new ArrayList<>();

    /**
     * Array of result URI.
     */
    private final List<URI> datafiles = new ArrayList<>();

    /**
     * Constructor with pagination.
     *
     * @param pagination
     */
    public MetadataDTO(PaginationDTO pagination) {
        this.pagination = pagination;
    }

    /**
     * Default empty constructor.
     */
    public MetadataDTO() {
        pagination = new PaginationDTO();
    }

    /**
     * Add metadata status.
     *
     * @param status status to add
     */
    public void addStatus(StatusDTO status) {
        this.status.add(status);
    }

    /**
     * Add metadata status list.
     *
     * @param statusList status list to add
     */
    public void addStatusList(List<StatusDTO> statusList) {
        this.status.addAll(statusList);
    }

    /**
     * Add metadata data file.
     *
     * @param uri file uri
     */
    public void addDataFile(URI uri) {
        this.datafiles.add(uri);
    }

    /**
     * Add metadata data file list.
     *
     * @param uris list of file uri
     */
    public void addDataFiles(List<URI> uris) {
        this.datafiles.addAll(uris);
    }

    /**
     * Getter for pagination.
     *
     * @return pagination DTO
     */
    public PaginationDTO getPagination() {
        return pagination;
    }

    /**
     * Getter for metadata status.
     *
     * @return list of status
     */
    public List<StatusDTO> getStatus() {
        return status;
    }

    /**
     * Getter for metadata data files.
     *
     * @return list fo files URI
     */
    public List<URI> getDatafiles() {
        return datafiles;
    }

}
