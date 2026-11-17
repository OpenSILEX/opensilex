//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.template.csvw;

/**
 * The keys of a CSVW document that are the module's own. CSVW lets a document carry annotations
 * under absolute IRIs, which any reader may ignore: these are the ones this module writes and reads,
 * all in one place so that a reader of another tool sees where they come from.
 *
 * @author Arnaud Charleroy
 */
final class CsvwVocabulary {

    static final String CONTEXT = "http://www.w3.org/ns/csvw";
    static final String NAMESPACE = "https://opensilex.org/ai-import/csvw#";

    static final String TITLE = "http://purl.org/dc/terms/title";
    static final String KIND = NAMESPACE + "kind";
    static final String ROLE = NAMESPACE + "role";
    static final String TYPE = NAMESPACE + "type";
    static final String PROPERTY = NAMESPACE + "property";

    private CsvwVocabulary() {
    }
}
