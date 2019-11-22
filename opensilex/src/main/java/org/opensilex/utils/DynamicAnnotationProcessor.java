//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.utils;

import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;

// TODO Vérifier si ça sert à quelque chose

/**
 *
 * @author vincent
 * https://www.baeldung.com/java-annotation-processing-builder
 */

@SupportedAnnotationTypes("*")
public class DynamicAnnotationProcessor extends AbstractProcessor {
 
    @Override
    public boolean process(Set<? extends TypeElement> annotations, 
      RoundEnvironment roundEnv) {
        return false;
    }
}