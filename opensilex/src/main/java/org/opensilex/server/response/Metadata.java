//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.server.response;

import io.swagger.annotations.ApiModel;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;


/**
 * Request metadata model.
 * @see Pagination, ResultForm
 * @update [Arnaud Charleroy] Oct. 2016: BrAPI datafiles update
 * @update [Vincent Migot] Nov. 2019: Refactor according to BrAPI v1.3
 * @author Samuël Chérimont
 */

@ApiModel
public class Metadata {

    private final Pagination pagination;
    private final List<StatusMessage> status = new ArrayList<>();
    private final List<URI> datafiles = new ArrayList<>();

    public Metadata(Pagination pagination) {
        this.pagination = pagination;
    }
    
    public void addStatus(StatusMessage status) {
        this.status.add(status);
    }
    
    public void addStatusList(List<StatusMessage> statusList) {
        this.status.addAll(statusList);
    }
    
    
    public void addDataFile(URI uri) {
        this.datafiles.add(uri);
    }
    
    public void addDataFiles(List<URI> uris) {
        this.datafiles.addAll(uris);
    }

    public Pagination getPagination() {
        return pagination;
    }

    public List<StatusMessage> getStatus() {
        return status;
    }

    public List<URI> getDatafiles() {
        return datafiles;
    }
    
    
}
