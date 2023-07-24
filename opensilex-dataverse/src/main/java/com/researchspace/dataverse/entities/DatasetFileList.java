package com.researchspace.dataverse.entities;

import lombok.Data;

import java.util.List;

/**
 * DatasetFileList is the response from uploading a file using the native API
 */
@Data
public class DatasetFileList {
    List<DatasetFile> files;
}

