//******************************************************************************
//                               TripletDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 7 March 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto;

import io.swagger.annotations.ApiModelProperty;
import opensilex.service.documentation.DocumentationAnnotation;
import opensilex.service.resource.validation.interfaces.Required;
import opensilex.service.resource.dto.manager.AbstractVerifiedClass;
import opensilex.service.resource.validation.interfaces.OType;
import opensilex.service.resource.validation.interfaces.URL;
import opensilex.service.model.Triplet;

/**
 * Triplet DTO.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class TripletDTO extends AbstractVerifiedClass {

    //subject of the triplet
    private String s;
    
    //property of the triplet 
    //(corresponds to the relation between the subject and the object)
    private String p;
    
    //object of the triplet
    private String o;
    
    //object type. It can be equals to literal or uri
    private String o_type;
    
    /**
     * Language. 
     * @example en-US
     */
    private String o_lang;
    //graph of the triplet
    private String g;

    @Override
    public Triplet createObjectFromDTO() {
        Triplet triplet = new Triplet();
        triplet.setSubject(s);
        triplet.setProperty(p);
        triplet.setObject(o);
        triplet.setObjectType(o_type);
        triplet.setObjectLang(o_lang);
        triplet.setG(g);

        return triplet;
    }

    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_TRIPLET_SUBJECT)
    public String getS() {
        return s;
    }

    public void setS(String s) {
        this.s = s;
    }

    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_TRIPLET_PROPERTY)
    public String getP() {
        return p;
    }

    public void setP(String p) {
        this.p = p;
    }

    @URL
    @Required
    @OType
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_TRIPLET_OBJECT)
    public String getO() {
        return o;
    }

    public void setO(String o) {
        this.o = o;
    }

    @URL
    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_TRIPLET_OBJECT_TYPE)
    public String getO_type() {
        return o_type;
    }

    public void setO_type(String o_type) {
        this.o_type = o_type;
    }

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_TRIPLET_OBJECT_LANGUAGE)
    public String getO_lang() {
        return o_lang;
    }

    public void setO_lang(String o_lang) {
        this.o_lang = o_lang;
    }

    @URL
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_TRIPLET_GRAPH)
    public String getG() {
        return g;
    }

    public void setG(String g) {
        this.g = g;
    }
}
