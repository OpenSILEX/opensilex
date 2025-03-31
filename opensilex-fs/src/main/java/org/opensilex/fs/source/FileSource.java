package org.opensilex.fs.source;

import java.util.Map;
import java.util.Objects;

/**
 * This class must represent the state of a file storage connection, this one must
 * be findable, identifiable, distinguishable.
 * Each connection to a file system/storage can extend this class and redefine the sets of properties
 * required to establish a connection to the file storage server/database, like url, database, region, etc
 */
public abstract class FileSource {

    private String id;
    private String type;
    private Map<String, Object> settings;

    protected FileSource() {
    }

    protected FileSource(String id, String type, Map<String, Object> settings) {
        this.id = id;
        this.type = type;
        this.settings = settings;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public FileSource setSettings(Map<String, Object> settings) {
        this.settings = settings;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileSource that)) return false;
        return Objects.equals(getId(), that.getId()) && Objects.equals(getType(), that.getType()) && Objects.equals(getSettings(), that.getSettings());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getType(), getSettings());
    }
}
