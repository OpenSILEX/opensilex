//******************************************************************************
//                          BrAPIv1OntologyReferenceDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// BrAPIv1ContactDTO: alice.boizet@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.brapi.model;

import java.util.List;

/**
 * @see <a href="https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI/1.3">BrAPI documentation</a>
 * @author Alice Boizet
 */
public class BrAPIv1OntologyReferenceDTO {
    private List<BrAPIv1DocumentationLinkDTO> documentationLinks;
    private String ontologyDbId;
    private String ontologyName;
    private String version;

    public List<BrAPIv1DocumentationLinkDTO> getDocumentationLinks() {
        return documentationLinks;
    }

    public void setDocumentationLinks(List<BrAPIv1DocumentationLinkDTO> documentationLinks) {
        this.documentationLinks = documentationLinks;
    }

    public String getOntologyDbId() {
        return ontologyDbId;
    }

    public void setOntologyDbId(String ontologyDbId) {
        this.ontologyDbId = ontologyDbId;
    }

    public String getOntologyName() {
        return ontologyName;
    }

    public void setOntologyName(String ontologyName) {
        this.ontologyName = ontologyName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
    
}
