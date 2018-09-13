//******************************************************************************
//                                       FileMetadataUAVDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 4 sept. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the JSON return by the web service used to generate the acquisition 
 * session excel file for the 4P platform, specific to the uav.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class MetadataFileUAVDTO extends MetadataFileDTO {
    //The camera type, associated to cameraAlias and cameraUri.
    //e.g. http://www.phenome-fppn.fr/vocabulary/2017#RGBCamera
    @SerializedName("Camera_type")
    protected String cameraType;
    //The camera alias, associated to cameraType and cameraUri.
    //e.g. RGB001_a
    @SerializedName("Camera_alias")
    protected String cameraAlias;
    //The camera uri, associated to cameraType and cameraUri.
    //e.g. http://www.phenome-fppn.fr/diaphen/s0001
    @SerializedName("Camera_uri")
    protected String cameraUri;
    //The vector type, associated to vectorAlias and vectorUri.
    //It must be a subtype of UAV.
    //e.g. http://www.phenome-fppn.fr/vocabulary/2017#UAV
    @SerializedName("Vector_type")
    protected String vectorType;
    //The vector alias, associated to vectorType and vectorUri.
    //e.g. UAV-hex-001
    @SerializedName("Vector_alias")
    protected String vectorAlias;
    //The vector uri, associated to vectorType and vectorAlias.
    //e.g. http://www.phenome-fppn.fr/diaphen/v01
    @SerializedName("Vector_uri")
    protected String vectorUri;
    //The radiometric target uri, associated to radiometricTargetAlias.
    //e.g. http://www.phenome-fppn.fr/diaphen/rt0003
    @SerializedName("RadiometricTarget_uri")
    protected String radiometricTargetUri;
    //The radiometric target alias, associated to radiometricTargetUri.
    //e.g. RD001
    @SerializedName("RadiometricTarget_alias")
    protected String radiometricTargetAlias;

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
