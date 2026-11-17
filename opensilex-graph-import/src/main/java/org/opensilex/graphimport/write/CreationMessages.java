//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.write;

import org.opensilex.aiimport.report.ReportMessage;

import java.net.URI;

/**
 * What the creation step tells the user, translatable under {@code GraphImport.report.creation.}.
 *
 * @author Arnaud Charleroy
 */
public final class CreationMessages {

    private CreationMessages() {
    }

    public static ReportMessage toFix() {
        return ReportMessage.of("creation.toFix", "The file must be corrected for this resource first.");
    }

    public static ReportMessage completing() {
        return ReportMessage.of("creation.completing",
                "This resource exists already: completing it is not supported yet.");
    }

    public static ReportMessage conflict() {
        return ReportMessage.of("creation.conflict", "This resource is in conflict with what the instance holds.");
    }

    public static ReportMessage targetFirst(String name, String step) {
        return ReportMessage.of("creation.targetFirst", "\"" + name + "\" must be created first, at the step " + step
                + ".").with("name", name).with("step", step);
    }

    public static ReportMessage targetBlocked(String name) {
        return ReportMessage.of("creation.targetBlocked", "\"" + name + "\" cannot be created, so neither can this.")
                .with("name", name);
    }

    public static ReportMessage pointedByBlocked(String name) {
        return ReportMessage.of("creation.pointedByBlocked", "\"" + name
                + "\", created with it, cannot be created yet: it waits for it.").with("name", name);
    }

    public static ReportMessage targetNotCreated(String name) {
        return ReportMessage.of("creation.targetNotCreated",
                "\"" + name + "\" is to be created and no step creates it.").with("name", name);
    }

    public static ReportMessage unwritable(URI property) {
        return ReportMessage.of("creation.unwritable", "The assistant cannot write " + property + " yet.")
                .with("property", property.toString());
    }

    public static ReportMessage noExperiment() {
        return ReportMessage.of("creation.noExperiment",
                "A factor belongs to an experiment: choose the experiment of the import, or give the factor one.");
    }

    public static ReportMessage noExperimentForObjects() {
        return ReportMessage.of("creation.noExperimentForObjects",
                "Scientific objects are created in an experiment: choose the experiment of the import.");
    }

    public static ReportMessage tooMany(int count, int most) {
        return ReportMessage.of("creation.tooMany", "This step would create " + count + " resources; at most " + most
                + " are created at once. Split the file.").with("count", count).with("most", most);
    }

    public static ReportMessage sameName(String name) {
        return ReportMessage.of("creation.sameName", "Two resources of this step are named \"" + name + "\".")
                .with("name", name);
    }

    public static ReportMessage noMoment() {
        return ReportMessage.of("creation.noMoment", "An event needs a date: its beginning or its end.");
    }

    public static ReportMessage noTarget() {
        return ReportMessage.of("creation.noTarget", "An event concerns at least one resource: give it one.");
    }

    public static ReportMessage existingFactor() {
        return ReportMessage.of("creation.existingFactor",
                "The factor of this level exists already: adding a level to it is not supported yet.");
    }

    public static ReportMessage unreadable(URI property, String value) {
        return ReportMessage.of("creation.unreadable", "The value " + value + " of " + property + " cannot be read.")
                .with("property", property.toString()).with("value", value);
    }

    public static ReportMessage refused(String detail) {
        return ReportMessage.of("creation.refused", "The platform refused it: " + detail).with("detail", detail);
    }

    public static ReportMessage forbidden() {
        return ReportMessage.of("creation.forbidden", "You may not create these resources.");
    }

    public static ReportMessage noProvenance() {
        return ReportMessage.of("creation.noProvenance",
                "Data need a provenance: choose the provenance of the import's data at the step Understand.");
    }

    public static ReportMessage batchRefused(int batch, int batches, String detail) {
        return ReportMessage.of("creation.batchRefused", "The platform refused the batch " + batch + " of " + batches
                + ": " + detail).with("batch", batch).with("batches", batches).with("detail", detail);
    }
}
