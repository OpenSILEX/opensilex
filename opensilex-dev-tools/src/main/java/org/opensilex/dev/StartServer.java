package org.opensilex.dev;

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
        /**
     * Utility static function to start opensielx server with modules in debug mode
     *
     * @param args
     */
    public static void main(String[] args) {
        MainCommand.main(new String[]{
            "server",
            "start",
            "--profile=dev"
        });
    }
}
