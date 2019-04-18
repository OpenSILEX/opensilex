//******************************************************************************
//                            RdfResourceDefinitionDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 7 Sept. 2018
// Contact: vincent.migot@inra.fr anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.rdfResourceDefinition;

import io.swagger.annotations.ApiModelProperty;
import java.util.ArrayList;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.model.RdfResourceDefinition;
import opensilex.service.model.Property;

/**
 * RDF resource definition DTO.
 * @see PropertyDTO
 * @author Vincent Migot <vincent.migot@inra.fr>
 */
public class RdfResourceDefinitionDTO extends AbstractVerifiedClass {

    //uri of the rdf resource
    protected String uri;
    
    //label of the rdf resource
    protected String label;
    
    //list of the properties of the rdf resource
    protected ArrayList<PropertyDTO> properties = new ArrayList<>();

    /**
     * Default empty constructor
     */
    public RdfResourceDefinitionDTO() {
    }
     
    /**
     * Constructor to instantiate DTO from a Model.
     * @param definition 
     */
    public RdfResourceDefinitionDTO(RdfResourceDefinition definition) {
       this();
       this.uri = definition.getUri();
       this.label = definition.getLabel();
       
       // Convert every property to DTO by using the overridable method getDTOInstance
       definition.getProperties().forEach((property) -> {
           PropertyDTO propertyDTO = this.getDTOInstance(property);
           this.properties.add(propertyDTO);
       });
    }

    /**
     * Returns an instance of PropertyDTO from a property
     * Could be implemented by subclasses to return different DTO depending on property
     * @param property
     * @return 
     */
    protected PropertyDTO getDTOInstance(Property property) {
        return new PropertyDTO(property);
    }
    
    @Override
    public RdfResourceDefinition createObjectFromDTO() {
        RdfResourceDefinition resource = new RdfResourceDefinition();
        resource.setUri(uri);

        getProperties().forEach((property) -> {
            resource.addProperty(property.createObjectFromDTO());
        });

        return resource;
    }

    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_INFRASTRUCTURE_URI)
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_VECTOR_LABEL)
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @NotEmpty
    @NotNull
    @Valid
    public ArrayList<PropertyDTO> getProperties() {
        return properties;
    }

    public void addProperty(PropertyDTO property) {
        properties.add(property);
    }
}
