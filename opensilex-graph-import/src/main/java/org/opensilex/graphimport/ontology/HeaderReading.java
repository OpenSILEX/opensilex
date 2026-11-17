//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology;

import java.net.URI;

/**
 * What a column header says by its form alone, before the ontology is asked whether the property
 * exists.
 *
 * @param form     how the header is written
 * @param property the property it names, as a full URI; null unless {@link #names()}
 * @param language the language of a {@code @fr} suffix, null without one
 * @param prefix   the prefix that was not recognised, for {@link Form#UNKNOWN_PREFIX}; null otherwise
 * @author Arnaud Charleroy
 */
public record HeaderReading(Form form, URI property, String language, String prefix) {

    /** How a header is written. */
    public enum Form {
        /** {@code http://…} or {@code <http://…>}. */
        FULL_URI,
        /** {@code prefix:name} with a known prefix. */
        PREFIXED_NAME,
        /** {@code rdf:type} or {@code a}: the column gives the class of each row. */
        TYPE,
        /** Written as {@code prefix:name}, but the prefix is not known: worth telling the user. */
        UNKNOWN_PREFIX,
        /** An ordinary header, left to the evidence. */
        PLAIN
    }

    static HeaderReading plain() {
        return new HeaderReading(Form.PLAIN, null, null, null);
    }

    /** True when the header names a property by itself. */
    public boolean names() {
        return property != null;
    }
}
