package org.opensilex.faidare.model;

public class Faidarev1DatasetAuthorshipDTO {
    private String license;
    private String datasetPUI;

    public String getLicense() {
        return license;
    }

    public Faidarev1DatasetAuthorshipDTO setLicense(String license) {
        this.license = license;
        return this;
    }

    public String getDatasetPUI() {
        return datasetPUI;
    }

    public Faidarev1DatasetAuthorshipDTO setDatasetPUI(String datasetPUI) {
        this.datasetPUI = datasetPUI;
        return this;
    }
}
