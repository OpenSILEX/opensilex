// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
package com.researchspace.dataverse.entities;

import com.researchspace.dataverse.http.FileUploadMetadata;
import java.util.Date;

/**
 * DatasetFileDetails is a subsection of the response from
 * {@link com.researchspace.dataverse.api.v1.DatasetOperations#uploadNativeFile(byte[], FileUploadMetadata, Identifier, String)}
 */
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

    @SuppressWarnings("all")
    public DatasetFileDetails() {
    }

    @SuppressWarnings("all")
    public int getId() {
        return this.id;
    }

    @SuppressWarnings("all")
    public int getFilesize() {
        return this.filesize;
    }

    @SuppressWarnings("all")
    public String getPersistentId() {
        return this.persistentId;
    }

    @SuppressWarnings("all")
    public String getPidURL() {
        return this.pidURL;
    }

    @SuppressWarnings("all")
    public String getFilename() {
        return this.filename;
    }

    @SuppressWarnings("all")
    public String getContentType() {
        return this.contentType;
    }

    @SuppressWarnings("all")
    public String getDescription() {
        return this.description;
    }

    @SuppressWarnings("all")
    public String getStorageIdentifier() {
        return this.storageIdentifier;
    }

    @SuppressWarnings("all")
    public String getRootDataFileId() {
        return this.rootDataFileId;
    }

    @SuppressWarnings("all")
    public String getMd5() {
        return this.md5;
    }

    @SuppressWarnings("all")
    public Checksum getChecksum() {
        return this.checksum;
    }

    @SuppressWarnings("all")
    public Date getCreationDate() {
        return this.creationDate;
    }

    @SuppressWarnings("all")
    public void setId(final int id) {
        this.id = id;
    }

    @SuppressWarnings("all")
    public void setFilesize(final int filesize) {
        this.filesize = filesize;
    }

    @SuppressWarnings("all")
    public void setPersistentId(final String persistentId) {
        this.persistentId = persistentId;
    }

    @SuppressWarnings("all")
    public void setPidURL(final String pidURL) {
        this.pidURL = pidURL;
    }

    @SuppressWarnings("all")
    public void setFilename(final String filename) {
        this.filename = filename;
    }

    @SuppressWarnings("all")
    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    @SuppressWarnings("all")
    public void setDescription(final String description) {
        this.description = description;
    }

    @SuppressWarnings("all")
    public void setStorageIdentifier(final String storageIdentifier) {
        this.storageIdentifier = storageIdentifier;
    }

    @SuppressWarnings("all")
    public void setRootDataFileId(final String rootDataFileId) {
        this.rootDataFileId = rootDataFileId;
    }

    @SuppressWarnings("all")
    public void setMd5(final String md5) {
        this.md5 = md5;
    }

    @SuppressWarnings("all")
    public void setChecksum(final Checksum checksum) {
        this.checksum = checksum;
    }

    @SuppressWarnings("all")
    public void setCreationDate(final Date creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    @SuppressWarnings("all")
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof DatasetFileDetails)) return false;
        final DatasetFileDetails other = (DatasetFileDetails) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.getId() != other.getId()) return false;
        if (this.getFilesize() != other.getFilesize()) return false;
        final Object this$persistentId = this.getPersistentId();
        final Object other$persistentId = other.getPersistentId();
        if (this$persistentId == null ? other$persistentId != null : !this$persistentId.equals(other$persistentId)) return false;
        final Object this$pidURL = this.getPidURL();
        final Object other$pidURL = other.getPidURL();
        if (this$pidURL == null ? other$pidURL != null : !this$pidURL.equals(other$pidURL)) return false;
        final Object this$filename = this.getFilename();
        final Object other$filename = other.getFilename();
        if (this$filename == null ? other$filename != null : !this$filename.equals(other$filename)) return false;
        final Object this$contentType = this.getContentType();
        final Object other$contentType = other.getContentType();
        if (this$contentType == null ? other$contentType != null : !this$contentType.equals(other$contentType)) return false;
        final Object this$description = this.getDescription();
        final Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final Object this$storageIdentifier = this.getStorageIdentifier();
        final Object other$storageIdentifier = other.getStorageIdentifier();
        if (this$storageIdentifier == null ? other$storageIdentifier != null : !this$storageIdentifier.equals(other$storageIdentifier)) return false;
        final Object this$rootDataFileId = this.getRootDataFileId();
        final Object other$rootDataFileId = other.getRootDataFileId();
        if (this$rootDataFileId == null ? other$rootDataFileId != null : !this$rootDataFileId.equals(other$rootDataFileId)) return false;
        final Object this$md5 = this.getMd5();
        final Object other$md5 = other.getMd5();
        if (this$md5 == null ? other$md5 != null : !this$md5.equals(other$md5)) return false;
        final Object this$checksum = this.getChecksum();
        final Object other$checksum = other.getChecksum();
        if (this$checksum == null ? other$checksum != null : !this$checksum.equals(other$checksum)) return false;
        final Object this$creationDate = this.getCreationDate();
        final Object other$creationDate = other.getCreationDate();
        if (this$creationDate == null ? other$creationDate != null : !this$creationDate.equals(other$creationDate)) return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final Object other) {
        return other instanceof DatasetFileDetails;
    }

    @Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getId();
        result = result * PRIME + this.getFilesize();
        final Object $persistentId = this.getPersistentId();
        result = result * PRIME + ($persistentId == null ? 43 : $persistentId.hashCode());
        final Object $pidURL = this.getPidURL();
        result = result * PRIME + ($pidURL == null ? 43 : $pidURL.hashCode());
        final Object $filename = this.getFilename();
        result = result * PRIME + ($filename == null ? 43 : $filename.hashCode());
        final Object $contentType = this.getContentType();
        result = result * PRIME + ($contentType == null ? 43 : $contentType.hashCode());
        final Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final Object $storageIdentifier = this.getStorageIdentifier();
        result = result * PRIME + ($storageIdentifier == null ? 43 : $storageIdentifier.hashCode());
        final Object $rootDataFileId = this.getRootDataFileId();
        result = result * PRIME + ($rootDataFileId == null ? 43 : $rootDataFileId.hashCode());
        final Object $md5 = this.getMd5();
        result = result * PRIME + ($md5 == null ? 43 : $md5.hashCode());
        final Object $checksum = this.getChecksum();
        result = result * PRIME + ($checksum == null ? 43 : $checksum.hashCode());
        final Object $creationDate = this.getCreationDate();
        result = result * PRIME + ($creationDate == null ? 43 : $creationDate.hashCode());
        return result;
    }

    @Override
    @SuppressWarnings("all")
    public String toString() {
        return "DatasetFileDetails(id=" + this.getId() + ", filesize=" + this.getFilesize() + ", persistentId=" + this.getPersistentId() + ", pidURL=" + this.getPidURL() + ", filename=" + this.getFilename() + ", contentType=" + this.getContentType() + ", description=" + this.getDescription() + ", storageIdentifier=" + this.getStorageIdentifier() + ", rootDataFileId=" + this.getRootDataFileId() + ", md5=" + this.getMd5() + ", checksum=" + this.getChecksum() + ", creationDate=" + this.getCreationDate() + ")";
    }
}
