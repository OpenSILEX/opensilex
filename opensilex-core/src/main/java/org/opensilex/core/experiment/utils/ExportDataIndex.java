/*                                     ExportDataIndex.java
 *  OpenSILEX
 *  Copyright © INRA 2021
 *  Creation date:  16 March, 2021
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.experiment.utils;

import java.net.URI;
import java.util.Objects;

/**
 * A class that help to aggregate data for CSV export
 * @author A. Charleroy
 */
public class ExportDataIndex {
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

    public ExportDataIndex(URI provenanceUri, URI objectUri) {
        this.provenanceUri = provenanceUri;
         this.objectUri = objectUri;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + Objects.hashCode(this.provenanceUri);
        hash = 79 * hash + Objects.hashCode(this.objectUri);
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
        final ExportDataIndex other = (ExportDataIndex) obj;
        if (!Objects.equals(this.provenanceUri, other.provenanceUri)) {
            return false;
        }
        if (!Objects.equals(this.objectUri, other.objectUri)) {
            return false;
        }
        return true;
    }

}
