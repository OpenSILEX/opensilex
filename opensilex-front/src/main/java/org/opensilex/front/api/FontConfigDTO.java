/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.api;

import java.util.Map;
import org.opensilex.front.theme.FontConfig;

/**
 *
 * @author vidalmor
 */
public class FontConfigDTO {

    private String family;
    
    private String style;
    
    private String weight;
    
    private String url;
    
    private Map<String, String> src;

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getSrc() {
        return src;
    }

    public void setSrc(Map<String, String> src) {
        this.src = src;
    }

    public static FontConfigDTO fromFontConfig(FontConfig f) {
        FontConfigDTO dto = new FontConfigDTO();
        
        dto.setFamily(f.family());
        dto.setStyle(f.style());
        dto.setWeight(f.weight());
        dto.setSrc(f.src());
        dto.setUrl(f.url());
        
        return dto;
    }
}
