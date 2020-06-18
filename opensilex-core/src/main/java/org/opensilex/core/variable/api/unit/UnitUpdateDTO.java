//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.core.variable.api.unit;

import java.net.URI;
import java.util.List;

import com.sun.mail.imap.protocol.MODSEQ;
import org.opensilex.core.ontology.OntologyReference;
import org.opensilex.core.ontology.SKOSReferencesDTO;
import org.opensilex.core.variable.dal.unit.UnitModel;
import org.opensilex.server.rest.validation.Required;

public class UnitUpdateDTO extends SKOSReferencesDTO {

    @Required
    protected String label;

    protected String comment;

    protected String symbol;

    protected String alternativeSymbol;

    private URI type;

    protected List<OntologyReference> relations;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getAlternativeSymbol() {
        return alternativeSymbol;
    }

    public void setAlternativeSymbol(String alternativeSymbol) {
        this.alternativeSymbol = alternativeSymbol;
    }

    public List<OntologyReference> getRelations() {
        return relations;
    }

    public void setRelations(List<OntologyReference> reference) {
        this.relations = reference;
    }

    public UnitModel newModel() {
        return defineModel(new UnitModel());
    }

    public URI getType() { return type; }

    public void setType(URI type) { this.type = type; }

    public UnitModel defineModel(UnitModel model) {
        model.setName(getLabel());
        model.setComment(getComment());
        model.setSymbol(getSymbol());
        model.setAlternativeSymbol(getAlternativeSymbol());
        setSkosReferencesToModel(model);
        if(type != null){
            model.setType(type);
        }
        return model;
    }
}
