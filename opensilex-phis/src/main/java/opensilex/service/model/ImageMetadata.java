//******************************************************************************
//                               ImageMetadata.java 
// PHIS-SILEX
// Copyright © INRA 2017
// Creation date: 11 December 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import java.util.ArrayList;

/**
 * Image metadata model.
 * @author Morgane Vidal <morgane.vidal@inra.fr>
 * @update [Andréas Garcia] Jan. 2019: modify "concern(s)" occurences into 
 * "concernedItem(s)"
 */
public class ImageMetadata {
    
    /**
     * Image URI.
     */
    private String uri;
    
    /**
     * Image type (in the ontology).
     */
    private String rdfType;
    
    /**
     * Elements concerned by the image.
     */
    private ArrayList<ConcernedItem> concernedItems = new ArrayList<>();
    
    /**
     * Shooting configuration.
     */
    private ShootingConfiguration configuration;
    
    /** 
     * Information about the image file.
     */
    private FileInformations fileInformations;

    public ImageMetadata() {
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public ArrayList<ConcernedItem> getConcernedItems() {
        return concernedItems;
    }

    public void setConcernedItems(ArrayList<ConcernedItem> concernedItems) {
        this.concernedItems = concernedItems;
    }
    
    public void addConcernedItem(ConcernedItem concernedItem) {
        this.concernedItems.add(concernedItem);
    }

    public ShootingConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(ShootingConfiguration configuration) {
        this.configuration = configuration;
    }

    public FileInformations getFileInformations() {
        return fileInformations;
    }

    public void setFileInformations(FileInformations fileInformations) {
        this.fileInformations = fileInformations;
    }

    public String getRdfType() {
        return rdfType;
    }

    public void setRdfType(String rdfType) {
        this.rdfType = rdfType;
    }
}
