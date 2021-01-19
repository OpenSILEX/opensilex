/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.rest;

import org.apache.catalina.valves.rewrite.RewriteMap;

/**
 *
 * @author vince
 */
public class RestRewriteMap implements RewriteMap {

    @Override
    public String setParameters(String params) {
        return null;
    }

    @Override
    public String lookup(String key) {
        if (key == null) {
            return null;
        }

        String result = "";
        String[] parts = key.split("/rest/", 2);
        if (parts.length == 2) {
            result = "/rest/" + parts[1];
        } else {
            result = key;
        }

        return result;

    }

}
