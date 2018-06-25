//******************************************************************************
//                                       ProvenanceDateCheck.java
//
// Author(s): Arnaud Charleroy <arnaud.charleroy@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 21 juin 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  25 juin 2018
// Subject: check provenance date
//******************************************************************************
package phis2ws.service.resources.dto.validation.interfaces;

import phis2ws.service.resources.dto.validation.validators.ProvenanceDateValidator;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

/**
 *
 * @author Arnaud Charleroy<arnaud.charleroy@inra.fr>
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ProvenanceDateValidator.class)
@Documented
public @interface ProvenanceDateCheck {

    String message() default "start date must be filled if provenance is filled";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
