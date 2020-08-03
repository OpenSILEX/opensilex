/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.api;

import java.net.URI;

/**
 *
 * @author vmigot
 */
public class VueDatatypeDTO {

    protected URI uri;
    protected String intputComponent;
    protected String viewComponent;
    protected String labelKey;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getIntputComponent() {
        return intputComponent;
    }

    public void setIntputComponent(String intputComponent) {
        this.intputComponent = intputComponent;
    }

    public String getViewComponent() {
        return viewComponent;
    }

    public void setViewComponent(String viewComponent) {
        this.viewComponent = viewComponent;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public void setLabelKey(String labelKey) {
        this.labelKey = labelKey;
    }

}
