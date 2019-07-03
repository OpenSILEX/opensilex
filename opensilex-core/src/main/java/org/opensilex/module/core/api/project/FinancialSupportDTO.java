/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.module.core.api.project;

import java.net.URI;
import org.opensilex.module.core.dal.project.FinancialSupport;

/**
 * DTO of the financial support for the projects
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class FinancialSupportDTO {
    private URI uri;
    private String label;

    public FinancialSupportDTO(FinancialSupport financialSupport) {
        uri = financialSupport.getUri();
        label = financialSupport.getLabel();
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
