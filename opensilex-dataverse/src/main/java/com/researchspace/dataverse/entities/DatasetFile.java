package com.researchspace.dataverse.entities;

import com.researchspace.dataverse.http.FileUploadMetadata;
import lombok.Data;

import java.util.List;

/**
 * DatasetFile is part of the response from
 * {@link com.researchspace.dataverse.api.v1.DatasetOperations#uploadNativeFile(byte[], FileUploadMetadata, Identifier, String)}
 */
@Data
public class DatasetFile {
    private  String description;
    private  String label;
    private  String directoryLabel;
    private  String datasetVersionId;
    private List<String> categories;
    private  boolean restricted;
    private  int version;
    private DatasetFileDetails dataFile;

}
