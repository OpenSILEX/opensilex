//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.dev;

import java.nio.file.*;

/**
 *
 * @author vincent
 */
public class StartServer {

    public static void main(String[] args) throws Exception {
        start(null);
    }

    public static void start(Path baseDirectory) throws Exception {
        DevModule.run(baseDirectory, new String[]{
            "server",
            "start"
        },null);
    }

}
