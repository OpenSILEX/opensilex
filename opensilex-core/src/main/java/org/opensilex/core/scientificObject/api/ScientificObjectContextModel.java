/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.core.scientificObject.api;

import java.net.URI;

/**
 *
 * @author vmigot
 */
public class ScientificObjectContextModel {
    
    private URI context;

    private String contextLabel;

    private URI contextType;

    private String contextTypeLabel;

    public URI getContext() {
        return context;
    }

    public void setContext(URI context) {
        this.context = context;
    }

    public String getContextLabel() {
        return contextLabel;
    }

    public void setContextLabel(String contextLabel) {
        this.contextLabel = contextLabel;
    }

    public URI getContextType() {
        return contextType;
    }

    public void setContextType(URI contextType) {
        this.contextType = contextType;
    }

    public String getContextTypeLabel() {
        return contextTypeLabel;
    }

    public void setContextTypeLabel(String contextTypeLabel) {
        this.contextTypeLabel = contextTypeLabel;
    }
    
    
}
