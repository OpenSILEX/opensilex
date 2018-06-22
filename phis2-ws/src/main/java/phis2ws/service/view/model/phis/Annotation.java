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

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import org.joda.time.DateTime;

/**
 * Represents an annotation
 *
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>
 */
public class Annotation {

    private final static String CREATED_LABEL = "creationDate";
    private final static String BODYVALUE_LABEL = "comment";
    
    private String uri;

    @SerializedName(CREATED_LABEL)
    private DateTime created;

    private String creator;

    @SerializedName(BODYVALUE_LABEL)
    private String bodyValue;

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
        this.targets.add(target);
    }

}
