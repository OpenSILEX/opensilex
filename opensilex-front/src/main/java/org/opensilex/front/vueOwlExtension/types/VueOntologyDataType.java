/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.front.vueOwlExtension.types;


/**
 * A specialization of {@link VueOntologyType} used to handle primitives value
 * @author vmigot
 */
public interface VueOntologyDataType extends VueOntologyType {

    String getLabelKey();
}
