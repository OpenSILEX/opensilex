//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology;

import org.apache.jena.vocabulary.XSD;
import org.opensilex.core.ontology.Oeso;

import java.net.URI;

/**
 * How the assistant reads the properties of an event, which the platform writes its own way rather than
 * as the ontology says, so that a file can give them as a person writes them:
 * <ul>
 *     <li>a moment ({@code time:hasBeginning}, {@code time:hasEnd}) points to a {@code time:Instant} the
 *     platform creates with the event: the file gives its date and time, read as one, a date alone being
 *     the start of that day;</li>
 *     <li>what an event concerns ({@code oeev:concerns}) is a URI held as text: the file gives the name or
 *     URI of a scientific object, read as a link;</li>
 *     <li>whether an event is instant ({@code oeev:isInstant}) follows from its end: the platform requires
 *     it, the event writer works it out, so the file need not give it;</li>
 *     <li>the provenance of a datum ({@code oeso:hasProvenance}) is chosen once for the whole import,
 *     so the file need not give it.</li>
 * </ul>
 *
 * @author Arnaud Charleroy
 */
public final class PlatformReadings {

    static final URI INSTANT = URI.create("http://www.w3.org/2006/time#Instant");
    static final URI BEGINNING = URI.create("http://www.w3.org/2006/time#hasBeginning");
    static final URI END = URI.create("http://www.w3.org/2006/time#hasEnd");
    static final URI CONCERNS = URI.create("http://www.opensilex.org/vocabulary/oeev#concerns");
    static final URI IS_INSTANT = URI.create("http://www.opensilex.org/vocabulary/oeev#isInstant");
    static final URI PROVENANCE = URI.create("http://www.opensilex.org/vocabulary/oeso#hasProvenance");
    private static final URI DATE_TIME = URI.create(XSD.dateTime.getURI());
    private static final URI SCIENTIFIC_OBJECT = URI.create(Oeso.ScientificObject.getURI());

    private PlatformReadings() {
    }

    /** Whether the property is a moment of an event, which a date alone gives as the start of its day. */
    public static boolean isMoment(URI property) {
        return BEGINNING.equals(property) || END.equals(property);
    }

    /** The restriction as the assistant reads it. */
    public static RestrictionRow read(RestrictionRow row) {
        if (row.kind() == PropertyKind.OBJECT && INSTANT.equals(row.range())) {
            return new RestrictionRow(row.domain(), row.property(), PropertyKind.DATA, DATE_TIME, row.cardinality());
        }
        if (CONCERNS.equals(row.property())) {
            return new RestrictionRow(row.domain(), row.property(), PropertyKind.OBJECT, SCIENTIFIC_OBJECT,
                    row.cardinality());
        }
        if (IS_INSTANT.equals(row.property()) || PROVENANCE.equals(row.property())) {
            return new RestrictionRow(row.domain(), row.property(), row.kind(), row.range(),
                    new Cardinality(0, row.cardinality().max()));
        }
        return row;
    }
}
