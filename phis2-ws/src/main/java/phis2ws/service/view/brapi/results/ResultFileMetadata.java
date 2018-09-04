//******************************************************************************
//                                       ResultFileMetadata.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 3 sept. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.resources.dto.FileMetadataDTO;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;

/**
 * A class which represents the result part in the response form, adapted to the file metadata
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultFileMetadata extends Result<FileMetadataDTO> {
    /**
     * @param fileMetadata the file metadata of the result 
     */
    public ResultFileMetadata(ArrayList<FileMetadataDTO> fileMetadata) {
        super(fileMetadata);
    }
    
    /**
     * @param fileMetadata
     * @param pagination
     * @param paginate 
     */
    public ResultFileMetadata(ArrayList<FileMetadataDTO> fileMetadata, Pagination pagination, boolean paginate) {
        super(fileMetadata, pagination, paginate);
    }
}
