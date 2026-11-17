//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.profile.custom;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * What the user said of the files of a conversation: which experiment they belong to when no
 * column says, and what each sheet is. State only, kept with the conversation; the rules and the
 * defaults live elsewhere.
 *
 * @author Arnaud Charleroy
 */
public class FileDescription {

    private String experimentUri;
    private String experimentName;
    private final Map<String, SheetDescription> sheets = new LinkedHashMap<>();

    /**
     * @return the experiment the user chose, as its URI, or {@code null}
     */
    public String getExperimentUri() {
        return experimentUri;
    }

    /**
     * @return the experiment the user chose, by the name the file will be matched against
     */
    public String getExperimentName() {
        return experimentName;
    }

    public FileDescription setExperiment(String uri, String name) {
        this.experimentUri = uri;
        this.experimentName = name;
        return this;
    }

    public Map<String, SheetDescription> getSheets() {
        return sheets;
    }

    public Optional<SheetDescription> find(String sheet) {
        return Optional.ofNullable(sheets.get(sheet));
    }

    /**
     * @return what the user said of the sheet, created empty on first use
     */
    public SheetDescription sheet(String sheet) {
        return sheets.computeIfAbsent(sheet, SheetDescription::new);
    }
}
