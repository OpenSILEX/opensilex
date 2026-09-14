//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
// Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.monitoring.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.time.Instant;
import java.util.List;

/**
 * MongoDB volumetry.
 *
 * <p>A bean and not a record: the swagger generator introspects properties by bean
 * convention, and a record's accessors would produce an empty model and a broken
 * TypeScript client.</p>
 *
 * @author Arnaud Charleroy
 */
public class MongoStatsDTO {

    @ApiModelProperty(value = "database name")
    @JsonProperty("database")
    private String database;

    @ApiModelProperty(value = "total number of documents")
    @JsonProperty("object_count")
    private Long objectCount;

    @ApiModelProperty(value = "number of collections")
    @JsonProperty("collection_count")
    private Long collectionCount;

    @ApiModelProperty(value = "uncompressed size of the data")
    @JsonProperty("data_size_bytes")
    private Long dataSizeBytes;

    @ApiModelProperty(value = "size on disk")
    @JsonProperty("storage_size_bytes")
    private Long storageSizeBytes;

    @ApiModelProperty(value = "total size of the indexes")
    @JsonProperty("index_size_bytes")
    private Long indexSizeBytes;

    @ApiModelProperty(value = "per collection volumetry, largest first")
    @JsonProperty("collections")
    private List<MongoCollectionStatsDTO> collections;

    @ApiModelProperty(value = "when these figures were computed")
    @JsonProperty("computed_at")
    private Instant computedAt;

    @ApiModelProperty(value = "true when served from the cache")
    @JsonProperty("cached")
    private boolean cached;

    @ApiModelProperty(value = "why a figure is missing, null otherwise")
    @JsonProperty("message")
    private String message;

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public Long getObjectCount() {
        return objectCount;
    }

    public void setObjectCount(Long objectCount) {
        this.objectCount = objectCount;
    }

    public Long getCollectionCount() {
        return collectionCount;
    }

    public void setCollectionCount(Long collectionCount) {
        this.collectionCount = collectionCount;
    }

    public Long getDataSizeBytes() {
        return dataSizeBytes;
    }

    public void setDataSizeBytes(Long dataSizeBytes) {
        this.dataSizeBytes = dataSizeBytes;
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

    public List<MongoCollectionStatsDTO> getCollections() {
        return collections;
    }

    public void setCollections(List<MongoCollectionStatsDTO> collections) {
        this.collections = collections;
    }

    public Instant getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(Instant computedAt) {
        this.computedAt = computedAt;
    }

    public boolean isCached() {
        return cached;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
