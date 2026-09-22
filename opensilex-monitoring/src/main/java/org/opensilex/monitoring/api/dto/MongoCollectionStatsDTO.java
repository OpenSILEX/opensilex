//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

/**
 * Volumetry of one MongoDB collection.
 *
 * <p>A bean and not a record: the swagger generator introspects properties by bean
 * convention, and a record's accessors would produce an empty model and a broken
 * TypeScript client.</p>
 *
 * @author Arnaud Charleroy
 */
public class MongoCollectionStatsDTO {

    @ApiModelProperty(value = "collection name")
    @JsonProperty("name")
    private String name;

    @ApiModelProperty(value = "number of documents")
    @JsonProperty("document_count")
    private Long documentCount;

    @ApiModelProperty(value = "uncompressed size of the documents")
    @JsonProperty("size_bytes")
    private Long sizeBytes;

    @ApiModelProperty(value = "size on disk")
    @JsonProperty("storage_size_bytes")
    private Long storageSizeBytes;

    @ApiModelProperty(value = "total size of the indexes")
    @JsonProperty("index_size_bytes")
    private Long indexSizeBytes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getDocumentCount() {
        return documentCount;
    }

    public void setDocumentCount(Long documentCount) {
        this.documentCount = documentCount;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Long getStorageSizeBytes() {
        return storageSizeBytes;
    }

    public void setStorageSizeBytes(Long storageSizeBytes) {
        this.storageSizeBytes = storageSizeBytes;
    }

    public Long getIndexSizeBytes() {
        return indexSizeBytes;
    }

    public void setIndexSizeBytes(Long indexSizeBytes) {
        this.indexSizeBytes = indexSizeBytes;
    }

}
