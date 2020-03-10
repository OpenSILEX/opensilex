/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.theme;

import java.util.List;

public interface ThemeConfig {
    
    String favicon();
    
    List<String> stylesheets();
    
    List<FontConfig> fonts();
}
