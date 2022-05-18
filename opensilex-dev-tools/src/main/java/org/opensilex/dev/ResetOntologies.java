/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.dev;

import org.opensilex.OpenSilex;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author vince
 */
public class ResetOntologies {

    public static void main(String[] args) throws Exception {

        Map<String, String> customArgs = new HashMap<>();
        customArgs.put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.INTERNAL_OPERATIONS_PROFILE_ID);

        DevModule.run(null, new String[]{
                "sparql",
                "reset-ontologies"
        }, customArgs);
    }
}
