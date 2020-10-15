//******************************************************************************
//                          DocumentModel.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: vincent.migot@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.core.document.dal;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.*;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.core.ontology.Oeso;
import org.apache.jena.vocabulary.DCTerms;
import org.opensilex.sparql.model.SPARQLTreeModel;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;
import org.opensilex.sparql.model.SPARQLLabel;
import org.opensilex.sparql.model.SPARQLResourceModel;
import org.opensilex.sparql.annotations.SPARQLTypeRDF;
import org.opensilex.sparql.utils.ClassURIGenerator;


/**
 * @author Emilie Fernandez
 */
@SPARQLResource(
        ontology = Oeso.class,
        resource = "Document",
        graph = "set/documents",
        prefix = "doc"
)
public class DocumentModel extends SPARQLResourceModel implements ClassURIGenerator<DocumentModel> {

    @SPARQLTypeRDF()
    URI type;
    public static final String TYPE_FIELD = "type";

    @SPARQLProperty(
        ontology = Oeso.class,
        property = "concerns"
    )
    URI concerns;
    public static final String CONCERNS_FIELD = "concerns";

    @SPARQLProperty(
            ontology = DCTerms.class,
            property = "title",
            required = true
    )
    String name;
    public static final String NAME_FIELD = "name";

    @SPARQLProperty(
        ontology = DCTerms.class,
        property = "creator",
        required = true
    )
    URI creator;
    public static final String CREATOR_FIELD = "creator";

    @SPARQLProperty(
        ontology = DCTerms.class,
        property = "language"
    )
    String language;
    public static final String LANGUAGE_FIELD = "language";

    @SPARQLProperty(
        ontology = DCTerms.class,
        property = "date",
        required = true
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
        ontology = RDFS.class,
        property = "comment"
    )
    String comment;
    public static final String COMMENT_FIELD = "comment";

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

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    public URI getConcerns() {
        return concerns;
    }

    public void setConcerns(URI concerns) {
        this.concerns = concerns;
    }

    public URI getCreator() {
        return creator;
    }

    public void setCreator(URI creator) {
        this.creator = creator;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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

    @Override
    public String[] getUriSegments(DocumentModel instance) {
        return new String[]{
            instance.getName()
        };
    }
}
