/*
 * ******************************************************************************
 *                                     FactorDAO.java
 *  OpenSILEX
 *  Copyright © INRA 2019
 *  Creation date:  17 December, 2019
 *  Contact: arnaud.charleroy@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 * ******************************************************************************
 */
package org.opensilex.core.experiment.factor.dal;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.service.schemaQuery.SparqlSchema;
import org.opensilex.sparql.service.schemaQuery.SparqlSchemaNode;
import org.opensilex.sparql.service.schemaQuery.SparqlSchemaRootNode;
import org.opensilex.utils.ListWithPagination;
import org.opensilex.utils.OrderBy;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

/**
 *
 * @author Arnaud Charleroy
 */
public class FactorDAO {

    protected final SPARQLService sparql;
    
    public static final String AGROVOC_FACTOR_CONCEPT_URI = "http://aims.fao.org/aos/agrovoc/c_331093";

    public FactorDAO(SPARQLService sparql) {
        this.sparql = sparql;
    }

    public FactorModel create(FactorModel instance) throws Exception {
        sparql.create(instance);
        return instance;
    }

    public FactorModel update(FactorModel instance) throws Exception {
        sparql.update(instance);
        return instance;
    }

    public void delete(URI instanceURI) throws Exception {
        sparql.delete(FactorModel.class, instanceURI);
    }

    public FactorModel get(URI instanceURI) throws Exception {
        return sparql.getByURI(FactorModel.class, instanceURI, null);
    }

    public ListWithPagination<FactorModel> search(
            String name,
            String factorLevelName,
            URI category,
            List<URI> experiments,
            List<OrderBy> orderByList,
            Integer page,
            Integer pageSize,
            String lang,
            boolean withFactorLevels
    ) throws Exception {

        SparqlSchemaNode<FactorLevelModel> factorLevsNode = new SparqlSchemaNode<>(
                FactorLevelModel.class,
                FactorModel.FACTORLEVELS_SPARQL_VAR,
                Collections.emptyList(),
                true,
                false
        );

        SparqlSchemaRootNode<FactorModel> rootNode = new SparqlSchemaRootNode<>(
                FactorModel.class,
                (withFactorLevels ? Collections.singletonList(factorLevsNode) : Collections.emptyList()),
                false
        );

        SparqlSchema<FactorModel> schema = new SparqlSchema<>(rootNode);

        return sparql.searchWithPaginationUsingSchema(
                FactorModel.class,
                lang,
                (SelectBuilder select) -> {
                    // TODO implements filters
                    appendFilters( name, factorLevelName, category, experiments, select);
                },
                schema,
                orderByList,
                page,
                pageSize
        );
    }

    public List<FactorModel> getAll(String lang) throws Exception {
        return sparql.search(FactorModel.class, lang);
    }

    /**
     * Append FILTER or VALUES clause on the given {@link SelectBuilder} for each non-empty simple attribute ( not a
     * {@link List} from the {@link FactorSearchDTO}
     *
     * @param name name search attribute
     * @param factorLevelName name search factorLevel attribute
     * @param category category of the factor
     * @param experimentUris experiment uris
     * @param select search query
     * @throws java.lang.Exception can throw an exception
     * @see SPARQLQueryHelper the utility class used to build Expr
     */
    protected void appendFilters(String name, String factorLevelName,  URI category, List<URI> experimentUris, SelectBuilder select)
            throws Exception {
        // build regex filters
        if (!StringUtils.isEmpty(name)) {
            select.addFilter(SPARQLQueryHelper.regexFilter(FactorModel.NAME_FIELD, name));
        }
        
        if (!StringUtils.isEmpty(factorLevelName)) {
            Var factorLevelVars = makeVar(FactorModel.FACTORLEVELS_SPARQL_VAR);
            Var factorLevelNameVar = makeVar(FactorLevelModel.NAME_FIELD + "factorLevel");
            Var uriVar = makeVar(FactorModel.URI_FIELD);
            select.addWhere(new Triple(factorLevelVars, Oeso.hasFactor.asNode(),uriVar));
            select.addWhere(new Triple(factorLevelVars, RDFS.label.asNode(),factorLevelNameVar));
            select.addFilter(SPARQLQueryHelper.regexFilter(FactorLevelModel.NAME_FIELD + "factorLevel", factorLevelName)); 
        }


        if (category != null) {
            Var uriVar = makeVar(FactorModel.URI_FIELD);
            select.addWhere(new Triple(
                    uriVar,
                    Oeso.hasCategory.asNode(),
                    SPARQLDeserializers.nodeURI(category))
            );
        }

        if (experimentUris != null && !experimentUris.isEmpty()) {
            Var xpsVar = makeVar(FactorModel.EXPERIMENTS_FIELD);
            Var xpVar = makeVar(FactorModel.EXPERIMENT_FIELD);
            Var uriVar = makeVar(FactorModel.URI_FIELD);
            select.addWhere(new Triple(xpsVar, Oeso.studyEffectOf.asNode(),uriVar));
            Expr experimentFilter = SPARQLQueryHelper.or(
                SPARQLQueryHelper.inURIFilter(xpsVar, experimentUris),
                SPARQLQueryHelper.inURIFilter(xpVar, experimentUris)
            );
            select.addFilter(experimentFilter);

//            SPARQLQueryHelper.addWhereValues(select, FactorModel.EXPERIMENTS_FIELD, experimentUris);
        }
    }
 

