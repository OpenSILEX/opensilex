/*                                     ImportDataIndex.java
 *  OpenSILEX
 *  Copyright © INRA 2021
 *  Creation date:  16 March, 2021
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.experiment.utils;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

/**
 * A class that help to aggregate data for CSV export
 * @author A. Charleroy
 */
public class ImportDataIndex {
    private Instant date;
    private URI variableUri;
    private URI provenanceUri;
    private URI objectUri; 

    public URI getProvenanceUri() {
        return provenanceUri;
    }

    public void setProvenanceUri(URI provenanceUri) {
        this.provenanceUri = provenanceUri;
    }
    
    public URI getObjectUri() {
        return objectUri;
    }

    public void setObjectUri(URI objectUri) {
        this.objectUri = objectUri;
    }
   
    public Instant getDate() {
        return date;
    }

    public void setDate(Instant date) {
        this.date = date;
    }

    public URI getVariableUri() {
        return variableUri;
    }

    public void setVariableUri(URI variableUri) {
        this.variableUri = variableUri;
    }
    
    public ImportDataIndex(Instant date, URI variableUri, URI provenanceUri, URI objectUri) {
        this.date = date;
        this.variableUri = variableUri;
        this.provenanceUri = provenanceUri;
        this.objectUri = objectUri;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.date);
        hash = 67 * hash + Objects.hashCode(this.variableUri);
        hash = 67 * hash + Objects.hashCode(this.provenanceUri);
        hash = 67 * hash + Objects.hashCode(this.objectUri);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ImportDataIndex other = (ImportDataIndex) obj;
        if (!Objects.equals(this.date, other.date)) {
            return false;
        }
        if (!Objects.equals(this.variableUri, other.variableUri)) {
            return false;
        }
        if (!Objects.equals(this.provenanceUri, other.provenanceUri)) {
            return false;
        }
        if (!Objects.equals(this.objectUri, other.objectUri)) {
            return false;
        }
        return true;
    }

  

}
