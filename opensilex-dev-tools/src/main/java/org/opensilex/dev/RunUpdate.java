package org.opensilex.dev;

/**
 * @author rcolin
 */
public class RunUpdate {

    public static void main(String[] args) throws Exception {

        if(args == null || args.length == 0){
            throw new IllegalArgumentException("RTFM. Usage: <update_class_path>.  No argument provided, please provide a valid update class");
        }
        String updateClass = args[0];

        DevModule.run(new String[]{
                "system",
                "run-update",
                updateClass
        });
    }
}