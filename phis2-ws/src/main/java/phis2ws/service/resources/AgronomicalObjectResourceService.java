//**********************************************************************************************
//                               AgronomicalObjectResourceService.java 
//
// Author(s): Morgane Vidal
// PHIS-SILEX version 1.0
// Copyright © - INRA - 2017
// Creation date: august 2017
// Contact: morgane.vidal@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
// Last modification date:  August, 30 2017 - update post - génération des uris
// Subject: Represents the agronomical object data service
//***********************************************************************************************
package phis2ws.service.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import phis2ws.service.PropertiesFileManager;
import phis2ws.service.authentication.Session;
import phis2ws.service.configuration.DefaultBrapiPaginationValues;
import phis2ws.service.configuration.GlobalWebserviceValues;
import phis2ws.service.dao.phis.AgronomicalObjectDao;
import phis2ws.service.dao.sesame.AgronomicalObjectDaoSesame;
import phis2ws.service.documentation.DocumentationAnnotation;
import phis2ws.service.documentation.StatusCodeMsg;
import phis2ws.service.injection.SessionInject;
import phis2ws.service.resources.dto.AgronomicalObjectDTO;
import phis2ws.service.utils.POSTResultsReturn;
import phis2ws.service.view.brapi.Status;
import phis2ws.service.view.brapi.form.AbstractResultForm;
import phis2ws.service.view.brapi.form.ResponseFormAgronomicalObject;
import phis2ws.service.view.brapi.form.ResponseFormPOST;
import phis2ws.service.view.model.phis.AgronomicalObject;

@Api("/agronomicalObjects")
@Path("agronomicalObjects")
public class AgronomicalObjectResourceService {

    final static Logger LOGGER = LoggerFactory.getLogger(AgronomicalObjectResourceService.class);
    
    //Session de l'utilisateur
    @SessionInject
    Session userSession;
  
    /**
     * 
     * @param agronomicalObjectsDTO
     * @return Une liste d'agronomical objects pour lesquels on a généré l'uri.
     */
    private ArrayList<AgronomicalObject> generateURIs(ArrayList<AgronomicalObjectDTO> agronomicalObjectsDTO) {
        String actualYear = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
        String baseURI = PropertiesFileManager.getConfigFileProperty("service", "baseURI");
        ArrayList<AgronomicalObject> agronomicalObjects = new ArrayList<>();
        HashMap<String, Integer> numberByYear = new HashMap<>();
        //Récupération du numéro où l'on s'est arrêté
        AgronomicalObjectDao agronomicalObjectDao = new AgronomicalObjectDao();
        String numberInYear = agronomicalObjectDao.getNumberOfAgronomicalObjectForYear(actualYear);
        numberByYear.put(actualYear, Integer.parseInt(numberInYear));        
        
        //Modification des URIs de chaque agronomicalObject
        for (AgronomicalObjectDTO agronomicalObject : agronomicalObjectsDTO) {
            String year = actualYear;
            if (agronomicalObject.getYear() != null) {
                year = actualYear;
            }
            if (numberByYear.containsKey(year)) {
                numberByYear.put(year, numberByYear.get(year) + 1);
            } else {
                numberByYear.put(year, 1);
            }
            
            //On calcule le nombre de 0 à ajouter devant le numéro de l'ao (pour la cohérance des URI).
            //au 30/08/17, il faut en tout 6 chiffres (ex : 000001)
            String nbYear = numberByYear.get(year).toString();
            while (nbYear.length() < 6) {
                nbYear = "0" + nbYear;
            }
            String uniqId = "o" + year.substring(2, 4) + nbYear;
            AgronomicalObject ao = agronomicalObject.createObjectFromDTO();
            ao.setUri(baseURI + year + "/" + uniqId);
            
            agronomicalObjects.add(ao);      
        }
        
        return agronomicalObjects;
    }

