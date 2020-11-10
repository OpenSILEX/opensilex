//******************************************************************************
//                          DataFileModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.dal;

import java.net.URI;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.query.BooleanExpression;

/**
 * DataFileModel
 * @author Alice Boizet
 */

@PersistenceCapable(table = "File")
@Index( name="fileUnicity", 
            unique="true", 
            members={"rdfType", "scientificObjects", "date"})
public class DataFileModel extends DataModel {
    @NotPersistent
    private final String baseURI = "id/file";
    
    URI rdfType;
    String filename;
    String path;

    public URI getRdfType() {
        return rdfType;
    }

    public void setRdfType(URI rdfType) {
        this.rdfType = rdfType;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String getGraphPrefix() {
        return baseURI;
    }
    
    @Override
    public BooleanExpression getURIExpr(URI uri) {
        QDataFileModel candidate = QDataFileModel.candidate();
        return candidate.uri.eq(uri);
    }

        
}
