//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile;

import org.apache.jena.vocabulary.RDFS;
import org.opensilex.core.location.dal.LocationModel;
import org.opensilex.core.ontology.Oeso;

import java.util.Set;

/**
 * What a column of an object sheet can become when the objects are created.
 * <p>
 * The values are what the platform's scientific-object importer reads as a column header: a
 * property URI, or the name of one of its move columns for a position. A column mapped to one of
 * them is written under that header, so the importer — not this module — decides whether the value
 * is acceptable for the type chosen.
 *
 * @author Arnaud Charleroy
 */
public final class ObjectTargets {

    public static final String NAME = RDFS.label.getURI();
    public static final String GERMPLASM = Oeso.hasGermplasm.getURI();
    public static final String FACTOR_LEVEL = Oeso.hasFactorLevel.getURI();
    public static final String PARENT = Oeso.isPartOf.getURI();
    public static final String COMMENT = RDFS.comment.getURI();
    public static final String X = LocationModel.X_FIELD;
    public static final String Y = LocationModel.Y_FIELD;

    /**
     * The column is read, shown, and not written.
     */
    public static final String NONE = "";

    /**
     * Targets the platform validates by rules of its own rather than by the type's restrictions: a
     * factor level against the experiment, a position as a move. Always offered, whatever the type.
     */
    public static final Set<String> ALWAYS_ACCEPTED = Set.of(NAME, FACTOR_LEVEL, X, Y, NONE);

    private ObjectTargets() {
    }

    public static boolean isPosition(String target) {
        return X.equals(target) || Y.equals(target);
    }
}
