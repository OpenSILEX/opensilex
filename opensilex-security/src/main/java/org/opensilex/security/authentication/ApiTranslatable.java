//******************************************************************************
//                          ApiProtected.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.authentication;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.ws.rs.core.HttpHeaders;
import org.opensilex.OpenSilex;

@ApiImplicitParams({
    @ApiImplicitParam(
            name = HttpHeaders.ACCEPT_LANGUAGE,
            dataType = "string",
            paramType = "header",
            value = "Request accepted language",
            example = OpenSilex.DEFAULT_LANGUAGE
    )
})
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiTranslatable {

}
