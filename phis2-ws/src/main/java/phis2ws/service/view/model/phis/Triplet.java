//******************************************************************************
//                                       Triplet.java
//
// Author(s): Morgane Vidal <morgane.vidal@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 7 mars 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  7 mars 2018
// Subject:
//******************************************************************************
package phis2ws.service.view.model.phis;

/**
 * Represents the triplet view.
 * @see https://www.w3.org/wiki/JSON_Triple_Sets
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class Triplet {
    
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
    //graph uri of the triplet
    private String g;

    public Triplet() {
    }

    public String getS() {
        return s;
    }

    public void setS(String s) {
        this.s = s;
    }

    public String getP() {
        return p;
    }

    public void setP(String p) {
        this.p = p;
    }

    public String getO() {
        return o;
    }

    public void setO(String o) {
        this.o = o;
    }

    public String getO_type() {
        return o_type;
    }

    public void setO_type(String o_type) {
        this.o_type = o_type;
    }

    public String getO_lang() {
        return o_lang;
    }

    public void setO_lang(String o_lang) {
        this.o_lang = o_lang;
    }

    public String getG() {
        return g;
    }

    public void setG(String g) {
        this.g = g;
    }
}
