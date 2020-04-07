//******************************************************************************
//                            ImageMetadata.java
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: 8 Dec. 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.Valid;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.model.ImageMetadata;

/**
 * Image metadata DTO.
 * @update [Andréas Garcia] Jan. 2019: modify "concern(s)" occurences into "concernedItem(s)"
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class ImageMetadataDTO extends AbstractVerifiedClass {
    
    private String rdfType;
    private List<ConcernedItemDTO> concernedItems;
    private ShootingConfigurationDTO configuration;
    private FileInformationDTO fileInfo;

    @Override
    public ImageMetadata createObjectFromDTO() {
       ImageMetadata imageMetadata = new ImageMetadata();
       imageMetadata.setRdfType(rdfType);
       concernedItems.forEach((concernedItemDTO) -> {
           imageMetadata.addConcernedItem(concernedItemDTO.createObjectFromDTO());
        });
       
       imageMetadata.setConfiguration(configuration.createObjectFromDTO());
       imageMetadata.setFileInformations(fileInfo.createObjectFromDTO());
       
       return imageMetadata;
    }
    
    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_IMAGE_TYPE)
    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }
    
    @Valid
    public List<ConcernedItemDTO> getConcernedItems() {
        return concernedItems;
    }

    public void setConcernedItems(List<ConcernedItemDTO> concernedItems) {
        this.concernedItems = concernedItems;
    }

    @Valid
    public ShootingConfigurationDTO getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ShootingConfigurationDTO configuration) {
        this.configuration = configuration;
    }

    @Valid
    public FileInformationDTO getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(FileInformationDTO fileInfo) {
        this.fileInfo = fileInfo;
    }
}
