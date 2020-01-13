/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.api;

import java.util.ArrayList;
import java.util.List;
import org.opensilex.front.theme.FontConfig;
import org.opensilex.front.theme.ThemeConfig;

/**
 *
 * @author vidalmor
 */
public class ThemeConfigDTO {
    
    boolean hasStyle;
    
    List<FontConfigDTO> fonts;

    public boolean isHasStyle() {
        return hasStyle;
    }

    public void setHasStyle(boolean hasStyle) {
        this.hasStyle = hasStyle;
    }

    public List<FontConfigDTO> getFonts() {
        return fonts;
    }

    public void setFonts(List<FontConfigDTO> fonts) {
        this.fonts = fonts;
    }
    
    public static ThemeConfigDTO fromThemeConfig(ThemeConfig config) {
        ThemeConfigDTO dto = new ThemeConfigDTO();
        dto.setHasStyle(config.stylesheets().size() > 0);
        
        List<FontConfigDTO> fonts = new ArrayList<>();
        config.fonts().forEach((FontConfig f) -> {
            fonts.add(FontConfigDTO.fromFontConfig(f));
        });
        dto.setFonts(fonts);
        return dto;
    }
}
