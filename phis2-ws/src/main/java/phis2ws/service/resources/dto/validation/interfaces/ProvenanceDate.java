//******************************************************************************
//                                       ProvenanceDate.java
//
// Author(s): Arnaud Charleroy <arnaud.charleroy@inra.fr>
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2018
// Creation date: 21 juin 2018
// Contact: arnaud.charleroy@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  25 juin 2018
// Subject: Validate date within a provenance class instance
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
import phis2ws.service.configuration.DateFormat;

/**
 * Validate date within a provenance class instance
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ProvenanceDateValidator.class)
@Documented
public @interface ProvenanceDate {

    String message() default "The provenance date must be filled if the uri is not set. This string is not a valid date. Excepted format : {value}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    DateFormat value() default DateFormat.YMDHMSZ;
}