    /**
     * Enregistre un ensemble d'objets agronomiques dans le triplestore et les associe à un essai s'il est renseigné
     * @param agronomicalObjectsDTO liste des objets agronomiques à enregistrer
     * @param context élément du contexte de la requête pour obtenir les
     *                informations de l'adresse ip de l'utilisateur
     * @return 
     */
    @POST
    @ApiOperation(value = "Post agronomical object(s)",
                  notes = "Register new agronomical object(s) in the database.")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Agronomical object(s) saved", response = ResponseFormPOST.class),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_SEND_DATA)
    })
    @ApiImplicitParams({
       @ApiImplicitParam(name = "Authorization", required = true,
                         dataType = "string", paramType = "header",
                         value = DocumentationAnnotation.ACCES_TOKEN,
                         example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response postAgronomicalObject(
            @ApiParam(value = DocumentationAnnotation.AGRONOMICAL_OBJECT_POST_DATA_DEFINITION) ArrayList<AgronomicalObjectDTO> agronomicalObjectsDTO,
            @Context HttpServletRequest context) {
        AbstractResultForm postResponse = null;
        //S'il y a au moins un objet agronomique
        if (!agronomicalObjectsDTO.isEmpty()) {
            try {
                AgronomicalObjectDaoSesame agronomicalObjectDaoSesame = new AgronomicalObjectDaoSesame();
                if (context.getRemoteAddr() != null) {
                    agronomicalObjectDaoSesame.remoteUserAdress = context.getRemoteAddr();
                }
                
                agronomicalObjectDaoSesame.user = userSession.getUser();
                
                //Génération de toutes les URI
                ArrayList<AgronomicalObject> agronomicalObjects = generateURIs(agronomicalObjectsDTO);
                
                //Vérification des objets agronomiques et insertion en sesame
                POSTResultsReturn resultSesame = agronomicalObjectDaoSesame.checkAndInsert(agronomicalObjects, agronomicalObjectsDTO);
                if (resultSesame.getHttpStatus().equals(Response.Status.CREATED)) {
                    //Code 201, agronomical objects insérés
                    AgronomicalObjectDao agronomicalObjectDao = new AgronomicalObjectDao();
                    POSTResultsReturn result = agronomicalObjectDao.checkAndInsertListAO(agronomicalObjects, agronomicalObjectsDTO);
                    if (result.getHttpStatus().equals(Response.Status.CREATED)) { //201, agronomical objects insérés
                        postResponse = new ResponseFormPOST(result.statusList);
                        return Response.status(result.getHttpStatus()).entity(postResponse).build();
                    } else if (result.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                            || result.getHttpStatus().equals(Response.Status.OK)
                            || result.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                        postResponse = new ResponseFormPOST(result.statusList);
                    }
                    return Response.status(result.getHttpStatus()).entity(postResponse).build();
                } else if (resultSesame.getHttpStatus().equals(Response.Status.BAD_REQUEST)
                        || resultSesame.getHttpStatus().equals(Response.Status.OK)
                        || resultSesame.getHttpStatus().equals(Response.Status.INTERNAL_SERVER_ERROR)) {
                    postResponse = new ResponseFormPOST(resultSesame.statusList);
                }
                return Response.status(resultSesame.getHttpStatus()).entity(postResponse).build();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
                return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
            }
        } else {
            postResponse = new ResponseFormPOST(new Status("Request error", StatusCodeMsg.ERR, "Empty agronomical objects list"));
            return Response.status(Response.Status.BAD_REQUEST).entity(postResponse).build();
        }
    }
    
    private Response noResultFound(ResponseFormAgronomicalObject getResponse, ArrayList<Status> insertStatusList) {
        insertStatusList.add(new Status("No results", StatusCodeMsg.INFO, "No results for the agronomical objects"));
        getResponse.setStatus(insertStatusList);
        return Response.status(Response.Status.NOT_FOUND).entity(getResponse).build();
    }
    
    /**
     * Collecte les données issues d'une requête de l'utilisateur (recherche d'objets agronomiques)
     * @param agronomicalObjectDaoSesame
     * @return la réponse pour l'utilisateur. Contient la liste des objets agronomiques
     *         correspondant à la recherche
     * SILEX:TODO
     * on ne peut chercher que par uri et experiment uri. Il faudra ajouter d'autres critères
     * on récupère une liste d'ao composés d'un uri et d'un type. Il faudra compléter ce retour 
     * \SILEX:TODO
     */
    private Response getAgronomicalObjectsData(AgronomicalObjectDaoSesame agronomicalObjectDaoSesame) {
        ArrayList<AgronomicalObject> agronomicalObjects;
        ArrayList<Status> statusList = new ArrayList<>();
        ResponseFormAgronomicalObject getResponse;
        
        agronomicalObjects = agronomicalObjectDaoSesame.allPaginate();
        
        if (agronomicalObjects == null) {
            getResponse = new ResponseFormAgronomicalObject(0, 0, agronomicalObjects, true);
            return noResultFound(getResponse, statusList);
        } else if (!agronomicalObjects.isEmpty()) {
            getResponse = new ResponseFormAgronomicalObject(agronomicalObjectDaoSesame.getPageSize(), agronomicalObjectDaoSesame.getPage(), agronomicalObjects, false);
            if (getResponse.getResult().dataSize() == 0) {
                return noResultFound(getResponse, statusList);
            } else {
                getResponse.setStatus(statusList);
                return Response.status(Response.Status.OK).entity(getResponse).build();
            }
        } else {
            getResponse = new ResponseFormAgronomicalObject(0, 0, agronomicalObjects, true);
            return noResultFound(getResponse, statusList);
        }
    }
    
    @GET
    @ApiOperation(value = "Get all AgronomicalObjects corresponding to the searched params given",
                  notes = "Retrieve all agronomical objects authorized for the user corresponding to the user corresponding to the searched params given")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Retrieve all agronomical objects", response = AgronomicalObject.class, responseContainer = "List"),
        @ApiResponse(code = 400, message = DocumentationAnnotation.BAD_USER_INFORMATION),
        @ApiResponse(code = 401, message = DocumentationAnnotation.USER_NOT_AUTHORIZED),
        @ApiResponse(code = 500, message = DocumentationAnnotation.ERROR_FETCH_DATA)
    })
    @ApiImplicitParams({
         @ApiImplicitParam(name = "Authorization", required = true,
                         dataType = "string", paramType = "header",
                         value = DocumentationAnnotation.ACCES_TOKEN,
                         example = GlobalWebserviceValues.AUTHENTICATION_SCHEME + " ")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAgronomicalObjectsBySearch(
        @ApiParam(value = DocumentationAnnotation.PAGE_SIZE) @QueryParam("pageSize") @DefaultValue(DefaultBrapiPaginationValues.PAGE_SIZE) int limit,
        @ApiParam(value = DocumentationAnnotation.PAGE) @QueryParam("page") @DefaultValue(DefaultBrapiPaginationValues.PAGE) int page,
        @ApiParam(value = "Search by URI", example = DocumentationAnnotation.EXAMPLE_AGRONOMICAL_OBJECT_URI) @QueryParam("uri") String uri,
        @ApiParam(value = "Search by experiment URI", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_URI) @QueryParam("experiment") String experimentURI,
        @ApiParam(value = "Search by alias", example = DocumentationAnnotation.EXAMPLE_EXPERIMENT_ALIAS) @QueryParam("alias") String alias
    ) {
        AgronomicalObjectDaoSesame agronomicalObjectDaoSesame = new AgronomicalObjectDaoSesame();
        
        if (uri != null) {
            agronomicalObjectDaoSesame.uri = uri;
        }
        if (experimentURI != null) {
            agronomicalObjectDaoSesame.experiment = experimentURI;
        }
        if (alias != null) {
            agronomicalObjectDaoSesame.alias = alias;
        }
        
        agronomicalObjectDaoSesame.user = userSession.getUser();
        agronomicalObjectDaoSesame.setPage(page);
        agronomicalObjectDaoSesame.setPageSize(limit);
        
        return getAgronomicalObjectsData(agronomicalObjectDaoSesame);
    }
}
