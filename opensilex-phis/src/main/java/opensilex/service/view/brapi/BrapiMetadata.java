//******************************************************************************
//                                 BrapiMetadata.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 25 Sept. 2018
// Contact: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.view.brapi;

import io.swagger.annotations.ApiModel;
import java.util.ArrayList;
import java.util.List;

/**
 * BrAPI metadata model.
 * @author Alice Boizet <alice.boizet@inra.fr>
 */
@ApiModel
public class BrapiMetadata {
    
    private BrapiPagination pagination;
    private List<Status> status;
    private List<String> datafiles;
    
    public BrapiMetadata() {
        pagination = null;
        status = null;
        datafiles = new ArrayList<>();
    }

    public BrapiMetadata(int pageSize, int currentPage, int sizeList) {
        pagination = new BrapiPagination(pageSize, currentPage, sizeList);
        status = null;
        datafiles = new ArrayList<>();
    }

    public BrapiPagination getPagination() {
        return pagination;
    }

    public void setPagination(BrapiPagination pagination) {
        this.pagination = pagination;
    }

    public List<Status> getStatus() {
        return status;
    }

    public void setStatus(List<Status> status) {
        this.status = status;
    }

    public List<String> getDatafiles() {
        return datafiles;
    }

    public void setDatafiles(List<String> datafiles) {
        this.datafiles = datafiles;
    }
}
