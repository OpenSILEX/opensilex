package com.researchspace.dataverse.http;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Request object for metadata included with native file upload to an existing dataset
 */
@Data
@Builder
public class FileUploadMetadata {
    private String description;
    private String directoryLabel;
    private List<String> categories;
    private boolean restrict;
    private boolean tabIngest;

}
