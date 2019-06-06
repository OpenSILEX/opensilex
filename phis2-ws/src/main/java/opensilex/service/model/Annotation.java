//******************************************************************************
//                               Annotation.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 14 June 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import java.util.ArrayList;
import org.joda.time.DateTime;

/**
 * Annotation model.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
public class Annotation {

    /**
     * URI of the annotation.
     * @example http://www.phenome-fppn.fr/platform/id/annotation/8247af37-769c-495b-8e7e-78b1141176c2
     */
    private String uri;

    /** 
     * Creation date of this annotation format yyyy-MM-ddTHH:mm:ssZ.
     * @example 2018-06-25T15:13:59+0200
     */
    private DateTime created;

    /** 
     * Creator of the annotations.
     * @example http://www.phenome-fppn.fr/diaphen/id/agent/arnaud_charleroy
     */
     private String creator;

    /** 
     * Text that describes the annotation.
     * @example Ustilago maydis infection
     */
    private ArrayList<String> bodyValues;

    /**
     * Motivation instance URI that describes the purpose of the annotation.
     * @example http://www.w3.org/ns/oa#commenting
     */
    private String motivatedBy;

    /**
     * URIs that are the objects of the annotation.
     * @example http://www.phenome-fppn.fr/diaphen/2017/o1032481
     */
    private ArrayList<String> targets;
    
    public Annotation(String uri, DateTime created, String creator, ArrayList<String> bodyValues, String motivatedBy, ArrayList<String> targets) {
        this.uri = uri;
        this.created = created;
        this.creator = creator;
        this.bodyValues = bodyValues;
        this.motivatedBy = motivatedBy;
        this.targets = targets;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public DateTime getCreated() {
        return created;
    }

    public void setCreated(DateTime created) {
        this.created = created;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public ArrayList<String> getBodyValues() {
        return bodyValues;
    }

    public void setBodyValues(ArrayList<String> bodyValues) {
        this.bodyValues = bodyValues;
    }

    public void addBodyValue(String bodyValue) {
        // If null arraylist is initialized
        if (this.bodyValues == null) {
            this.bodyValues = new ArrayList<>();
        }
        this.bodyValues.add(bodyValue);
    }

    public ArrayList<String> getTargets() {
        return targets;
    }

    public void setTargets(ArrayList<String> targets) {
        this.targets = targets;
    }

    public void addTarget(String target) {
        // If null arraylist is initialized
        if (this.targets == null) {
            this.targets = new ArrayList<>();
        }
        this.targets.add(target);
    }

    public String getMotivatedBy() {
        return motivatedBy;
    }

    public void setMotivatedBy(String motivatedBy) {
        this.motivatedBy = motivatedBy;
    }
}
