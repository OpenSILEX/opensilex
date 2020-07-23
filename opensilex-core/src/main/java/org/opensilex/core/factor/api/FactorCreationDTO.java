/*
 * ******************************************************************************
 *                                     FactorCreationDTO.java
 *  OpenSILEX
 *  Copyright © INRA 2019
 *  Creation date:  17 December, 2019
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.factor.api;

import java.net.URI;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.opensilex.OpenSilex;
import org.opensilex.core.factor.dal.FactorModel;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.server.rest.validation.Required;
import org.opensilex.server.rest.validation.ValidTranslationMap;
import org.opensilex.server.rest.validation.ValidURI;
import org.opensilex.sparql.model.SPARQLLabel;

/**
 *
 * @author Arnaud Charleroy
 */
public class FactorCreationDTO extends SKOSReferencesDTO {

    @ValidURI
    private URI uri;

    @ValidTranslationMap
    private Map<String, String> names;

    private String comment;

    @Valid
    private List<FactorLevelCreationDTO> factorLevels;

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public Map<String, String> getNames() {
        return names;
    }

    public void setNames(Map<String, String> name) {
        this.names = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<FactorLevelCreationDTO> getFactorLevels() {
        return factorLevels;
    }

    public void setFactorLevels(List<FactorLevelCreationDTO> factorsLevels) {
        this.factorLevels = factorsLevels;
    }

    public FactorModel newModel() {
        FactorModel model = new FactorModel();
        model.setUri(getUri());
        SPARQLLabel sparqlLabel = new SPARQLLabel();
        sparqlLabel.setTranslations(names);
        sparqlLabel.setDefaultValue(names.get(OpenSilex.DEFAULT_LANGUAGE));
        sparqlLabel.setDefaultLang(OpenSilex.DEFAULT_LANGUAGE);
        model.setName(sparqlLabel);
        model.setComment(getComment());
        this.setSkosReferencesToModel(model);
       
        return model;
    }
}
