//******************************************************************************
//                                        ProvenanceDate.java
// SILEX-PHIS
// Copyright © INRA 2018
// Creation date: 21, Jun 2018
// Contact: arnaud.charleroy@inra.fr, morgane.vidal@inra.fr, anne.tireau@inra.fr, 
// pascal.neveu@inra.fr
//******************************************************************************
package phis2ws.service.resources.validation.interfaces;

import phis2ws.service.resources.validation.validators.ProvenanceDateValidator;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;
import phis2ws.service.configuration.DateFormat;

/**
 * Validate date within a provenance class instance.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
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
