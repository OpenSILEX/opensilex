package org.opensilex.brapi.model;

/**
 * @see Brapi documentation V2.1 https://app.swaggerhub.com/apis/PlantBreedingAPI/BrAPI-Core/2.1
 * @author bernhard.gschloessl
 */
public class DataLinkDTO extends DataLink {
    private String dataFormat; //v2.1 : The structure of the data within a file. For example - VCF, table, image archive, multispectral image archives in EDAM ontology (used in Galaxy) MIAPPE V1.1 (DM-38) Data file description
    private String description; //v2.1 : The general description of this data link MIAPPE V1.1 (DM-38) Data file description
    private String fileFormat; //v2.1 : The MIME type of the file (ie text/csv, application/excel, application/zip). MIAPPE V1.1 (DM-38) Data file description
    private String provenance; //v2.1 : The description of the origin or ownership of this linked data.
    private String scientificType; //v2.1 : The general type of data. For example- Genotyping, Phenotyping raw data, Phenotyping reduced data, Environmental, etc
    private String version; //v2.1 : The version number for this data MIAPPE V1.1 (DM-39) Data file version

    public String getDataFormat() {
        return dataFormat;
    }

    public void setDataFormat(String dataFormat) {
        this.dataFormat = dataFormat;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public String getProvenance() {
        return provenance;
    }

    public void setProvenance(String provenance) {
        this.provenance = provenance;
    }

    public String getScientificType() {
        return scientificType;
    }

    public void setScientificType(String scientificType) {
        this.scientificType = scientificType;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
