package org.opensilex.core.data.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.core.data.dal.batchHistory.BatchHistoryModel;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;


/**
 * @author MKourdi
 */
public class BatchHistoryGetDTO {
    @JsonProperty("issued")
    private OffsetDateTime publicationDate;
    @JsonProperty("publisher")
    private URI publisher;
    @JsonProperty("uri")
    private URI uri;
    @JsonProperty("documentUri")
    private URI documentUri;

    public OffsetDateTime getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(OffsetDateTime publicationDate) {
        this.publicationDate = publicationDate;
    }

    public URI getPublisher() {
        return publisher;
    }

    public void setPublisher(URI publisher) {
        this.publisher = publisher;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getDocumentUri() {
        return documentUri;
    }

    public void setDocumentUri(URI documentUri) {
        this.documentUri = documentUri;
    }

    public static BatchHistoryGetDTO fromModel(BatchHistoryModel batchHistoryModel) {
        BatchHistoryGetDTO batchHistoryDTO = new BatchHistoryGetDTO();
        batchHistoryDTO.setPublisher(batchHistoryModel.getPublisher());
        batchHistoryDTO.setUri(batchHistoryModel.getUri());
        batchHistoryDTO.setPublicationDate(OffsetDateTime.ofInstant(batchHistoryModel.getPublicationDate(), ZoneOffset.UTC));
        batchHistoryDTO.setDocumentUri(batchHistoryModel.getDocumentUri());
        return batchHistoryDTO;
    }


}
