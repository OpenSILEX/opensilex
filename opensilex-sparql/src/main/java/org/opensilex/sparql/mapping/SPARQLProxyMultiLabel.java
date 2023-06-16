package org.opensilex.sparql.mapping;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.opensilex.sparql.model.SPARQLMultiLabels;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.List;
import java.util.Map;

public class SPARQLProxyMultiLabel extends SPARQLProxy<SPARQLMultiLabels> {

    private final URI resourceURI;
    private final Property labelProperty;
    private final boolean reverseRelation;

    public SPARQLProxyMultiLabel(SPARQLClassObjectMapperIndex mapperIndex, Node graph, String lang, SPARQLService service, URI resourceURI, Property labelProperty, boolean reverseRelation) {
        super(mapperIndex, graph, SPARQLMultiLabels.class, lang, service);
        this.resourceURI = resourceURI;
        this.labelProperty = labelProperty;
        this.reverseRelation = reverseRelation;
    }

    @Override
    protected SPARQLMultiLabels loadData() throws Exception {
        Map<String, List<String>> altLabelstranslations = service.getAltLabelsTranslations(graph, resourceURI, labelProperty, reverseRelation);

        SPARQLMultiLabels multiLabels = new SPARQLMultiLabels();
        multiLabels.setTranslationsOfAltLabels(altLabelstranslations);
        return multiLabels;
    }
}
