// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
package com.researchspace.dataverse.entities;

import com.researchspace.dataverse.http.FileUploadMetadata;
import java.util.List;

/**
 * DatasetFile is part of the response from
 * {@link com.researchspace.dataverse.api.v1.DatasetOperations#uploadNativeFile(byte[], FileUploadMetadata, Identifier, String)}
 */
public class DatasetFile {
    private String description;
    private String label;
    private String directoryLabel;
    private String datasetVersionId;
    private List<String> categories;
    private boolean restricted;
    private int version;
    private DatasetFileDetails dataFile;

    @SuppressWarnings("all")
    public DatasetFile() {
    }

    @SuppressWarnings("all")
    public String getDescription() {
        return this.description;
    }

    @SuppressWarnings("all")
    public String getLabel() {
        return this.label;
    }

    @SuppressWarnings("all")
    public String getDirectoryLabel() {
        return this.directoryLabel;
    }

    @SuppressWarnings("all")
    public String getDatasetVersionId() {
        return this.datasetVersionId;
    }

    @SuppressWarnings("all")
    public List<String> getCategories() {
        return this.categories;
    }

    @SuppressWarnings("all")
    public boolean isRestricted() {
        return this.restricted;
    }

    @SuppressWarnings("all")
    public int getVersion() {
        return this.version;
    }

    @SuppressWarnings("all")
    public DatasetFileDetails getDataFile() {
        return this.dataFile;
    }

    @SuppressWarnings("all")
    public void setDescription(final String description) {
        this.description = description;
    }

    @SuppressWarnings("all")
    public void setLabel(final String label) {
        this.label = label;
    }

    @SuppressWarnings("all")
    public void setDirectoryLabel(final String directoryLabel) {
        this.directoryLabel = directoryLabel;
    }

    @SuppressWarnings("all")
    public void setDatasetVersionId(final String datasetVersionId) {
        this.datasetVersionId = datasetVersionId;
    }

    @SuppressWarnings("all")
    public void setCategories(final List<String> categories) {
        this.categories = categories;
    }

    @SuppressWarnings("all")
    public void setRestricted(final boolean restricted) {
        this.restricted = restricted;
    }

    @SuppressWarnings("all")
    public void setVersion(final int version) {
        this.version = version;
    }

    @SuppressWarnings("all")
    public void setDataFile(final DatasetFileDetails dataFile) {
        this.dataFile = dataFile;
    }

    @Override
    @SuppressWarnings("all")
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof DatasetFile)) return false;
        final DatasetFile other = (DatasetFile) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.isRestricted() != other.isRestricted()) return false;
        if (this.getVersion() != other.getVersion()) return false;
        final Object this$description = this.getDescription();
        final Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final Object this$label = this.getLabel();
        final Object other$label = other.getLabel();
        if (this$label == null ? other$label != null : !this$label.equals(other$label)) return false;
        final Object this$directoryLabel = this.getDirectoryLabel();
        final Object other$directoryLabel = other.getDirectoryLabel();
        if (this$directoryLabel == null ? other$directoryLabel != null : !this$directoryLabel.equals(other$directoryLabel)) return false;
        final Object this$datasetVersionId = this.getDatasetVersionId();
        final Object other$datasetVersionId = other.getDatasetVersionId();
        if (this$datasetVersionId == null ? other$datasetVersionId != null : !this$datasetVersionId.equals(other$datasetVersionId)) return false;
        final Object this$categories = this.getCategories();
        final Object other$categories = other.getCategories();
        if (this$categories == null ? other$categories != null : !this$categories.equals(other$categories)) return false;
        final Object this$dataFile = this.getDataFile();
        final Object other$dataFile = other.getDataFile();
        if (this$dataFile == null ? other$dataFile != null : !this$dataFile.equals(other$dataFile)) return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final Object other) {
        return other instanceof DatasetFile;
    }

    @Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isRestricted() ? 79 : 97);
        result = result * PRIME + this.getVersion();
        final Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final Object $label = this.getLabel();
        result = result * PRIME + ($label == null ? 43 : $label.hashCode());
        final Object $directoryLabel = this.getDirectoryLabel();
        result = result * PRIME + ($directoryLabel == null ? 43 : $directoryLabel.hashCode());
        final Object $datasetVersionId = this.getDatasetVersionId();
        result = result * PRIME + ($datasetVersionId == null ? 43 : $datasetVersionId.hashCode());
        final Object $categories = this.getCategories();
        result = result * PRIME + ($categories == null ? 43 : $categories.hashCode());
        final Object $dataFile = this.getDataFile();
        result = result * PRIME + ($dataFile == null ? 43 : $dataFile.hashCode());
        return result;
    }

    @Override
    @SuppressWarnings("all")
    public String toString() {
        return "DatasetFile(description=" + this.getDescription() + ", label=" + this.getLabel() + ", directoryLabel=" + this.getDirectoryLabel() + ", datasetVersionId=" + this.getDatasetVersionId() + ", categories=" + this.getCategories() + ", restricted=" + this.isRestricted() + ", version=" + this.getVersion() + ", dataFile=" + this.getDataFile() + ")";
    }
}
