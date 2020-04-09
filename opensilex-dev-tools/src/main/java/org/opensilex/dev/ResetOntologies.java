/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.dev;

/**
 *
 * @author vince
 */
public class ResetOntologies {

    public static void main(String[] args) throws Exception {
        DevModule.run(new String[]{
            "sparql",
            "reset-ontologies"
        });
    }
}
