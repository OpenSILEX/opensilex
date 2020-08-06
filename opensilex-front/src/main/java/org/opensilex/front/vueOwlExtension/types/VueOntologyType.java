/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.types;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author vmigot
 */
public interface VueOntologyType {

    public String getUri();

    public default List<String> getUriAliases() {
        return new ArrayList<>();
    }

    public String getInputComponent();

    public String getViewComponent();

}