    public List<FactorModel> getList(List<URI> uris) throws Exception {
        return sparql.getListByURIs(FactorModel.class, uris, null);
    }

    public List<FactorModel> getByExperiment(URI xpUri, String lang) throws Exception {
        return sparql.search(FactorModel.class, lang, (select) -> {
            Var xpsVar = makeVar(FactorModel.EXPERIMENTS_FIELD);
            Var xpVar = makeVar(FactorModel.EXPERIMENT_FIELD);
            Var uriVar = makeVar(FactorModel.URI_FIELD);
            
            Expr experimentFilter = SPARQLQueryHelper.or(
                SPARQLQueryHelper.eq(xpsVar, SPARQLDeserializers.nodeURI(xpUri)),
                SPARQLQueryHelper.eq(xpVar, SPARQLDeserializers.nodeURI(xpUri))
            );
            select.addFilter(experimentFilter); 
            select.addWhere(makeVar(FactorModel.URI_FIELD), Oeso.studiedEffectIn, SPARQLDeserializers.nodeURI(xpUri));
            select.addOptional(new Triple(xpsVar, Oeso.studyEffectOf.asNode(),uriVar));
        });
    }
    
    public List<FactorCategoryModel> searchCategories(String stringPattern, String lang ,List<OrderBy> orderByList) throws Exception{

        return sparql.search(
                FactorCategoryModel.class,
                lang,
                selectBuilder -> { 
                    addFactorCategoryNameRegexFilter(selectBuilder, stringPattern);
                    addFactorCategoryNameLangFilter(selectBuilder, lang);
                },
                orderByList
        );

    }
  
    private void addFactorCategoryNameRegexFilter(SelectBuilder selectBuilder, String stringPattern) {
        Expr regexFilter = SPARQLQueryHelper.regexFilter(FactorCategoryModel.NAME_FIELD, stringPattern);
        if (regexFilter != null) {
            selectBuilder.addFilter(regexFilter);
        }
    }

    private void addFactorCategoryNameLangFilter(SelectBuilder selectBuilder, String lang) {
        if (!StringUtils.isEmpty(lang)) {
            Expr langFilter = SPARQLQueryHelper.langFilterWithDefault(FactorCategoryModel.NAME_FIELD, Locale.forLanguageTag(lang).getLanguage());
            selectBuilder.addFilter(langFilter);
        }
    }
    
    /**
     * Get factor associated experiment URI List
     * @param factor
     * @return
     * @throws Exception 
     */
    public List<URI> associatedFactorExperimentURIs(FactorModel factor) throws Exception {
        Var uriVar = makeVar(SPARQLResourceModel.URI_FIELD);
        List<SPARQLResourceModel> search = sparql.search(
                SPARQLResourceModel.class,
                null,
                selectBuilder -> {
                    selectBuilder.addWhere(new Triple(
                            uriVar,
                            Oeso.hasFactor.asNode(),
                            SPARQLDeserializers.nodeURI(factor.getUri())));
                },
                null
        );
        List<URI> uriList = new ArrayList<>();
        for (SPARQLResourceModel resource : search) {
            uriList.add(resource.getUri());
        }

        return uriList;
    }

    /**
     * Count total of factors binded to a experiment URI
     *
     * @param experiment the URI on which find associated factors
     * @return the number of factors associated to a target
     */
    public int countFactors(URI experiment) throws Exception {

        Node factorGraph = sparql.getDefaultGraph(FactorModel.class);
        return sparql.count(factorGraph, FactorModel.class,null, (SelectBuilder countBuilder) -> {
            this.appendFilters(null, null,null, Collections.singletonList(experiment),countBuilder);
        },null);
    }
    
}
