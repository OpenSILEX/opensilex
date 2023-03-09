package org.opensilex.core.datafile.dal;

import org.opensilex.core.data.dal.DataSearchFilter;

import java.net.URI;
import java.util.Collections;
import java.util.List;

public class DataFileSearchFilter extends DataSearchFilter {

    List<URI> rdfTypes;

    String fileName;

    public List<URI> getRdfTypes() {
        return rdfTypes;
    }

    public DataFileSearchFilter setRdfTypes(List<URI> rdfTypes) {
        this.rdfTypes = rdfTypes;
        return this;
    }

    public DataFileSearchFilter setRdfType(URI rdfType){
        this.rdfTypes = rdfType == null ? null : Collections.singletonList(rdfType);
        return this;
    }

    public String getFileName() {
        return fileName;
    }

    public DataFileSearchFilter setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }
}
