//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.core.variable.bll;

import org.apache.commons.collections4.CollectionUtils;
import org.opensilex.core.CoreModule;
import org.opensilex.core.external.opensilex.SharedResourceInstanceService;
import org.opensilex.core.variable.api.BaseVariableDetailsDTO;
import org.opensilex.core.variable.api.VariableAPI;
import org.opensilex.core.variable.api.VariableDetailsDTO;
import org.opensilex.core.variable.api.characteristic.CharacteristicAPI;
import org.opensilex.core.variable.api.characteristic.CharacteristicDetailsDTO;
import org.opensilex.core.variable.api.entity.EntityAPI;
import org.opensilex.core.variable.api.entity.EntityDetailsDTO;
import org.opensilex.core.variable.api.entityOfInterest.InterestEntityAPI;
import org.opensilex.core.variable.api.entityOfInterest.InterestEntityDetailsDTO;
import org.opensilex.core.variable.api.method.MethodAPI;
import org.opensilex.core.variable.api.method.MethodDetailsDTO;
import org.opensilex.core.variable.api.unit.UnitAPI;
import org.opensilex.core.variable.api.unit.UnitDetailsDTO;
import org.opensilex.core.variable.dal.BaseVariableDAO;
import org.opensilex.core.variable.dal.BaseVariableModel;
import org.opensilex.core.variable.dal.CharacteristicModel;
import org.opensilex.core.variable.dal.EntityModel;
import org.opensilex.core.variable.dal.InterestEntityModel;
import org.opensilex.core.variable.dal.MethodModel;
import org.opensilex.core.variable.dal.UnitModel;
import org.opensilex.core.variable.dal.VariableModel;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.utils.ListWithPagination;

import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Copies variables from a shared resource instance into this one, components included.
 * <p>
 * A variable cannot be created without its entity, characteristic, method and unit — the model
 * requires all four — so copying a variable means copying up to five resources, and either all of
 * them arrive or none does. That is why the whole thing runs in one transaction.
 * <p>
 * The logic used to live inside {@link VariableAPI} and was therefore reachable only through HTTP.
 * It sits here so that any caller in the process can reuse it rather than reimplement a
 * transaction that has five ways to go half done.
 *
 * @author Arnaud Charleroy
 */
public class VariableCopyLogic {

    private final SPARQLService sparql;
    private final CoreModule coreModule;
    private final AccountModel currentUser;

    public VariableCopyLogic(SPARQLService sparql, CoreModule coreModule, AccountModel currentUser) {
        this.sparql = sparql;
        this.coreModule = coreModule;
        this.currentUser = currentUser;
    }

    /**
     * Copies the given variables and whatever they are made of that is not here yet.
     *
     * @param sharedResourceInstance the instance to copy from, as declared in the configuration
     * @param uris                   the variables to copy, by their URI on that instance
     * @return what was created, empty when none of the URIs is unknown here
     */
    public VariableCopyResult copy(URI sharedResourceInstance, List<URI> uris) throws Exception {
        SharedResourceInstanceService service = new SharedResourceInstanceService(
                coreModule.getSharedResourceInstanceConfiguration(sharedResourceInstance),
                currentUser.getLanguage());

        // Keeps only the URIs this instance does not already carry, so copying twice is harmless.
        Set<URI> variableSetToCopy = sparql.getExistingUris(VariableModel.class, uris, false);

        VariableCopyResult result = new VariableCopyResult();
        if (CollectionUtils.isEmpty(variableSetToCopy)) {
            return result;
        }

        ListWithPagination<VariableDetailsDTO> variableDetailsList = service.getListByURI(
                Paths.get(VariableAPI.PATH, VariableAPI.GET_BY_URIS_PATH).toString(),
                VariableAPI.GET_BY_URIS_URI_PARAM, variableSetToCopy, VariableDetailsDTO.class);

        List<URI> entityUris = new ArrayList<>();
        List<URI> entityOfInterestUris = new ArrayList<>();
        List<URI> characteristicUris = new ArrayList<>();
        List<URI> methodUris = new ArrayList<>();
        List<URI> unitUris = new ArrayList<>();

        for (VariableDetailsDTO variable : variableDetailsList.getList()) {
            entityUris.add(variable.getEntity().getUri());
            if (variable.getEntityOfInterest() != null) {
                entityOfInterestUris.add(variable.getEntityOfInterest().getUri());
            }
            characteristicUris.add(variable.getCharacteristic().getUri());
            methodUris.add(variable.getMethod().getUri());
            unitUris.add(variable.getUnit().getUri());
        }

        try {
            sparql.startTransaction();

            result.setEntityUris(new ArrayList<>(createIfMissing(
                    EntityModel.class, EntityDetailsDTO.class, entityUris, service, EntityAPI.PATH)));
            result.setInterestEntityUris(new ArrayList<>(createIfMissing(
                    InterestEntityModel.class, InterestEntityDetailsDTO.class, entityOfInterestUris,
                    service, InterestEntityAPI.PATH)));
            result.setCharacteristicUris(new ArrayList<>(createIfMissing(
                    CharacteristicModel.class, CharacteristicDetailsDTO.class, characteristicUris,
                    service, CharacteristicAPI.PATH)));
            result.setMethodUris(new ArrayList<>(createIfMissing(
                    MethodModel.class, MethodDetailsDTO.class, methodUris, service, MethodAPI.PATH)));
            result.setUnitUris(new ArrayList<>(createIfMissing(
                    UnitModel.class, UnitDetailsDTO.class, unitUris, service, UnitAPI.PATH)));

            createBaseVariable(VariableModel.class, variableDetailsList.getList(), service);

            result.setVariableUris(new ArrayList<>(variableSetToCopy));

            sparql.commitTransaction();
            return result;
        } catch (Exception e) {
            sparql.rollbackTransaction();
            throw e;
        }
    }

    private <T extends BaseVariableModel<T>, U extends BaseVariableDetailsDTO<T>> Set<URI> createIfMissing(
            Class<T> modelClass, Class<U> detailsClass, Collection<URI> uriCollection,
            SharedResourceInstanceService service, String apiPath) throws Exception {

        Set<URI> missingUriSet = sparql.getExistingUris(modelClass, uriCollection, false);

        List<U> detailsList = service.getListByURI(
                        Paths.get(apiPath, VariableAPI.GET_BY_URIS_PATH).toString(),
                        VariableAPI.GET_BY_URIS_URI_PARAM, missingUriSet, detailsClass)
                .getList();

        createBaseVariable(modelClass, detailsList, service);

        return missingUriSet;
    }

    private <T extends BaseVariableModel<T>, U extends BaseVariableDetailsDTO<T>> void createBaseVariable(
            Class<T> modelClass, Collection<U> detailsCollection,
            SharedResourceInstanceService service) throws Exception {

        BaseVariableDAO<T> dao = new BaseVariableDAO<>(modelClass, sparql);

        List<T> modelList = detailsCollection.stream().map(detailsDto -> {
            T model = detailsDto.toModel();
            model.setPublisher(currentUser.getUri());
            model.setFromSharedResourceInstance(service.getSharedResourceInstanceURI());
            return model;
        }).collect(Collectors.toList());

        dao.createList(modelList);
    }
}
