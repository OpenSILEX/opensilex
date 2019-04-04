//******************************************************************************
//                             SessionInject.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: Sept. 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation about the current session information. 
 * @see https://jersey.java.net/documentation/latest/ioc.html
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SessionInject {
}
