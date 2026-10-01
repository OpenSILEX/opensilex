//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * What the user can create from an open conversation.
 *
 * @author Arnaud Charleroy
 */
public enum CreationTarget {

    PROJECT,
    EXPERIMENT,

    /**
     * The treatments of the workbook, as the levels of the experiment's factors: they must exist
     * before an object can name one.
     */
    FACTORS,

    /**
     * The observed objects of the workbook, in one pass, through the platform's own importer.
     */
    SCIENTIFIC_OBJECTS,

    /**
     * Variables brought in from a shared resource instance, their components with them. Creating a
     * variable from nothing is a different target: this one only copies what already exists
     * somewhere authoritative.
     */
    VARIABLE,

    /**
     * What happened during the trial: sprayings, incidents, observation rounds.
     */
    EVENT,

    DATA;

    /**
     * The target the model named, whatever its case; empty when it names none.
     */
    public static Optional<CreationTarget> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String name = raw.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).filter(target -> target.name().equals(name)).findFirst();
    }

    /**
     * Every target, as the tools list them to the model: "PROJECT, EXPERIMENT, … or DATA".
     */
    public static String choices() {
        String all = Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
        int last = all.lastIndexOf(", ");
        return all.substring(0, last) + " or " + all.substring(last + 2);
    }
}
