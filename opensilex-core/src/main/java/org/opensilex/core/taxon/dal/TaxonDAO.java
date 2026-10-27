//******************************************************************************
//                          OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026.
//******************************************************************************
package org.opensilex.core.taxon.dal;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.util.*;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * Data access for taxa. No business rule here: see TaxonLogic.
 */
public class TaxonDAO {

    private final SPARQLService sparql;

    public TaxonDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public TaxonModel create(TaxonModel taxon) throws Exception {
        sparql.create(taxon);
        return taxon;
    }

    public TaxonModel update(TaxonModel taxon) throws Exception {
        sparql.update(taxon);
        return taxon;
    }

    public void delete(URI uri) throws Exception {
        sparql.delete(TaxonModel.class, uri);
    }

    public TaxonModel get(URI uri, String lang) throws Exception {
        return sparql.getByURI(TaxonModel.class, uri, lang);
    }

    public List<TaxonModel> getList(Collection<URI> uris, String lang) throws Exception {
        return sparql.getListByURIs(TaxonModel.class, uris, lang);
    }

    public ListWithPagination<TaxonModel> search(TaxonSearchFilter filter) throws Exception {
        return sparql.searchWithPagination(
                TaxonModel.class,
                filter.getLang(),
                select -> {
                    if (StringUtils.isNotBlank(filter.getName())) {
                        select.addFilter(SPARQLQueryHelper.regexFilter(TaxonModel.LABEL_FIELD, filter.getName()));
                    }
                    if (filter.getRdfTypes() != null && !filter.getRdfTypes().isEmpty()) {
                        select.addFilter(SPARQLQueryHelper.inURIFilter(SPARQLResourceModel.TYPE_FIELD, filter.getRdfTypes()));
                    }
                    if (filter.getKingdom() != null) {
                        select.addFilter(SPARQLQueryHelper.eq(TaxonModel.KINGDOM_FIELD, filter.getKingdom()));
                    }
                    if (StringUtils.isNotBlank(filter.getTaxonId())) {
                        // Multi-valued property: the value is matched with a triple pattern on the taxon URI
                        // (same technique as the synonym filter of GermplasmSparqlDAO)
                        select.addWhere(makeVar(SPARQLResourceModel.URI_FIELD), Oeso.hasTaxonId, NodeFactory.createLiteral(filter.getTaxonId()));
                    }
                },
                filter.getOrderByList(),
                filter.getPage(),
                filter.getPageSize()
        );
    }

    /**
     * @return the taxon having this identifier (with its prefix) among its identifiers, if any.
     */
    public Optional<TaxonModel> findByTaxonId(String taxonId, String lang) throws Exception {
        TaxonSearchFilter filter = new TaxonSearchFilter().setTaxonId(taxonId);
        filter.setPageSize(1).setLang(lang);
        return search(filter).getList().stream().findFirst();
    }

    /**
     * @return true if any resource, in any graph, points to this taxon (genetic resource, scientific object,
     * experiment, variable, species of a genus...).
     */
    public boolean isUsed(URI taxonUri) throws Exception {
        AskBuilder ask = new AskBuilder();
        ask.addGraph(makeVar("g"), new WhereBuilder().addWhere(makeVar("s"), makeVar("p"), SPARQLDeserializers.nodeURI(taxonUri)));
        return sparql.executeAskQuery(ask);
    }

    /**
     * @return the kingdoms declared in the ontology (closed list), with their label in the requested language.
     */
    public List<Kingdom> getKingdoms(String lang) throws Exception {
        Var kingdom = makeVar("kingdom");
        Var label = makeVar("label");
        SelectBuilder select = new SelectBuilder();
        select.addVar(kingdom).addVar(label);
        select.addGraph(makeVar("g"), new WhereBuilder()
                .addWhere(kingdom, RDF.type, Oeso.Kingdom)
                .addWhere(kingdom, RDFS.label, label));

        Map<String, String> labelByUri = new LinkedHashMap<>();
        Map<String, String> fallbackByUri = new LinkedHashMap<>();
        sparql.executeSelectQuery(select, result -> {
            String uri = result.getStringValue("kingdom");
            String value = result.getStringValue("label");
            String valueLang = result.getLiteralValue("label").map(l -> l.lang()).orElse("");
            fallbackByUri.putIfAbsent(uri, value);
            if (lang != null && lang.equalsIgnoreCase(valueLang)) {
                labelByUri.put(uri, value);
            }
        });

        List<Kingdom> kingdoms = new ArrayList<>();
        fallbackByUri.forEach((uri, fallback) ->
                kingdoms.add(new Kingdom(URI.create(uri), labelByUri.getOrDefault(uri, fallback))));
        kingdoms.sort(Comparator.comparing(Kingdom::label, String.CASE_INSENSITIVE_ORDER));
        return kingdoms;
    }

    public boolean isKingdom(URI uri, String lang) throws Exception {
        AskBuilder ask = new AskBuilder();
        ask.addGraph(makeVar("g"), new WhereBuilder()
                .addWhere(SPARQLDeserializers.nodeURI(uri), RDF.type, Oeso.Kingdom));
        return sparql.executeAskQuery(ask);
    }
}
