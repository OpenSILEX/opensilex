/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.theme;

import java.nio.file.Path;

public class ThemeBuilder {
    
    private final String themeName;
    
    private final Path themePath;
    
    public ThemeBuilder(String themeName, Path themePath) {
        this.themeName = themeName;
        this.themePath = themePath;
    }
    
    public void build() {
        // Read yml
        
        // Compile scss if needed
        
        // Minify all css
    }
}
