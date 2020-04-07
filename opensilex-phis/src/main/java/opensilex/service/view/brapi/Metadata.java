//******************************************************************************
//                               Metadata.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: 3 Dec. 2015
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.view.brapi;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Request metadata model.
 * @see Pagination, ResultForm
 * @update [Arnaud Charleroy] Oct. 2016: BrAPI datafiles update
 * @author Samuël Chérimont
 */

@ApiModel
public class Metadata {

    private final Pagination pagination;
    private List<Status> status;
    private List<String> datafiles;

    public void setDatafiles(List<String> datafiles) {
        this.datafiles = datafiles;
    }

    public Metadata() {
        pagination = null;
        status = null;
        datafiles = new ArrayList<>();
    }

    public Metadata(List<Status> statuslist) {
        pagination = null;
        status = statuslist;
        datafiles = new ArrayList<>();
    }

    public Metadata(Status status) {
        pagination = null;
        List<Status> statusList = new ArrayList<>();
        statusList.add(status);
        this.status = statusList;
        datafiles = new ArrayList<>();
    }

    /**
     * Constructor.
     * @param pageSize
     * @param currentPage
     * @param sizeList result element count
     * @see Pagination
     * @note the status list is always empty because there is no particular 
     * information about the status
     */
    public Metadata(int pageSize, int currentPage, int sizeList) {
        if (sizeList > 1) {
            pagination = new Pagination(pageSize, currentPage, sizeList);
        } else {
            pagination = null;
        }
        status = null;
        datafiles = new ArrayList<>();
    }

    public Metadata(int pageSize, int currentPage, int sizeList, List<Status> statuslist) {
        if (sizeList > 1) {
            pagination = new Pagination(pageSize, currentPage, sizeList);
        } else {
            pagination = null;
        }
        status = statuslist;
        datafiles = new ArrayList<>();
    }

    public Metadata(int pageSize, int currentPage, int sizeList, Status status) {
        if (sizeList > 1) {
            pagination = new Pagination(pageSize, currentPage, sizeList);
        } else {
            pagination = null;
        }
        List<Status> statusList = new ArrayList<>();
        statusList.add(status);
        this.status = statusList;
        datafiles = new ArrayList<>();
    }
    
    @ApiModelProperty(position = 1, required = true)
    public Pagination getPagination() {
        return pagination;
    }

    @ApiModelProperty(position = 2, required = true)
    public List<Status> getStatus() {
        return status;
    }

    public void setStatus(List<Status> status) {
        this.status = status;
    }

    public void addStatus(Status status) {
        if (this.status == null) {
            this.status = new ArrayList<>();
        }
        this.status.add(status);
    }

    @ApiModelProperty(position = 3, required = true)
    public List<String> getDatafiles() {
        return datafiles;
    }
}
