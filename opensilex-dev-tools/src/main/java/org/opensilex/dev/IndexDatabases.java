
//******************************************************************************
//                        IndexDatabases.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2020
// Contact: anne.tireau@inrae.fr, pascal.neveu@inrae.fr
//******************************************************************************
package org.opensilex.dev;

import java.nio.file.*;

/**
 *
 * @author Cheimae
 */
public class IndexDatabases {

    public static void main(String[] args) throws Exception {
        DevModule.run(null, new String[]{
            "indexer",
            "index-db"
        });
    }

}