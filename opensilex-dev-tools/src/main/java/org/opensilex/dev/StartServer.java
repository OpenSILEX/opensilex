package org.opensilex.dev;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.opensilex.OpenSilex;
import org.opensilex.cli.MainCommand;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author vincent
 */
public class StartServer {

    public static void main(String[] args) throws IOException {
        
        File configFile = new File(StartServer.class.getClassLoader().getResource("./config/opensilex.yml").getPath());
        OpenSilex.setup(new HashMap<String, String>() {
            {
                put(OpenSilex.PROFILE_ID_ARG_KEY, OpenSilex.DEV_PROFILE_ID);
                put(OpenSilex.CONFIG_FILE_ARG_KEY, configFile.getAbsolutePath());
            }
        });

        MainCommand.run(new String[]{
            "server",
            "start"
        });

    }
}
