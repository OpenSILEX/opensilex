//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import org.opensilex.graphimport.ontology.PropertyKind;
import org.opensilex.graphimport.ontology.PropertyView;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Which sheets each link property of a sheet may point to: the sheets whose rows are of a class the
 * property's range accepts, by the rule the links are resolved with (the class, or one of its
 * ancestors, is the range). The screens cannot tell it themselves, since a range may sit above the
 * classes they know.
 *
 * @author Arnaud Charleroy
 */
public final class SheetFits {

    private SheetFits() {
    }

    /**
     * @return for each sheet with a class, for each of its link properties, the names of the sheets its
     *         rows may point to, in the workbook's order
     */
    public static Map<String, Map<URI, List<String>>> of(Interpretation interpretation, ClassViews views)
            throws Exception {
        Map<String, Set<URI>> classesBySheet = new LinkedHashMap<>();
        for (SheetInterpretation sheet : interpretation.sheets()) {
            Set<URI> classes = new LinkedHashSet<>();
            if (sheet.sheetClass().chosen() != null) {
                classes.add(sheet.sheetClass().chosen());
            }
            classes.addAll(sheet.rowClasses());
            if (!classes.isEmpty()) {
                classesBySheet.put(sheet.name(), classes);
            }
        }
        Map<String, Map<URI, List<String>>> fits = new LinkedHashMap<>();
        for (SheetInterpretation sheet : interpretation.sheets()) {
            if (sheet.classView() == null) {
                continue;
            }
            Map<URI, List<String>> byProperty = new LinkedHashMap<>();
            for (PropertyView property : sheet.classView().getProperties()) {
                if (property.kind() == PropertyKind.OBJECT) {
                    byProperty.put(property.uri(), sheetsFor(property.range(), classesBySheet, views));
                }
            }
            fits.put(sheet.name(), byProperty);
        }
        return fits;
    }

    private static List<String> sheetsFor(URI range, Map<String, Set<URI>> classesBySheet, ClassViews views)
            throws Exception {
        List<String> sheets = new ArrayList<>();
        for (Map.Entry<String, Set<URI>> sheet : classesBySheet.entrySet()) {
            if (range == null || accepts(range, sheet.getValue(), views)) {
                sheets.add(sheet.getKey());
            }
        }
        return sheets;
    }

    private static boolean accepts(URI range, Set<URI> classes, ClassViews views) throws Exception {
        for (URI classUri : classes) {
            if (views.get(classUri).isA(range)) {
                return true;
            }
        }
        return false;
    }
}
