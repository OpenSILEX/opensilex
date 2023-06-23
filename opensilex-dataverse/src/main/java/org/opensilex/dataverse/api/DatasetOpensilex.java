package org.opensilex.dataverse.api;

import com.researchspace.dataverse.entities.Dataset;

public class DatasetOpensilex extends Dataset {

    private String metadataLanguage;
    public String getMetadataLanguage() {
        return this.metadataLanguage;
    }

    public void setMetadataLanguage(String metadataLanguage) {
        this.metadataLanguage = metadataLanguage;
    }

    @Override
    public String toString() {
        return "Dataset(metadataLanguage=" + this.getMetadataLanguage() + ", datasetVersion=" + this.getDatasetVersion() + ", latestVersion=" + this.getLatestVersion() + ", id=" + this.getId() + ", identifier=" + this.getIdentifier() + ", protocol=" + this.getProtocol() + ", authority=" + this.getAuthority() + ", persistentUrl=" + this.getPersistentUrl() + ")";
    }
}
