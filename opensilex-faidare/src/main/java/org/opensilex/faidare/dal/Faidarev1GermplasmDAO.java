/*
 * *****************************************************************************
 *                         Faidarev1GermplasmDAO.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2024.
 * Last Modification: 19/04/2024 12:04
 * Contact: gabriel.besombes@inrae.fr
 * *****************************************************************************
 */

package org.opensilex.faidare.dal;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.aggregate.AggCountDistinct;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.experiment.dal.ExperimentDAO;
import org.opensilex.core.germplasm.dal.GermplasmDAO;
import org.opensilex.core.germplasm.dal.GermplasmModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.faidare.model.Faidarev1GermplasmModel;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.nosql.mongodb.service.v2.MongoDBServiceV2;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 * @author Gabriel Besombes
 * This class was added to answer specific optimization issues with FAIDARE services
 */
public class Faidarev1GermplasmDAO extends GermplasmDAO {
    public Faidarev1GermplasmDAO(SPARQLService sparql, MongoDBServiceV2 nosql) {
        super(sparql, nosql);
    }

    //@todo: remove nosql parameter when experimentDAO can be instanciated with MongoDBServiceV2
    public ListWithPagination<Faidarev1GermplasmModel> faidareSearch(AccountModel user, URI germplasmDbId, String germplasmName, int page, int pageSize, MongoDBService nosql) throws Exception {

        /* Count the number of accessions

        SELECT  (count(distinct *) AS ?count)
        WHERE
        { ?uri  rdf:type  vocabulary:Accession}*/

        SelectBuilder accessionsCount = new SelectBuilder();

        Var uriVar = makeVar("uri");
        Var countVar = makeVar("count");

        accessionsCount.addWhere(uriVar, RDF.type.asNode(), Oeso.Accession.asNode());

        accessionsCount.addVar(new AggCountDistinct().toString(), countVar);

        List<SPARQLResult> resultSet = sparql.executeSelectQuery(accessionsCount);

        int numberOfAccessions;
        if (resultSet.size() == 1) {
            numberOfAccessions = Integer.parseInt(resultSet.get(0).getStringValue("count"));
        } else {
            throw new SPARQLException("Invalid count query");
        }

        /* Get the user's experimentations

        SELECT DISTINCT  ?uri ?rdfType ?rdfTypeName ?endDate ?lastUpdateDate ?name ?description ?publisher ?isPublic ?publicationDate ?startDate ?objective
        WHERE
        { ?rdfType (rdfs:subClassOf)* vocabulary:Experiment
                OPTIONAL
            { ?rdfType  rdfs:label  ?rdfTypeName
                FILTER langMatches(lang(?rdfTypeName), "en")
            }
            OPTIONAL
            { ?rdfType  rdfs:label  ?rdfTypeName
                FILTER langMatches(lang(?rdfTypeName), "")
            }
            GRAPH <http://opensilex.test/set/experiment>
            { ?uri  rdf:type              ?rdfType ;
                vocabulary:startDate  ?startDate ;
                vocabulary:hasObjective  ?objective
                OPTIONAL
                { ?uri  vocabulary:endDate  ?endDate}
                OPTIONAL
                { ?uri  dc:modified  ?lastUpdateDate}
                OPTIONAL
                { ?uri  rdfs:label  ?name}
                OPTIONAL
                { ?uri  rdfs:comment  ?description}
                OPTIONAL
                { ?uri  dc:publisher  ?publisher}
                OPTIONAL
                { ?uri  vocabulary:isPublic  ?isPublic}
                OPTIONAL
                { ?uri  dc:issued  ?publicationDate}
            }
            FILTER ( ! isBlank(?uri) )
        }*/

        ExperimentDAO experimentDAO = new ExperimentDAO(sparql, nosql);
        Set<URI> userExperiments = experimentDAO.getUserExperiments(user);

        /* Get the accessions with the experimentations the user has access to that they are used in

        SELECT  ?uri ?label ?website ?code ?institute ?species ?variety ?variety_name (GROUP_CONCAT(DISTINCT ?experiment_uri ; separator=',') AS ?experiment_uri__opensilex__concat)
        WHERE
          { GRAPH <http://phenome.inrae.fr/diaphen/set/germplasm>
              { ?uri  rdf:type  vocabulary:Accession
                OPTIONAL
                  { ?uri  foaf:homepage  ?website}
                OPTIONAL
                  { ?uri  vocabulary:hasId  ?code}
                OPTIONAL
                  { ?uri  vocabulary:fromInstitute  ?institute}
                OPTIONAL
                  { ?uri  vocabulary:fromSpecies  ?species}
                OPTIONAL
                  { ?uri      vocabulary:fromVariety  ?variety .
                    ?variety  rdfs:label            ?variety_name}
                OPTIONAL
                  { GRAPH ?experiment_uri
                      { ?so  vocabulary:hasGermplasm  ?uri}}
                OPTIONAL
                  { GRAPH <http://phenome.inrae.fr/diaphen/set/germplasm>
                      { ?uri  rdfs:label  ?label}}
              }}
        GROUP BY ?uri ?label ?website ?code ?institute ?species ?variety ?variety_name
        */

        final Node germplasmGraph = sparql.getDefaultGraph(GermplasmModel.class);
        SelectBuilder accessions = new SelectBuilder();

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

        WhereBuilder experimentsWhere = new WhereBuilder().addGraph(experimentUriVar, soVar, Oeso.hasGermplasm.asNode(), uriVar);
        SPARQLQueryHelper.addWhereUriValues(experimentsWhere, experimentUriVar.getVarName(), userExperiments);

        accessions.addGraph(germplasmGraph, new WhereBuilder()
                .addWhere(uriVar, RDF.type.asNode(), Oeso.Accession.asNode())
                .addOptional(uriVar, FOAF.homepage.asNode(), websiteVar)
                .addOptional(uriVar, Oeso.hasId.asNode(), codeVar)
                .addOptional(uriVar, Oeso.fromInstitute.asNode(), instituteVar)
                .addOptional(uriVar, Oeso.fromSpecies.asNode(), speciesVar)
                .addOptional(
                        new WhereBuilder()
                                .addWhere(uriVar, Oeso.fromVariety.asNode(), varietyVar)
                                .addWhere(varietyVar, RDFS.label.asNode(), varietyNameVar)
                )
                .addOptional(
                        experimentsWhere
                )
                .addOptional(
                        new WhereBuilder().addGraph(germplasmGraph, uriVar, RDFS.label.asNode(), labelVar)
                )
        );

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
                        if (StringUtils.isNotEmpty(joiningColumn)) {
                            String[] experiments = joiningColumn.split(SPARQLQueryHelper.GROUP_CONCAT_SEPARATOR);

                            model.setExperiments(Arrays.stream(experiments).map(xp -> {
                                try {
                                    return new URI(xp);
                                } catch (URISyntaxException e) {
                                    throw new RuntimeException(e);
                                }
                            }).collect(Collectors.toList()));
                        } else {
                            model.setExperiments(new ArrayList<>());
                        }
                        return model;
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
        ).collect(Collectors.toList());

        return new ListWithPagination<>(searchResults, page, pageSize, numberOfAccessions);
    }
}
