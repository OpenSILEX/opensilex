//******************************************************************************
//                               Document.java 
// SILEX-PHIS
// Copyright © INRA 2017
// Creation date: March 2017
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
//          pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.model;

import java.util.ArrayList;
import opensilex.service.resource.dto.ConcernedItemDTO;

/**
 * Document model.
 * @update [Andréas Garcia] 15 Jan. 2019: Replace "concern" occurences by 
 * "concernedItem"
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr> 
 */
public class Document {
    private String uri;
    private String documentType;
    private String creator;
    private String language;
    private String title;
    private String creationDate;
    private String format;
    private String comment;
    private ArrayList<ConcernedItemDTO> concernedItems = new ArrayList<>();
    private String status;

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public ArrayList<ConcernedItemDTO> getConcernedItems() {
        return concernedItems;
    }

    public void setConcernedItems(ArrayList<ConcernedItemDTO> concernedItems) {
        this.concernedItems = concernedItems;
    }
    
    public void addConcernedItem(ConcernedItemDTO concernedItem) {
        this.concernedItems.add(concernedItem);
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
    /**
     * Compares to another document. It compares attributes one by one.
     * @param document Document to compare
     * @return true if equal. False otherwise.
     */
    public boolean equals(Document document) {
        return this.uri.equals(document.uri)
            && this.documentType.equals(document.documentType)
            && this.creator.equals(document.creator)
            && this.language.equals(document.language)
            && this.title.equals(document.title)
            && this.creationDate.equals(document.creationDate)
            && this.format.equals(document.format)
            && this.status.equals(document.status);
    }
}
