package com.researchspace.dataverse.entities;

import com.researchspace.dataverse.http.FileUploadMetadata;
import lombok.Data;

import java.util.Date;

/**
 * DatasetFileDetails is a subsection of the response from
 * {@link com.researchspace.dataverse.api.v1.DatasetOperations#uploadNativeFile(byte[], FileUploadMetadata, Identifier, String)}
 */
@Data
public class DatasetFileDetails {
    private int id;
    private int filesize;
    private String persistentId;
    private String pidURL;
    private String filename;
    private String contentType;
    private String description;
    private String storageIdentifier;
    private String rootDataFileId;
    private String md5;
    private Checksum checksum;
    private Date creationDate;
}
