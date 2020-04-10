//******************************************************************************
//                         FileMetadataPhenomobileDTO.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 30 Aug. 2018
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.resource.dto.acquisitionSession;

import com.google.gson.annotations.SerializedName;

/**
 * Metadata file DTO specific to Phenomobile.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 */
public class MetadataFilePhenomobileDTO extends MetadataFileDTO {
    
    /**
     * The current installation URI. 
     * @example http://www.phenome-fppn.fr/diaphen
     */
    @SerializedName("Installation")
    protected String installation;
    
    /**
     * The group plot type, associated to groupPlotAlias, groupPlotUri, groupPlotSpecies.
     * @example http://www.opensilex.org/vocabulary/oeso#Experiment
     * */
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
     * @example http://www.phenome-fppn.fr/diaphen/DIA2017-2
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

    @Override
    public Object createObjectFromDTO() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
}
