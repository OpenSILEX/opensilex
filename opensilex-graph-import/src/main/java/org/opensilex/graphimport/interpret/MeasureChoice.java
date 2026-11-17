//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.interpret;

import java.net.URI;

/**
 * The variable a column of a sheet of data measures: chosen by the user, or suggested because the header
 * is the name of a variable.
 *
 * @param variable the variable
 * @param status   {@link Choice.Status#CHOSEN} or {@link Choice.Status#SUGGESTED}
 * @param label    its name, to show
 * @author Arnaud Charleroy
 */
public record MeasureChoice(URI variable, Choice.Status status, String label) {
}
