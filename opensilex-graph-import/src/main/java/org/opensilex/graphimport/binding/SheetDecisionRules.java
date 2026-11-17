//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.binding;

import org.opensilex.aiimport.workbook.SheetStructure;
import org.opensilex.graphimport.graph.scope.DataScope;
import org.opensilex.graphimport.interpret.ClassCatalog;
import org.opensilex.graphimport.interpret.ClassViews;
import org.opensilex.graphimport.ontology.ClassView;
import org.opensilex.graphimport.ontology.PropertyView;
import org.opensilex.graphimport.ontology.Uris;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What a decision about a sheet must respect before it is kept: a class the assistant knows, columns
 * the sheet has, properties the class accepts. A decision that breaks one is refused whole, so that
 * the user never believes a choice was kept when it was not.
 *
 * @author Arnaud Charleroy
 */
public final class SheetDecisionRules {

    private SheetDecisionRules() {
    }

    /**
     * @return why the decision cannot be kept, or empty when it can
     */
    public static Optional<String> whyNot(ImportDecisions.SheetDecisions decisions, SheetStructure sheet,
                                          ClassCatalog catalog, ClassViews views) throws Exception {
        int columns = sheet.getHeaders().size();
        Integer identifier = decisions.identifierColumn();
        if (identifier != null && (identifier < 0 || identifier >= columns)) {
            return Optional.of("The sheet has no column " + (identifier + 1) + ".");
        }
        for (Integer column : decisions.leftOut()) {
            if (column == null || column < 0 || column >= columns) {
                return Optional.of("The sheet has no column " + (column == null ? "" : column + 1) + ".");
            }
            if (decisions.properties().containsKey(column)) {
                return Optional.of("The column " + (column + 1) + " cannot be left out and given a property at once.");
            }
        }
        URI classUri = decisions.classUri() == null ? null : Uris.expand(decisions.classUri());
        if (classUri != null && !catalog.contains(classUri)) {
            return Optional.of("The assistant cannot import rows of " + decisions.classUri() + ".");
        }
        if (!decisions.measures().isEmpty()) {
            if (classUri == null || !(classUri.equals(DataScope.DATA) || catalog.isA(classUri, DataScope.DATA))) {
                return Optional.of("Only a sheet of data has columns of measures.");
            }
            for (Integer column : decisions.measures().keySet()) {
                if (column == null || column < 0 || column >= columns) {
                    return Optional.of("The sheet has no column " + (column == null ? "" : column + 1) + ".");
                }
                if (decisions.leftOut().contains(column)) {
                    return Optional.of("The column " + (column + 1) + " cannot be left out and hold measures at once.");
                }
            }
        }
        if (decisions.properties().isEmpty() && decisions.given().isEmpty()) {
            return Optional.empty();
        }
        if (classUri == null) {
            return Optional.of(decisions.properties().isEmpty()
                    ? "Choose the class of the sheet before giving a value for all its rows."
                    : "Choose the class of the sheet before the property of a column.");
        }
        ClassView view = views.get(classUri);
        for (URI property : decisions.given().keySet()) {
            if (view.getProperty(property) == null) {
                return Optional.of(classUri + " does not accept the property " + property + ".");
            }
            if (decisions.properties().containsValue(property)) {
                return Optional.of("A column already gives " + property + ": no value is given for all rows.");
            }
        }
        for (Map.Entry<Integer, URI> column : decisions.properties().entrySet()) {
            if (column.getKey() < 0 || column.getKey() >= columns) {
                return Optional.of("The sheet has no column " + (column.getKey() + 1) + ".");
            }
            if (view.getProperty(Uris.expand(column.getValue())) == null) {
                return Optional.of(classUri + " does not accept the property " + column.getValue() + ".");
            }
        }
        return Optional.empty();
    }

    /**
     * What a declared resource must respect before it is kept: a class the assistant knows, a name, and
     * properties its class accepts, no more values than each takes.
     *
     * @return why the resource cannot be kept, or empty when it can
     */
    public static Optional<String> whyNot(ImportDecisions.DeclaredResource resource, ClassCatalog catalog,
                                          ClassViews views) throws Exception {
        if (resource.id() == null || resource.id().isBlank()) {
            return Optional.of("A declared resource needs an identifier.");
        }
        if (resource.classUri() == null || !catalog.contains(resource.classUri())) {
            return Optional.of("The assistant cannot declare a resource of " + resource.classUri() + ".");
        }
        if (resource.name() == null) {
            return Optional.of("A declared resource needs a name.");
        }
        ClassView view = views.get(resource.classUri());
        for (Map.Entry<URI, List<String>> values : resource.values().entrySet()) {
            PropertyView property = view.getProperty(values.getKey());
            if (property == null) {
                return Optional.of(resource.classUri() + " does not accept the property " + values.getKey() + ".");
            }
            if (!property.cardinality().accepts(values.getValue().size())) {
                return Optional.of(values.getKey() + " cannot take " + values.getValue().size() + " values.");
            }
        }
        return Optional.empty();
    }
}
