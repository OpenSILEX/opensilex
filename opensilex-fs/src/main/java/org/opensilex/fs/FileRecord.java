package org.opensilex.fs;

import org.opensilex.fs.source.FileSource;

/**
 * Record of a file in any file storage
 * The file can have any type (document, image) and can be saved in any source (local, s3, mongodb, etc)
 *
 * @author rcolin
 */
public class FileRecord {

    /* Unique logical id of the file (can be mapped to another id/uri) */
    private String id;

    /* Unique hash of physical file content to guarantee the uniqueness/integrity of the file */
    private String hash;

    /* Physical data-source access and path/key inside the source */
    private FileSource fileSource;
    private String path;
    private String name;

    /* Metadata */
    private long size;
    private String type;

    public long getSize() {
        return size;
    }

    public FileRecord setSize(long size) {
        this.size = size;
        return this;
    }

    public String getId() {
        return id;
    }

    public FileRecord setId(String id) {
        this.id = id;
        return this;
    }

    public String getHash() {
        return hash;
    }

    public FileRecord setHash(String hash) {
        this.hash = hash;
        return this;
    }

    public String getPath() {
        return path;
    }

    public FileRecord setPath(String path) {
        this.path = path;
        return this;
    }

    public String getName() {
        return name;
    }

    public FileRecord setName(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public FileRecord setType(String type) {
        this.type = type;
        return this;
    }

    public FileSource getFileSource() {
        return fileSource;
    }

    public FileRecord setFileSource(FileSource fileSource) {
        this.fileSource = fileSource;
        return this;
    }
}
