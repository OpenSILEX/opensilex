package org.opensilex.core.datafile.dal;

import org.opensilex.core.data.api.DataSearchDTO;

import java.net.URI;

public class DataFileSearchFilter extends DataSearchDTO {

    private String filename;
    private String path;
    private URI archive;

    public String getFilename() {
        return filename;
    }

    public DataFileSearchFilter setFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public String getPath() {
        return path;
    }

    public DataFileSearchFilter setPath(String path) {
        this.path = path;
        return this;
    }

    public URI getArchive() {
        return archive;
    }

    public DataFileSearchFilter setArchive(URI archive) {
        this.archive = archive;
        return this;
    }
}
