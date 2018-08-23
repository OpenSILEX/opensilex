//**********************************************************************************************
//                                       ResultImageMetadata.java
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 2 janv. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//***********************************************************************************************
package phis2ws.service.view.brapi.results;

import java.util.ArrayList;
import phis2ws.service.view.brapi.Pagination;
import phis2ws.service.view.manager.Result;
import phis2ws.service.view.model.phis.ImageMetadata;

/**
 * A class which represents the result part in the response form, adapted to the images metadata
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ResultImageMetadata extends Result<ImageMetadata> {
    /**
     * Constructor which calls the mother-class constructor in the case of a
     * list with only 1 element
     * @param imagesMetadata 
     */
    public ResultImageMetadata(ArrayList<ImageMetadata> imagesMetadata) {
        super(imagesMetadata);
    }
    
    /**
     * Contructor which calls the mother-class constructor in the case of a
     * list with several elements
     * @param imagesMetadata
     * @param pagination
     * @param paginate 
     */
    public ResultImageMetadata(ArrayList<ImageMetadata> imagesMetadata, Pagination pagination, boolean paginate) {
        super(imagesMetadata, pagination, paginate);
    }
}
