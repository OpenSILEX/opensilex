/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.utils;

import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;

/**
 *
 * @author vincent
 * https://www.baeldung.com/java-annotation-processing-builder
 */

@SupportedAnnotationTypes({
    "org.opensilex.config.ConfigDescription",
    "org.opensilex.sparql.annotations.*",
    "org.opensilex.server.validation.*"
})
public class DynamicAnnotationProcessor extends AbstractProcessor {
 
    @Override
    public boolean process(Set<? extends TypeElement> annotations, 
      RoundEnvironment roundEnv) {
        return false;
    }
}