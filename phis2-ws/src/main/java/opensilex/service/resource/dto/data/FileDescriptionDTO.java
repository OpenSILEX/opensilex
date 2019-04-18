//******************************************************************************
//                             FileDescriptionDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 7 March 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import opensilex.service.configuration.DateFormat;
import opensilex.service.resource.dto.ConcernedItemDTO;
import opensilex.service.model.ConcernedItem;
import opensilex.service.model.FileDescription;

/**
 * File description DTO.
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
public class FileDescriptionDTO {

    /**
     * URI of the data file.
     * @example http://www.phenome-fppn.fr/diaphen/id/dataFile/RGBImage/55fjbbmtmr4m3kkizslzaddfkdt2ranum3ikz6cdiajqzfdc7yqa31d87b83efac4c358ceb5b0da6ed27ff
     */
    String uri;
    
    /**
     * RDF type of the data file.
     * @example http://www.opensilex.org/vocabulary/oeso#HemisphericalImage
     */
    String rdfType;
    
    /**
     * Date corresponding to the given value. The format should be yyyy-MM-ddTHH:mm:ssZ.
     * @example 2018-06-25T15:13:59+0200    
     */
    String date;
    
    /**
     * A List of concerned items related to the data file.
     */
    List<ConcernedItemDTO> concernedItems;
    
    /**
     * URI of the provenance from which data file comes.
     * @example http://www.phenome-fppn.fr/diaphen/id/provenance/1552404943020
     */
    String provenanceUri;
    
    /**
     * Additional information for the file description. Its content depends on 
     * the type of file. 
     * @example 
     *{
     *  "SensingDevice" => "http://www.opensilex.org/demo/s001",
     *  "Vector" => "http://www.opensilex.org/demo/v001"
     *}
     */
    Map<String, Object> metadata;

    /**
     * Constructor of the DTO from a FileDescription model.
     * @param description 
     */
    public FileDescriptionDTO(FileDescription description) {
        SimpleDateFormat df = new SimpleDateFormat(DateFormat.YMDTHMSZ.toString());
        
        if (description.getDate() != null) {
            setDate(df.format(description.getDate()));
        }
        
        setUri(description.getUri());
        setProvenanceUri(description.getProvenanceUri());
        setRdfType(description.getRdfType());
        
        List<ConcernedItemDTO> items = new ArrayList<>();
        for (ConcernedItem item : description.getConcernedItems()) {
            ConcernedItemDTO itemDTO = new ConcernedItemDTO();
            itemDTO.setTypeURI(item.getRdfType());
            itemDTO.setUri(item.getUri());
            items.add(itemDTO);
        }
        setConcernedItems(items);
        setMetadata(description.getMetadata());
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
    
    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<ConcernedItemDTO> getConcernedItems() {
        return concernedItems;
    }

    public void setConcernedItems(List<ConcernedItemDTO> concernedItems) {
        this.concernedItems = concernedItems;
    }

    public String getProvenanceUri() {
        return provenanceUri;
    }

    public void setProvenanceUri(String provenanceUri) {
        this.provenanceUri = provenanceUri;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
