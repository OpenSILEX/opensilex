package org.opensilex.core.dataV2.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.core.dataV2.model.BatchHistoryModel;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;


/**
 * @author MKourdi
 */
public class BatchHistoryGetDTO {
    @JsonProperty("username")
    private String userName;
    @JsonProperty("batch_id")
    private String batchId;
    @JsonProperty("issued")
    private OffsetDateTime publicationDate;
    @JsonProperty("publisher")
    private URI publisher;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

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

    public static BatchHistoryGetDTO fromModel(BatchHistoryModel batchHistoryModel) {
        BatchHistoryGetDTO batchHistoryDTO = new BatchHistoryGetDTO();
        batchHistoryDTO.setBatchId(batchHistoryModel.getBatchId());
        batchHistoryDTO.setUserName(batchHistoryModel.getUsername());
        batchHistoryDTO.setPublisher(batchHistoryModel.getPublisher());
        batchHistoryDTO.setPublicationDate(OffsetDateTime.ofInstant(batchHistoryModel.getPublicationDate(), ZoneOffset.UTC));
        return batchHistoryDTO;
    }
}
