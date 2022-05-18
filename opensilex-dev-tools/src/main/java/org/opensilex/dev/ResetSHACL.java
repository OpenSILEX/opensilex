//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.dev;

import java.nio.file.Path;
import java.util.HashMap;
import org.opensilex.OpenSilex;
import org.opensilex.cli.MainCommand;

/**
 *
 * @author vincent
 */
public class ResetSHACL {

    public static void main(String[] args) throws Exception {
        start(OpenSilex.getDefaultBaseDirectory());
    }

    public static void start(Path baseDirectory) throws Exception {
        DevModule.run(null,new String[]{
            "sparql",
            "shacl-enable"
        },null);
    }
}
