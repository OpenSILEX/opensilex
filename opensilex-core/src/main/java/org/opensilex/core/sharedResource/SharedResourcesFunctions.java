package org.opensilex.core.sharedResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opensilex.core.CoreModule;
import org.opensilex.sparql.service.SPARQLService;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SharedResourcesFunctions {

    public static final String LOCAL_RESOURCE = "http://localhost";

    private static SharedResourcesFunctions INSTANCE;

    public static SharedResourcesFunctions getInstance(CoreModule coreModule){
        if(INSTANCE == null){
            INSTANCE = new SharedResourcesFunctions(coreModule);
        }
        return INSTANCE;
    }

    private SharedResourcesFunctions(CoreModule coreModule) {
        this.coreModule = coreModule;
    }

    private final CoreModule coreModule;


    /*Cette fonction permet de récupérer la liste des ressources partagées disponibles indiquées dans le fichier opensilex.yml*/
    public List<SharedResourcesDTO> getAllSharedResources(

    ) throws Exception {

        //Création du dto de l'instance locale
        SharedResourcesDTO localInstance = new SharedResourcesDTO();
        localInstance.setUri(new URI(LOCAL_RESOURCE));
        localInstance.setLabel("component.sharedResources.local-instance");
        localInstance.setLocal(true);

        // création de la liste des dtos de toutes les ressources disponibles
        List<SharedResourcesDTO> sharedResourcesDTOS = new ArrayList<>();
        sharedResourcesDTOS.add(localInstance);
        sharedResourcesDTOS.addAll(coreModule.getSharedResources());

        return sharedResourcesDTOS;
    }

    /*Cette fonction renvoie la réponse (String) d'une connexion de type HttpURLConnection*/
    private String readResponse(HttpURLConnection connection)throws Exception {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream()));
        String content = "";
        String inputLine;
        while ((inputLine = in.readLine()) != null)
            content += inputLine;
        in.close();
        return content;
    }

    /*Cette fonction renvoie un token au format String en fonction de l'adresse de la ressource donnée en paramètre (ex : http://138.102.159.36:8083/rest)*/
    /*La fonction utilise le compte admin pour se connecter (admin@opensilex.org / admin)*/
    public String getToken(String urlSharedResource)throws Exception {

        //URL du service qui génère le token
        URL urlToken = new URL(urlSharedResource + "/security/authenticate");
        //création de la connexion
        HttpURLConnection connection = (HttpURLConnection) urlToken.openConnection();
        //propriétés du service
        connection.setDoOutput(true); // POST
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json");
        //paramètres du service
        String data = "{\"identifier\": \"admin@opensilex.org\", \"password\": \"admin\"}";
        byte[] out = data.getBytes(StandardCharsets.UTF_8);
        //envoi des paramètres en entrée
        OutputStream stream = connection.getOutputStream();
        stream.write(out);
        //lecture de la réponse
        String stringResponse = readResponse(connection);
        ObjectMapper mapperSearch = new ObjectMapper();
        JsonNode jsonResult = mapperSearch.readTree(stringResponse);
        //récupération du token au format String
        String token = "Bearer " + jsonResult.get("result").get("token").asText();

        connection.disconnect();
        return token;
    }

    /*Cette fonction renvoie la réponse (String) d'un service (ex : http://138.102.159.36:8083/rest/ontology/shared_resources)*/
    public String connectionToService(String urlService, String token)throws Exception {

        // connexion au service
        URL url = new URL(urlService);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Authorization", token);

        // lecture de la réponse si la connexion a fonctionné
        int statut = connection.getResponseCode();
        if (statut == 200) {
            String Response = readResponse(connection);
            connection.disconnect();
            return Response;
        }
        return null;
    }

    /*Cette fonction renvoie la réponse (JsonNode) d'un service (ex : http://138.102.159.36:8083/rest/ontology/shared_resources)*/
    public JsonNode jsonResponseToService(String urlService, String token)throws Exception {

        // récupération de la réponse du service au format String
        String Response = connectionToService(urlService,token);
        // conversion de la réponse en JsonNode
        if (Response != null) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonResult = mapper.readTree(Response);
            return jsonResult;
        }
        return null;
    }

    /*Cette fonction convertit un uri court en uri long si le prefixe de cet uri court existe dans la liste des prefixes de la bdd*/
    public URI prefixeTranslation(URI uri) throws Exception {

        // récupération des namespaces (indiqués dans la fonction getDefaultPrefixes() de SPARQLService.java)
        Map<String, String> nameSpaces = SPARQLService.getPrefixes();
        String prefixeUri = uri.getScheme();

        if (nameSpaces.get(prefixeUri) != null){
            URI translatedUri = new URI(uri.toString().replace(prefixeUri + ":",nameSpaces.get(prefixeUri)));
            return translatedUri;
        }else{
            return null;
        }
    }

}
