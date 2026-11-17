//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create.steps;

import java.util.List;

/**
 * Where one step stands for a conversation.
 *
 * @param blockedBy the steps it depends on that are not done, when it is blocked
 * @param done      how many of the things the files name for this step exist on the instance
 * @param total     how many the files name
 * @author Arnaud Charleroy
 */
public record StepState(ImportStep step, StepStatus status, List<ImportStep> blockedBy, int done, int total) {
}
