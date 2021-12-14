/*******************************************************************************
 *                         OntologyStoreLoader.java
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2021.
 * Contact: renaud.colin@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
 *
 ******************************************************************************/

package org.opensilex.sparql.ontology.store;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.opensilex.OpenSilex;
import org.opensilex.sparql.deserializer.URIDeserializer;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLNamedResourceModel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.model.SPARQLTreeModel;
import org.opensilex.sparql.ontology.dal.ClassModel;
import org.opensilex.sparql.ontology.dal.DatatypePropertyModel;
import org.opensilex.sparql.service.SPARQLQueryHelper;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.apache.jena.arq.querybuilder.Converters.makeVar;

/**
 * @author rcolin
 */
public class OntologyStoreLoader {

    private final SPARQLService sparql;
    private final AbstractOntologyStore ontologyStore;

    protected static final ExprFactory exprFactory = SPARQLQueryHelper.getExprFactory();

    protected static final Var URI_VAR = makeVar(SPARQLResourceModel.URI_FIELD);
    protected static final Var PARENT_VAR = makeVar(SPARQLTreeModel.PARENT_FIELD);
    protected static final Var DOMAIN_VAR = makeVar(DatatypePropertyModel.DOMAIN_FIELD);
    protected static final Var RANGE_VAR = makeVar(DatatypePropertyModel.RANGE_FIELD);
    private static final Node OWL_CLASS_URI = NodeFactory.createURI(OWL2.Class.getURI());

    protected final List<String> languages;
    protected final List<Var> commentVars;
    protected final List<Var> nameVars;
    protected final SelectBuilder getAllClassesQuery;

    public OntologyStoreLoader(SPARQLService sparql, AbstractOntologyStore ontologyStore, List<String> languages) {
        this.sparql = sparql;
        this.ontologyStore = ontologyStore;

        this.languages = languages;
        commentVars = languages.stream()
                .map(lang -> getLangVar(ClassModel.COMMENT_FIELD, lang))
                .collect(Collectors.toList());

        nameVars = languages.stream()
                .map(lang -> getLangVar(SPARQLNamedResourceModel.NAME_FIELD, lang))
                .collect(Collectors.toList());

        getAllClassesQuery = buildGetAllClassesQuery();
    }

    private static Var getLangVar(String field, String lang) {
        String varName = field + "_" + (StringUtils.isEmpty(lang) ? "no_lang" : lang);
        return makeVar(varName);
    }

    public void load() throws SPARQLException {

        List<ClassModel> classes = new ArrayList<>();
        AtomicReference<String> lastClassURI = new AtomicReference<>();

        sparql.executeSelectQueryAsStream(getAllClassesQuery).forEach(result -> {

            ClassModel classModel;
            String classURI = URIDeserializer.formatURIAsStr(result.getStringValue(SPARQLResourceModel.URI_FIELD));

            if (!classURI.equals(lastClassURI.get())) {
                classModel = new ClassModel();
                classModel.setUri(URI.create(classURI));
                classModel.setType(AbstractOntologyStore.OWL_CLASS_URI);
//            model.setTypeLabel(defaultClassLabel);
                classModel.setLabel(getLabel(result, nameVars));
                classModel.setComment(getLabel(result, commentVars));
                classes.add(classModel);

                lastClassURI.set(classURI);
            } else {
                classModel = classes.get(classes.size() - 1);
            }

            String parentURI = result.getStringValue(SPARQLTreeModel.PARENT_FIELD);
            if (!StringUtils.isEmpty(parentURI)) {
                ClassModel parentModel = new ClassModel();
                parentModel.setUri(URIDeserializer.formatURI(parentURI));
                classModel.getParents().add(parentModel);
            }

        });

        ontologyStore.addAll(classes);
    }

    private void appendNamesAndComments(SelectBuilder select) {

        nameVars.forEach(select::addVar);
        commentVars.forEach(select::addVar);

        for (int i = 0; i < languages.size(); i++) {
            Var nameVar = nameVars.get(i);
            Var commentVar = commentVars.get(i);
            String lang = languages.get(i);

            select.addOptional(new WhereBuilder()
                    .addWhere(URI_VAR, RDFS.label, nameVar)
                    .addFilter(SPARQLQueryHelper.langFilter(nameVar.getVarName(), lang, false))
            );
            select.addOptional(new WhereBuilder()
                    .addWhere(URI_VAR, RDFS.comment, commentVar)
                    .addFilter(SPARQLQueryHelper.langFilter(commentVar.getVarName(), lang, false))
            );
        }
    }

    protected SPARQLLabel getLabel(SPARQLResult result, List<Var> translatedVars) {

        SPARQLLabel label = new SPARQLLabel();

        for (int i = 0; i < languages.size(); i++) {
            String varName = translatedVars.get(i).getVarName();
            String value = result.getStringValue(varName);
            if (!StringUtils.isEmpty(value)) {
                label.addTranslation(value, languages.get(i));
            }
        }

        Map<String, String> labelTranslations = label.getTranslations();
        if (labelTranslations.containsKey(OpenSilex.DEFAULT_LANGUAGE)) {
            label.setDefaultValue(labelTranslations.get(OpenSilex.DEFAULT_LANGUAGE));
            label.setDefaultLang(OpenSilex.DEFAULT_LANGUAGE);
        }

        return label;
    }


    private SelectBuilder buildGetAllClassesQuery() {

        SelectBuilder select = new SelectBuilder()
                .setDistinct(true)
                .addVar(URI_VAR)
                .addVar(PARENT_VAR)
                .addWhere(URI_VAR, RDF.type, OWL_CLASS_URI)
                .addFilter(exprFactory.isIRI(URI_VAR))
                .addOptional(new WhereBuilder()
                        .addWhere(URI_VAR, RDFS.subClassOf, PARENT_VAR)
                        .addWhere(PARENT_VAR, RDF.type, OWL_CLASS_URI)
                        .addFilter(exprFactory.isIRI(PARENT_VAR))
                ).addOrderBy(URI_VAR);

        appendNamesAndComments(select);

        return select;
    }
}
