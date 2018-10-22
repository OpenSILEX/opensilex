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
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Check the following rule : the provenance date must not be null if the provenance uri is not set.
 * @author Arnaud Charleroy <arnaud.charleroy@inra.fr>, Morgane Vidal <morgane.vidal@inra.fr>
 * @update [Morgane Vidal] 10 Oct, 2018 : remove the date validation (the @Date must already be used on the date field). 
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = ProvenanceDateValidator.class)
@Documented
public @interface ProvenanceDate {

    String message() default "The provenance date must be filled if the uri is not set.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
