//******************************************************************************
//                             SessionInject.java 
// SILEX-PHIS
// Copyright © INRA 2016
// Creation date: August 2016
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package opensilex.service.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation pour recevoir les informations d'une session en cours (Ex :
 * client, adresse Ip etc ...)
 * 
 * @see https://jersey.java.net/documentation/latest/ioc.html
 * @date 07/16
 * @author A. Charleroy
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SessionInject {
}
