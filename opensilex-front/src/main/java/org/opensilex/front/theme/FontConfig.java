/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.theme;

import java.util.Map;

/**
 *
 * @author vidalmor
 */
public interface FontConfig {
    
    String family();
    
    String style();
    
    String weight();
    
    String url();
    
    Map<String, String> src();
}
