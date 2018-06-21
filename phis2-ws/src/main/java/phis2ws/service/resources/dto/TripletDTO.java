//******************************************************************************
//                                       TripletDTO.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 7 mars 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  7 mars 2018
// Subject:
//******************************************************************************
package phis2ws.service.resources.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.Map;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.resources.dto.constraints.Required;
import phis2ws.service.resources.dto.manager.AbstractVerifiedClass;
import phis2ws.service.view.model.phis.Triplet;

/**
 * Corresponds to the submitted json for a triplet
 *
 * @see https://www.w3.org/wiki/JSON_Triple_Sets
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
    //object language. Example of value : "en-US"
    private String o_lang;
    //graph of the triplet
    private String g;


    @Override
    public Triplet createObjectFromDTO() {
        Triplet triplet = new Triplet();
        triplet.setS(s);
        triplet.setP(p);
        triplet.setO(o);
        triplet.setO_type(o_type);
        triplet.setO_lang(o_lang);
        triplet.setG(g);

        return triplet;
    }

    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_TRIPLET_SUBJECT)
    public String getS() {
        return s;
    }

    public void setS(String s) {
        this.s = s;
    }

    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_TRIPLET_PROPERTY)
    public String getP() {
        return p;
    }

    public void setP(String p) {
        this.p = p;
    }

    @Required
    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_TRIPLET_OBJECT)
    public String getO() {
        return o;
    }

    public void setO(String o) {
        this.o = o;
    }

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

    @ApiModelProperty(example = DocumentationAnnotation.EXAMPLE_TRIPLET_GRAPH)
    public String getG() {
        return g;
    }

    public void setG(String g) {
        this.g = g;
    }

}
