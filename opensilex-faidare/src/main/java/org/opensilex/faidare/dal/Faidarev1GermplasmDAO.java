package org.opensilex.faidare.dal;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.faidare.model.Faidarev1GermplasmModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

public class Faidarev1GermplasmDAO extends GermplasmDAO {
    public Faidarev1GermplasmDAO(SPARQLService sparql, MongoDBService nosql) {
        super(sparql, nosql);
    }

    public ListWithPagination<Faidarev1GermplasmModel> faidareSearch(AccountModel user, URI germplasmDbId, String germplasmName, int page, int pageSize) throws Exception {

        /*SparqlNoProxyFetcher<Faidarev1GermplasmModel> customFetcher = new SparqlNoProxyFetcher<>(Faidarev1GermplasmModel.class, sparql);

        ListWithPagination<Faidarev1GermplasmModel> models = sparql.searchWithPagination(
                sparql.getDefaultGraph(Faidarev1GermplasmModel.class),
                Faidarev1GermplasmModel.class,
                user.getLanguage(),
                (SelectBuilder select) -> {
                    appendRdfTypeFilter(select, new URI(Oeso.Accession.toString()));
                    appendUriFilter(select, germplasmDbId);
                    appendRegexLabelFilter(select, germplasmName);
                },
                Collections.emptyMap(),
                result -> customFetcher.getInstance(result, user.getLanguage()),
                null,
                page,
                pageSize
        );*/

        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);

        Set<URI> userExperiments = experimentDAO.getUserExperiments(user);

        final Node germplasmGraph = sparql.getDefaultGraph(Faidarev1GermplasmModel.class);
        SelectBuilder accessions = new SelectBuilder();

        Var uriVar = makeVar("uri");
        accessions.addVar(uriVar);
        Var labelVar = makeVar("label");
        accessions.addVar(labelVar);
        Var websiteVar = makeVar("website");
        accessions.addVar(websiteVar);
        Var codeVar = makeVar("code");
        accessions.addVar(codeVar);
        Var instituteVar = makeVar("institute");
        accessions.addVar(instituteVar);
        Var speciesVar = makeVar("species");
        accessions.addVar(speciesVar);
        Var varietyVar = makeVar("variety");
        accessions.addVar(varietyVar);
        Var varietyNameVar = makeVar("variety_name");
        accessions.addVar(varietyNameVar);
        Var experimentUriVar = makeVar("experiment_uri");
        Var soVar = makeVar("so");

        accessions.addWhere(uriVar, RDF.type.asNode(), Oeso.Accession.asNode());
        accessions.addGraph(germplasmGraph, new WhereBuilder()
                .addOptional(uriVar, FOAF.homepage.asNode(), websiteVar)
                .addOptional(uriVar, Oeso.hasId.asNode(), codeVar)
                .addOptional(uriVar, Oeso.fromInstitute.asNode(), instituteVar)
                .addOptional(uriVar, Oeso.fromSpecies.asNode(), speciesVar)
                .addOptional(uriVar, Oeso.fromVariety.asNode(), varietyVar)
                .addOptional(varietyVar, RDFS.label.asNode(), varietyNameVar)
        );

        accessions.addGraph(experimentUriVar, new WhereBuilder().addOptional(soVar, Oeso.hasGermplasm.asNode(), uriVar));

        accessions.addGraph(germplasmGraph, uriVar, RDFS.label.asNode(), labelVar);

        SPARQLQueryHelper.addWhereUriValues(accessions, experimentUriVar.getVarName(), userExperiments);
        SPARQLQueryHelper.appendGroupConcatAggregator(accessions, experimentUriVar, true);

        accessions.addGroupBy(uriVar)
                .addGroupBy(labelVar)
                .addGroupBy(websiteVar)
                .addGroupBy(codeVar)
                .addGroupBy(instituteVar)
                .addGroupBy(speciesVar)
                .addGroupBy(varietyVar)
                .addGroupBy(varietyNameVar);

        if (Objects.nonNull(germplasmDbId)) {
            SPARQLQueryHelper.addWhereUriValues(accessions, uriVar.getVarName(), Collections.singletonList(germplasmDbId));
        }

        if (Objects.nonNull(germplasmName)) {
            SPARQLQueryHelper.addWhereValues(accessions, labelVar.getVarName(), Collections.singletonList(germplasmName));
        }

        accessions.setLimit(pageSize);
        accessions.setOffset(page*pageSize);

        List<Faidarev1GermplasmModel> searchResults =  sparql.executeSelectQueryAsStream(accessions).map(
                row -> {
                    try {
                        Faidarev1GermplasmModel model = new Faidarev1GermplasmModel()
                                .setLabel(new SPARQLLabel(row.getStringValue(labelVar.getVarName()), ""))
                                .setCode(row.getStringValue(codeVar.getVarName()))
                                .setVarietyName(row.getStringValue(varietyNameVar.getVarName()))
                                .setInstitute(row.getStringValue(instituteVar.getVarName()));
                        model.setUri(new URI(row.getStringValue(uriVar.getVarName())));

                        if (Objects.nonNull(row.getStringValue(websiteVar.getVarName()))) {
                            model.setWebsite(new URI(row.getStringValue(websiteVar.getVarName())));
                        }

                        if (Objects.nonNull(row.getStringValue(speciesVar.getVarName()))) {
                            model.setSpecies(new URI(row.getStringValue(speciesVar.getVarName())));
                        }

                        if (Objects.nonNull(row.getStringValue(varietyVar.getVarName()))) {
                            model.setVariety(new URI(row.getStringValue(varietyVar.getVarName())));
                        }

                        // get experiments where accession is used
                        String joiningColumn = row.getStringValue(SPARQLQueryHelper.getConcatVarName(experimentUriVar.getVarName()));
                        String[] experiments = joiningColumn.split(SPARQLQueryHelper.GROUP_CONCAT_SEPARATOR);

                        model.setExperiments(Arrays.stream(experiments).map(xp -> {
                            try {
                                return new URI(xp);
                            } catch (URISyntaxException e) {
                                throw new RuntimeException(e);
                            }
                        }).collect(Collectors.toList()));
                        return model;
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
        ).collect(Collectors.toList());

        return new ListWithPagination<>(searchResults, page, pageSize, true); // TODO : change "true"
    }
}
