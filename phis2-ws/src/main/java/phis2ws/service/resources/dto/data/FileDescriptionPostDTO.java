//******************************************************************************
//                                       FileDescriptionPostDTO.java
// SILEX-PHIS
// Copyright © INRA 2019
// Creation date: 7 mars 2019
// Contact: Expression userEmail is undefined on line 6, column 15 in file:///home/vincent/opensilex/phis-ws/phis2-ws/licenseheader.txt., anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import phis2ws.service.configuration.DateFormat;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.dto.ConcernItemDTO;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.resources.validation.interfaces.Date;
import phis2ws.service.resources.validation.interfaces.Required;
import phis2ws.service.resources.validation.interfaces.URL;
import phis2ws.service.view.model.phis.ConcernedItem;
import phis2ws.service.view.model.phis.FileDescription;

/**
 * This class describe FileDescription Metadata
 * @author vincent
 */
public class FileDescriptionPostDTO extends AbstractVerifiedClass {
    
    String rdfType;
    
    String date;
    
    List<ConcernItemDTO> concernedItems;
    
    String provenanceUri;
    
    Map<String, Object> metadata;

    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_RDFTYPE_URI)
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
    public List<ConcernItemDTO> getConcernedItems() {
        return concernedItems;
    }

    public void setConcernedItems(List<ConcernItemDTO> concernedItems) {
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
        
        return description;
    }
    
    /**
     * Method to unserialize FileDescriptionPostDTO, required because this data is received as @FormDataParam
     * @param param
     * @return
     * @throws IOException 
     */
    public static FileDescriptionPostDTO fromString(String param) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(param, FileDescriptionPostDTO.class);
    }
}
