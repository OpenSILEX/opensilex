//**********************************************************************************************
//                                       SessionInject.java 
//
// Author(s): Arnaud CHARLEROY
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2016
// Creation date: august 2016
// Contact:arnaud.charleroy@supagro.inra.fr, anne.tireau@supagro.inra.fr, pascal.neveu@supagro.inra.fr
// Last modification date:  October, 2016
// Subject: Annotation created to inject user data in webservice resources classes
// Custom Injection Provider
// see https://jersey.java.net/documentation/latest/ioc.html
//***********************************************************************************************
package phis2ws.service.injection;

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
 * @author A. CHARLEROY
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SessionInject {
}
