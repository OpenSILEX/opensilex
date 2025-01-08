package org.opensilex.security.person.api;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;

/**
 * This class test the deserialization of the data (the record) sent by ORCID.
 * We can't test directly with a call to the API because we can't choose an ORCID and pray for it to not change its data in the futur.
 * So we use a file with an exemple of what an ORCID record could be in the V3 of the ORCID API.
 */
public class OrcidRecordDTOTest {

    private static OrcidRecordDTO jcvdTestRecord;

    @BeforeClass
    public static void insantiateJcvdTestRecord() {
        String jsonFilePath = "orcid_record_sample_APIV3_for_test.json";

        ClassLoader classLoader = OrcidRecordDTOTest.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(jsonFilePath);

        Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
        String jsonContent = scanner.hasNext() ? scanner.next() : "";

        JsonReader jsonReader = Json.createReader(new StringReader(jsonContent));
        JsonObject recordJsonObject = jsonReader.readObject();

        jcvdTestRecord = new OrcidRecordDTO(recordJsonObject);
    }

    @Test
    public void constructorDeserializeFirstName(){
        assertEquals("Jean Claude", jcvdTestRecord.firstName);
    }

    @Test
    public void constructorDeserializeLastName(){
        assertEquals("Ven Damme", jcvdTestRecord.lastName);
    }

    @Test
    public void constructorDeserializeEmail(){
        List<String> expectedEmails = new ArrayList<>();
        expectedEmails.add("jcvd@jcvd.be");

        assertEquals(expectedEmails, jcvdTestRecord.emails);
    }

    @Test
    public void constructorDeserializeOrganizations(){
        List<String> expectedOrganizations = new ArrayList<>();
        expectedOrganizations.add("Belgywood cinemas");
        expectedOrganizations.add("Le DOJOnclaude Ven Damme");

        assertEquals(expectedOrganizations, jcvdTestRecord.organizations);
    }

}