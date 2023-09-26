//******************************************************************************
//                          DataFileGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: alice.boizet@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.data.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import org.opensilex.core.data.dal.DataFileModel;
import org.opensilex.security.user.api.UserGetDTO;
import org.opensilex.server.rest.validation.DateFormat;
import org.opensilex.server.rest.validation.ValidURI;

/**
 *
 * @author Alice Boizet
 */
public class DataFileGetDTO extends DataFileCreationDTO {
    @NotNull
    @ValidURI
    @Override
    public URI getUri() {
        return uri;
    }

    private String filename;

    @JsonProperty("publisher")
    protected UserGetDTO publisher;

    @JsonProperty("issued")
    protected Instant publicationDate;

    public UserGetDTO getPublisher() {
        return publisher;
    }

    public void setPublisher(UserGetDTO publisher) {
        this.publisher = publisher;
    }

    public Instant getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(Instant publicationDate) {
        this.publicationDate = publicationDate;
    }

    public void setDate(Instant instant, String offset, Boolean isDateTime) {
        if (isDateTime) {
            OffsetDateTime odt = instant.atOffset(ZoneOffset.of(offset));
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(DateFormat.YMDTHMSMSX.toString());
            this.setDate(dtf.format(odt));
        } else {
            LocalDate date = ZonedDateTime.ofInstant(instant, ZoneId.of(offset)).toLocalDate();           
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(DateFormat.YMD.toString());            ;
            this.setDate(dtf.format(date));
        }        
    }
    
    public static DataFileGetDTO fromModel(DataFileModel model){
        DataFileGetDTO dto = new DataFileGetDTO();        
        dto.setUri(model.getUri());
        dto.setRdfType(model.getRdfType());
        dto.setTarget(model.getTarget());
        dto.setDate(model.getDate(), model.getOffset(), model.getIsDateTime());        
        dto.setMetadata(model.getMetadata());   
        dto.setProvenance(model.getProvenance());
        dto.setArchive(model.getArchive());
        dto.setFilename(model.getFilename());
        if (Objects.nonNull(model.getPublicationDate())) {
            dto.setPublicationDate(model.getPublicationDate());
        }

        return dto;
    }
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

}
