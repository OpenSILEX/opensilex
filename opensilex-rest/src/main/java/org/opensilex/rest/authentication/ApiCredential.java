//******************************************************************************
//                          ApiProtected.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.rest.authentication;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <pre>
 * Annotation use to add header token protection for API service.
 * 
 * Protection mechanism is implemented in {@code org.opensilex.server.security.AuthenticationFilter}
 * </pre>
 * 
 * @see org.opensilex.rest.authentication.AuthenticationFilter
 * @author Vincent Migot
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiCredential {

    public String groupId();
    
    public String groupLabelKey();
    
    public String credentialId();
    
    public String credentialLabelKey();

    public boolean hide() default false;
    
}
