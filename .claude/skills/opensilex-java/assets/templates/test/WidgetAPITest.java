package org.opensilex.core.widget.api;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;
import org.opensilex.core.widget.dal.WidgetModel;
import org.opensilex.integration.test.ServiceDescription;
import org.opensilex.integration.test.security.AbstractSecurityIntegrationTest;
import org.opensilex.server.response.PaginatedListResponse;
import org.opensilex.server.response.SingleObjectResponse;
import org.opensilex.sparql.model.SPARQLResourceModel;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Integration test of {@link WidgetAPI}. {@code ServiceDescription} ties each call to the real Java method, so a renamed
 * or re-signatured endpoint breaks this test at class-load time instead of silently testing a stale URL.
 */
public class WidgetAPITest extends AbstractSecurityIntegrationTest {

    public static final String PATH = "/core/widgets";

    public static ServiceDescription create;
    public static ServiceDescription update;
    public static ServiceDescription get;
    public static ServiceDescription delete;
    public static ServiceDescription search;

    static {
        try {
            create = new ServiceDescription(WidgetAPI.class.getMethod("createWidget", WidgetCreationDTO.class), PATH);
            update = new ServiceDescription(WidgetAPI.class.getMethod("updateWidget", WidgetUpdateDTO.class), PATH);
            get = new ServiceDescription(WidgetAPI.class.getMethod("getWidget", URI.class), PATH + "/{uri}");
            delete = new ServiceDescription(WidgetAPI.class.getMethod("deleteWidget", URI.class), PATH + "/{uri}");
            search = new ServiceDescription(
                    WidgetAPI.class.getMethod("searchWidgets", String.class, List.class, int.class, int.class),
                    PATH
            );
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static WidgetCreationDTO creationDTO(String name) {
        WidgetCreationDTO dto = new WidgetCreationDTO();
        dto.setName(name);
        dto.setDescription("Description of " + name);
        return dto;
    }

    private static WidgetUpdateDTO updateDTO(String name) {
        WidgetUpdateDTO dto = new WidgetUpdateDTO();
        dto.setName(name);
        dto.setDescription("New description of " + name);
        return dto;
    }

    @Test
    public void testWidgetBasicCRUDAsAdmin() throws Exception {
        testBasicCRUDAsAdmin(
                create, get, update, delete,
                creationDTO("Widget 1"), updateDTO("Widget 1 renamed"),
                new TypeReference<SingleObjectResponse<WidgetDTO>>() {
                }
        );
    }

    @Test
    public void createWithExistingNameReturnsConflict() throws Exception {
        new UserCallBuilder(create)
                .setBody(creationDTO("Duplicated"))
                .buildAdmin()
                .executeCallAndAssertStatus(Response.Status.CREATED);

        new UserCallBuilder(create)
                .setBody(creationDTO("Duplicated"))
                .buildAdmin()
                .executeCallAndAssertStatus(Response.Status.CONFLICT);
    }

    @Test
    public void searchByNamePatternReturnsOnlyMatchingWidgets() throws Exception {
        for (String name : List.of("Widget A", "Widget B", "Other")) {
            new UserCallBuilder(create)
                    .setBody(creationDTO(name))
                    .buildAdmin()
                    .executeCallAndAssertStatus(Response.Status.CREATED);
        }

        List<WidgetDTO> found = new UserCallBuilder(search)
                .addParam("name", "Widget.*")
                .buildAdmin()
                .executeCallAndDeserialize(new TypeReference<PaginatedListResponse<WidgetDTO>>() {
                })
                .getDeserializedResponse()
                .getResult();

        assertEquals(2, found.size());
    }

    /**
     * Every model class written by the tests of this class: without it, data leaks from one method to the next.
     */
    @Override
    protected List<Class<? extends SPARQLResourceModel>> getModelsToClean() {
        List<Class<? extends SPARQLResourceModel>> models = new ArrayList<>();
        models.add(WidgetModel.class);
        return models;
    }
}
