package com.researchspace.dataverse.entities;

import com.researchspace.dataverse.http.FileUploadMetadata;
import lombok.Data;

/**
 * Checksum is part of the response from
 * {@link com.researchspace.dataverse.api.v1.DatasetOperations#uploadNativeFile(byte[], FileUploadMetadata, Identifier, String)}
 */
@Data
public class Checksum {
    private  String type;
    private  String value;
}
