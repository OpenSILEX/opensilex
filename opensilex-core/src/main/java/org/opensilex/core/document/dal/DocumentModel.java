//******************************************************************************
//                          DocumentModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.document.dal;

import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OA;
import org.apache.jena.vocabulary.OWL2;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.uri.generation.ClassURIGenerator;

import java.net.URI;
import java.util.List;


/**
 * @author Emilie Fernandez
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "Document",
        graph = DocumentModel.GRAPH,
        prefix = "doc"
)
public class DocumentModel extends SPARQLResourceModel implements ClassURIGenerator<DocumentModel> {

    public static final String GRAPH = "document";

    @SPARQLProperty(
            ontology = OA.class,
            property = "hasTarget",
            required = true
    )
    private List<URI> targets;
    public static final String TARGET_FIELD = "targets";

    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "title",
            required = true
    )
    String title;
    public static final String TITLE_FIELD = "title";

    @SPARQLProperty(
        ontology = Oeso.class,
        property = "hasAuthor"
    )
    List<String> authors;
    public static final String AUTHORS_FIELD = "authors";

    @SPARQLProperty(
        ontology = DCTerms.class,
        property = "language"
    )
    String language;
    public static final String LANGUAGE_FIELD = "language";

    @SPARQLProperty(
        ontology = DCTerms.class,
        property = "date"
    )
    String date;
    public static final String DATE_FIELD = "date";

    @SPARQLProperty(
        ontology = DCTerms.class,
        property = "format"
    )
    String format;
    public static final String FORMAT_FIELD = "format";

    @SPARQLProperty(
        ontology = DCTerms.class,
        property = "description"
    )
    String description;
    public static final String DESCRIPTION_FIELD = "description";

    @SPARQLProperty(
        ontology = DCTerms.class,
        property = "subject"
    )
    List<String> subject;
    public static final String SUBJECT_FIELD = "subject";

    @SPARQLProperty(
        ontology = OWL2.class,
        property = "deprecated"
    )
    String deprecated;
    public static final String DEPRECATED_FIELD = "deprecated";

    @SPARQLProperty(
        ontology = DCTerms.class,
        property = "identifier"
    )
    String identifier;
    public static final String IDENTIFIER_FIELD = "identifier";

    @SPARQLProperty(
        ontology = DCTerms.class,
        property = "source"
    )
    URI source;
    public static final String SOURCE_FIELD = "source";

    public List<URI> getTargets() {
        return targets;
    }

    public void setTargets(List<URI> targets) {
        this.targets = targets;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getSubject() {
        return subject;
    }

    public void setSubject(List<String> subject) {
        this.subject = subject;
    }

    public String getDeprecated() {
        return deprecated;
    }

    public void setDeprecated(String deprecated) {
        this.deprecated = deprecated;
    }
    
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public URI getSource() {
        return source;
    }

    public void setSource(URI source) {
        this.source = source;
    }

    @Override
    public String[] getInstancePathSegments(DocumentModel instance) {
        return new String[]{
            instance.getTitle()
        };
    }
}
