package org.opensilex.dev;

import org.opensilex.OpenSilex;

import java.util.HashMap;
import java.util.Map;

/**
 * @author rcolin
 */
public class RunUpdate {

    public static void main(String[] args) throws Exception {

        if(args == null || args.length == 0){
            throw new IllegalArgumentException("RTFM. Usage: <update_class_path>.  No argument provided, please provide a valid update class");
        }
        String updateClass = args[0];

        Map<String, String> customArgs = new HashMap<>();
        customArgs.put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.INTERNAL_OPERATIONS_PROFILE_ID);

        DevModule.run(null, new String[]{
                "system",
                "run-update",
                updateClass
        }, customArgs);
    }
}