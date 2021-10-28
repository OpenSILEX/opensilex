package org.opensilex.dev;

/**
 * @author rcolin
 */
public class RunUpdate {

    public static void main(String[] args) throws Exception {

        DevModule.run(new String[]{
                "system",
                "run-update",
                "org.opensilex.dev.migration.GraphAndCollectionMigration"
        });
    }
}