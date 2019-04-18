//******************************************************************************
//                           FileMetadataUAVDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 4 Sept. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.acquisitionSession;

import com.google.gson.annotations.SerializedName;

/**
 * Metadata file DTO specific to UAV.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class MetadataFileUAVDTO extends MetadataFileDTO {
    
    /**
     * The current installation URI. 
     * @example http://www.phenome-fppn.fr/diaphen
     */
    @SerializedName("Installation")
    protected String installation;
    
    /**
     * The group plot type, associated to groupPlotAlias, groupPlotUri, groupPlotSpecies
     * @example http://www.opensilex.org/vocabulary/oeso#Experiment
     */
    @SerializedName("GroupPlot_type")
    protected String groupPlotType;
    
    /**
     * The alias of the group plot, associated to groupPlotType, groupPlotUri, groupPlotSpecies 
     * @example BBlee_2018
     */
    @SerializedName("GroupPlot_alias")
    protected String groupPlotAlias;
    
    /**
     * The group plot uri, associated to groupPlotType, grouPlotAlias, groupPlotSpecies
     * @examplehttp://www.phenome-fppn.fr/diaphen/DIA2017-2
     */
    @SerializedName("GroupPlot_uri")
    protected String groupPlotUri;
    
    /**
     * The species of the group plot, associated to groupPlotType, groupPlotUri, groupPlotAlias
     * @example Maize
     */
    @SerializedName("GroupPlot_species")
    protected String groupPlotSpecies;
    
    /**
     * A pilot email
     * @example john.doe@email.fr
     */
    @SerializedName("Pilot")
    protected String pilot;
    
    /**
     * The camera type, associated to cameraAlias and cameraUri.
     * @example http://www.opensilex.org/vocabulary/oeso#RGBCamera
     */
    @SerializedName("Camera_type")
    protected String cameraType;
    
    /**
     * The camera alias, associated to cameraType and cameraUri.
     * @example RGB001_a
     */
    @SerializedName("Camera_alias")
    protected String cameraAlias;
    
    /***
     * The camera URI, associated to cameraType and cameraUri.
     * @example http://www.phenome-fppn.fr/diaphen/s0001
     */
    @SerializedName("Camera_uri")
    protected String cameraUri;
    
    /**
     * The vector type, associated to vectorAlias and vectorUri. 
     * It must be a subtype of UAV.
     * @example http://www.opensilex.org/vocabulary/oeso#UAV
     */
    @SerializedName("Vector_type")
    protected String vectorType;
    
    /**
     * The vector alias, associated to vectorType and vectorUri.
     * @example UAV-hex-001
     */
    @SerializedName("Vector_alias")
    protected String vectorAlias;
    
    /**
     * The vector uri, associated to vectorType and vectorAlias.
     * @example http://www.phenome-fppn.fr/diaphen/v01
     */
    @SerializedName("Vector_uri")
    protected String vectorUri;
    
    /**
     * The radiometric target alias, associated to radiometricTargetUri.
     * @example RD001
     */
    @SerializedName("RadiometricTarget_alias")
    protected String radiometricTargetAlias;
    
    /**
     * The radiometric target URI, associated to radiometricTargetAlias.
     * @example http://www.phenome-fppn.fr/diaphen/rt0003
     */
    @SerializedName("RadiometricTarget_uri")
    protected String radiometricTargetUri;
    
    public MetadataFileUAVDTO() {
        super();
    }
    
    public String getInstallation() {
        return installation;
    }

    public void setInstallation(String installation) {
        this.installation = installation;
    }

    public String getGroupPlotType() {
        return groupPlotType;
    }

    public void setGroupPlotType(String groupPlotType) {
        this.groupPlotType = groupPlotType;
    }

    public String getGroupPlotUri() {
        return groupPlotUri;
    }

    public void setGroupPlotUri(String groupPlotUri) {
        this.groupPlotUri = groupPlotUri;
    }

    public String getGroupPlotAlias() {
        return groupPlotAlias;
    }

    public void setGroupPlotAlias(String groupPlotAlias) {
        this.groupPlotAlias = groupPlotAlias;
    }

    public String getGroupPlotSpecies() {
        return groupPlotSpecies;
    }

    public void setGroupPlotSpecies(String groupPlotSpecies) {
        this.groupPlotSpecies = groupPlotSpecies;
    }

    public String getPilot() {
        return pilot;
    }

    public void setPilot(String pilot) {
        this.pilot = pilot;
    }

    public String getCameraType() {
        return cameraType;
    }

    public void setCameraType(String cameraType) {
        this.cameraType = cameraType;
    }

    public String getCameraAlias() {
        return cameraAlias;
    }

    public void setCameraAlias(String cameraAlias) {
        this.cameraAlias = cameraAlias;
    }

    public String getCameraUri() {
        return cameraUri;
    }

    public void setCameraUri(String cameraUri) {
        this.cameraUri = cameraUri;
    }

    public String getVectorType() {
        return vectorType;
    }

    public void setVectorType(String vectorType) {
        this.vectorType = vectorType;
    }

    public String getVectorAlias() {
        return vectorAlias;
    }

    public void setVectorAlias(String vectorAlias) {
        this.vectorAlias = vectorAlias;
    }

    public String getVectorUri() {
        return vectorUri;
    }

    public void setVectorUri(String vectorUri) {
        this.vectorUri = vectorUri;
    }

    public String getRadiometricTargetUri() {
        return radiometricTargetUri;
    }

    public void setRadiometricTargetUri(String radiometricTargetUri) {
        this.radiometricTargetUri = radiometricTargetUri;
    }

    public String getRadiometricTargetAlias() {
        return radiometricTargetAlias;
    }

    public void setRadiometricTargetAlias(String radiometricTargetAlias) {
        this.radiometricTargetAlias = radiometricTargetAlias;
    }
}
