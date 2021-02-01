//******************************************************************************
//                          DocumentGetDTO.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: renaud.colin@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.document.api;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.opensilex.core.document.dal.DocumentModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.fs.service.FileStorageService;

/**
 *
 * A basic GetDTO which extends the {@link DocumentDTO} and which add the conversion from an {@link DocumentModel} to a {@link DocumentGetDTO}
 *
 * @author Emilie Fernandez
 */
public class DocumentGetDTO extends DocumentDTO {

    public static DocumentGetDTO fromModel(DocumentModel model) {

        DocumentGetDTO dto = new DocumentGetDTO();
        dto.setUri(model.getUri())
                .setIdentifier(model.getIdentifier())
                .setType(model.getType())
                .setTypeLabel(model.getTypeLabel().getDefaultValue())
                .setTitle(model.getTitle())
                .setDate(model.getDate())
                .setTargets(model.getTargets())
                .setAuthors(model.getAuthors())
                .setLanguage(model.getLanguage())
                .setFormat(model.getFormat())
                .setDescription(model.getDescription())
                .setSubject(model.getSubject())
                .setDeprecated(Boolean.parseBoolean(model.getDeprecated()));

        return dto;
    }
}
