package org.opensilex.security.person.api;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.opensilex.server.exceptions.BadGatewayException;
import org.opensilex.server.exceptions.NotFoundException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ORCIDClientTest {

    private static Response responseMock;

    @BeforeClass
    public static void createMockedResponse(){
        responseMock = mock(Response.class);
    }

    @Test
    public void getRecordThrowsNotFoundExceptionIf404Response(){
        ORCIDClient orcidClientSpy = spy(ORCIDClient.class);
        when(responseMock.getStatus()).thenReturn(Response.Status.NOT_FOUND.getStatusCode());
        doReturn(responseMock).when(orcidClientSpy).sendGetRequest(anyString());

        assertThrows("this case should throw a not found exception", NotFoundException.class , () -> orcidClientSpy.getRecord("any"));
    }

    @Test
    public void getRecordWorksWithA200Response(){
        when(responseMock.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        when(responseMock.readEntity(String.class)).thenReturn("{}");

        ORCIDClient orcidClientSpy = spy(ORCIDClient.class);
        doReturn(responseMock).when(orcidClientSpy).sendGetRequest(anyString());

        String expectedString = "mocked result";
        try (MockedConstruction<OrcidRecordDTO> mockedConstruction = mockConstruction(OrcidRecordDTO.class)) {
            // Vous pouvez maintenant mocker les méthodes de l'objet construit
            OrcidRecordDTO recordThatShouldBeMocked = orcidClientSpy.getRecord("");
            OrcidRecordDTO orcidRecordDTOMock = mockedConstruction.constructed().get(0);
            when(orcidRecordDTOMock.getFirstName()).thenReturn(expectedString);
            assertEquals(expectedString, recordThatShouldBeMocked.getFirstName());
        }
    }

    @Test
    public void OrcidExistsTrueIf200(){
        ORCIDClient orcidClientSpy = spy(ORCIDClient.class);
        when(responseMock.getStatus()).thenReturn(Response.Status.OK.getStatusCode());
        doReturn(responseMock).when(orcidClientSpy).sendGetRequest(anyString());

        assertTrue(orcidClientSpy.orcidExists(""));
    }

    @Test
    public void OrcidExistsFalseIf404(){
        ORCIDClient orcidClientSpy = spy(ORCIDClient.class);
        when(responseMock.getStatus()).thenReturn(Response.Status.NOT_FOUND.getStatusCode());
        doReturn(responseMock).when(orcidClientSpy).sendGetRequest(anyString());

        assertFalse(orcidClientSpy.orcidExists(""));
    }

    @Test
    public void OrcidExistsThrowsError(){
        ORCIDClient orcidClientSpy = spy(ORCIDClient.class);
        ResponseBuilder responseBuilder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
        Response response = responseBuilder.build();
        doReturn(response).when(orcidClientSpy).sendGetRequest(anyString());

        assertThrows("If the response is not a 200 or 404, method should raise an exception", BadGatewayException.class, () -> orcidClientSpy.orcidExists(""));
    }

}