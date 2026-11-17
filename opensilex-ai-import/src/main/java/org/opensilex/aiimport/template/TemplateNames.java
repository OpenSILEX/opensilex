//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.template;

import org.apache.commons.io.FilenameUtils;
import org.opensilex.aiimport.service.AiImportSession;

/**
 * The names a template is given and downloaded under.
 *
 * @author Arnaud Charleroy
 */
public final class TemplateNames {

    private static final String DEFAULT = "template";

    private TemplateNames() {
    }

    /**
     * @return the name a template starts with: the first file of the conversation, without its extension
     */
    public static String of(AiImportSession session) {
        String file = session.getFileName();
        return file == null || file.isBlank() ? DEFAULT : FilenameUtils.getBaseName(file);
    }

    /**
     * @return the name of the file a template is downloaded as, reduced to what every file system accepts
     */
    public static String fileName(FileTemplate template) {
        String name = template.getName() == null || template.getName().isBlank() ? DEFAULT : template.getName();
        return name.replaceAll("[^A-Za-z0-9._-]+", "_") + ".csvm.json";
    }
}
