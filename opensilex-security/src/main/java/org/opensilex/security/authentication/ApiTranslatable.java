//******************************************************************************
//                          ApiTranslatable.java
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.security.authentication;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.ws.rs.core.HttpHeaders;
import org.opensilex.OpenSilex;

@Parameters({
    @Parameter(name = HttpHeaders.ACCEPT_LANGUAGE,
            schema = @Schema(type = "string"),
            in = ParameterIn.HEADER,
            description = "Request accepted language",
            example = OpenSilex.DEFAULT_LANGUAGE)
})
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiTranslatable {

}
