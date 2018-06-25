//******************************************************************************
//                                       Annotation.java
//
// Author(s): Arnaud Charleroy<arnaud.charleroy@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 14 juin 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  14 juin 2018
// Subject: Represents an annotation
//******************************************************************************
package phis2ws.service.view.model.phis;

import java.util.ArrayList;
import org.joda.time.DateTime;

/**
 * Represents an annotation
 *
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>
 */
public class Annotation {

  

    private String uri;

    private DateTime created;

    private String creator;

    private String bodyValue;
    
    private String motivatedBy;

    private ArrayList<String> targets;

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

    public String getBodyValue() {
        return bodyValue;
    }

    public void setBodyValue(String bodyValue) {
        this.bodyValue = bodyValue;
    }

    public ArrayList<String> getTargets() {
        return targets;
    }

    public void setTargets(ArrayList<String> targets) {
        this.targets = targets;
    }

    public void addTarget(String target) {
        if ( this.targets == null) {
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
