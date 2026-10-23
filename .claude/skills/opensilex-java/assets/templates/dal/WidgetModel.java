package org.opensilex.core.widget.dal;

import org.apache.jena.vocabulary.DCTerms;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;

import java.time.LocalDate;

/**
 * A widget.
 * <p>
 * Every mapped property must exist in the ontology (OWL file of the module + a constant in {@link Oeso}), otherwise the
 * SHACL validation rejects the instance when it is created. Every field needs a getter and a setter: the mapper and
 * the lazy proxies go through accessors.
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "Widget",
        graph = WidgetModel.GRAPH,
        prefix = "wdg"
)
public class WidgetModel extends SPARQLNamedResourceModel<WidgetModel> {

    public static final String GRAPH = "widget";

    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "description"
    )
    private String description;
    public static final String DESCRIPTION_FIELD = "description";

    @SPARQLProperty(
            ontology = Oeso.class,
            property = "startDate"
    )
    private LocalDate startDate;
    public static final String START_DATE_FIELD = "startDate";

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
}
