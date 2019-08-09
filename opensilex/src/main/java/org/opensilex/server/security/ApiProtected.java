/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensilex.server.security;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@ApiImplicitParams({
    @ApiImplicitParam(
            name = ApiProtected.HEADER_NAME, 
            required = true,
            dataType = "string", 
            paramType = "header",
            value = "Authentication token")
})
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiProtected {
    
    public final static String HEADER_NAME = "Authorization";
        

}