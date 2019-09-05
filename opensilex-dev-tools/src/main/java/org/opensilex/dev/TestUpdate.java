/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.dev;

import opensilex.service.updates.U_20190904_ProjectMigration;
import org.opensilex.OpenSilex;
import org.opensilex.module.ModuleUpdate;

/**
 *
 * @author vincent
 */
public class TestUpdate {
    
    public static void main(String[] args) throws Exception {
        
        String configFile = TestUpdate.class.getClassLoader().getResource("./opensilex.yml").getPath();
        
        // TODO: Use environment variables instead
        OpenSilex.setup(new String[] {
            "--" + OpenSilex.PROFILE_ID_ARG_KEY + "=" + OpenSilex.DEV_PROFILE_ID,
            "--" + OpenSilex.CONFIG_FILE_ARG_KEY + "=" + configFile
        });
        
        Class<? extends ModuleUpdate> updateClass = U_20190904_ProjectMigration.class;
        
        ModuleUpdate updateInstance = updateClass.getConstructor().newInstance();
        
        updateInstance.execute();
    }
}
