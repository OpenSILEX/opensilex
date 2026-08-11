package org.opensilex.unit.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.Test;
import org.opensilex.unit.test.AbstractUnitTest;
import org.opensilex.utils.SwaggerAPIGenerator;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static org.junit.Assert.*;

public class OpenApiValidationTest extends AbstractUnitTest {

    @Tag(name = "TestAPI", description = "Test API for OpenAPI validation")
    @Path("/test")
    public static class TestEndpoint {
        @GET
        @Operation(summary = "Test method")
        @Produces(MediaType.APPLICATION_JSON)
        public String getTest() {
            return "ok";
        }
    }

    @Test
    public void testOpenApiSpecGeneration() throws Exception {
        OpenAPI openAPI = SwaggerAPIGenerator.getFullApi(opensilex.getReflections());
        assertNotNull("Generated OpenAPI object should not be null", openAPI);
        assertNotNull("OpenAPI paths should not be null", openAPI.getPaths());
        assertNotNull("OpenAPI components schemas should not be null", openAPI.getComponents().getSchemas());
    }

    @Test
    public void testAllRestEndpointsHaveOpenApiAnnotations() {
        var pathClasses = opensilex.getReflections().getTypesAnnotatedWith(Path.class);
        for (Class<?> clazz : pathClasses) {
            assertTrue("Class " + clazz.getName() + " should be annotated with @Tag",
                    clazz.isAnnotationPresent(Tag.class));
        }
    }

    @Test
    public void testRestApplicationInitOpenApi() throws Exception {
        org.opensilex.server.rest.RestApplication app = new org.opensilex.server.rest.RestApplication(opensilex);
        assertNotNull("RestApplication instance should not be null", app);
    }
}
