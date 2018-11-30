//******************************************************************************
//                                       ResultFileMetadata.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 3 sept. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.resources.dto.acquisitionSession.MetadataFileDTO;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;

/**
 * A class which represents the result part in the response form, adapted to the file metadata
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultMetadataFile extends Result<MetadataFileDTO> {
    /**
     * @param fileMetadata the file metadata of the result 
     */
    public ResultMetadataFile(ArrayList<MetadataFileDTO> fileMetadata) {
        super(fileMetadata);
    }
    
    /**
     * @param fileMetadata
     * @param pagination
     * @param paginate 
     */
    public ResultMetadataFile(ArrayList<MetadataFileDTO> fileMetadata, Pagination pagination, boolean paginate) {
        super(fileMetadata, pagination, paginate);
    }
}
