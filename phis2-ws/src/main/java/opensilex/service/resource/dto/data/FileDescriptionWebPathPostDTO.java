//******************************************************************************
//                                FileDescriptionWebPathPostDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 23 avr. 2019
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import opensilex.service.configuration.DateFormat;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.model.ConcernedItem;
import opensilex.service.model.FileDescription;
import opensilex.service.resource.dto.ConcernedItemDTO;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.validation.interfaces.Date;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.validation.interfaces.URL;

/**
 *
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class FileDescriptionWebPathPostDTO extends AbstractVerifiedClass {
    
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
     * Web path to the file.
     * @example http://www.opensilex.org/images/img001.jpg
     */
    String webPath;
    
    /**
     * List of concerned items related to the data file.
     */
    List<ConcernedItemDTO> concernedItems;
    
    /**
     * URI of the provenance from which data file come.
     * @example http://www.phenome-fppn.fr/diaphen/id/provenance/1552404943020
     */
    String provenanceUri;
    
    /**
     * Additional information for the file description. Its content depends on the type of file. 
     * @example 
     *{
     *  "SensingDevice" => "http://www.opensilex.org/demo/s001",
     *  "Vector" => "http://www.opensilex.org/demo/v001"
     *}
     */
    Map<String, Object> metadata;

    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_IMAGE_TYPE)
    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }

    @Date(DateFormat.YMDTHMSZ)
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_XSDDATETIME)
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Valid
    public List<ConcernedItemDTO> getConcernedItems() {
        return concernedItems;
    }

    public void setConcernedItems(List<ConcernedItemDTO> concernedItems) {
        this.concernedItems = concernedItems;
    }

    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_PROVENANCE_URI)
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
    
    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_DATA_FILE_WEB_PATH)
    public String getWebPath() {
        return webPath;
    }

    public void setWebPath(String webPath) {
        this.webPath = webPath;
    }

    @Override
    public FileDescription createObjectFromDTO() throws ParseException {
        FileDescription description = new FileDescription();
        
        description.setRdfType(getRdfType());
        
        SimpleDateFormat df = new SimpleDateFormat(DateFormat.YMDTHMSZ.toString());
        description.setDate(df.parse(getDate()));
        
        List<ConcernedItem> items = new ArrayList<>();
        getConcernedItems().forEach((itemDTO) -> {
            items.add(itemDTO.createObjectFromDTO());
        });
        description.setConcernedItems(items);
        description.setProvenanceUri(getProvenanceUri());
        description.setMetadata(getMetadata());
        description.setWebPath(getWebPath());
        
        return description;
    }
    
    /**
     * Method to unserialize FileDescriptionPostDTO.
     * Required because this data is received as @FormDataParam.
     * @param param
     * @return
     * @throws IOException 
     */
    public static FileDescriptionPostDTO fromString(String param) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(param, FileDescriptionPostDTO.class);
    }
}
