package org.opensilex.faidare.builder;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.vocabulary.RDF;
import org.opensilex.core.data.dal.DataDAO;
import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.ontology.Oeso;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.faidare.model.Faidarev1ContactDTO;
import org.opensilex.faidare.model.Faidarev1LastUpdateDTO;
import org.opensilex.faidare.model.Faidarev1StudyDTO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.person.dal.PersonModel;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;
import org.opensilex.sparql.exceptions.SPARQLException;
import org.opensilex.sparql.service.SPARQLResult;
import org.opensilex.sparql.service.SPARQLService;
import org.opensilex.sparql.utils.Ontology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.opensilex.sparql.service.SPARQLQueryHelper.makeVar;

public class Faidarev1StudyDTOBuilder {
    private final String SCIENTIFIC_SUPERVISOR = "ScientificSupervisor";
    private final String TECHNICAL_SUPERVISOR = "TechnicalSupervisor";

    public static final Logger LOGGER = LoggerFactory.getLogger(Faidarev1StudyDTOBuilder.class);

    private final DataDAO dataDAO;
    private final AccountModel currentAccount;
    private final SPARQLService sparql;
    private final String experimentDetailsPath;


    public Faidarev1StudyDTOBuilder(DataDAO dataDAO, AccountModel currentAccount, SPARQLService sparql, String experimentDetailsPath) {
        this.dataDAO = dataDAO;
        this.currentAccount = currentAccount;
        this.sparql = sparql;
        this.experimentDetailsPath = experimentDetailsPath;
    }

    public Faidarev1StudyDTO fromModel(ExperimentModel model) throws Exception {
        Faidarev1StudyDTO dto = new Faidarev1StudyDTO();

        dto.setStudyDbId(SPARQLDeserializers.getExpandedURI(model.getUri()))
                .setStudyName(model.getName())
                .setName(model.getName())
                .setStartDate(Objects.toString(model.getStartDate(), null))
                .setEndDate(Objects.toString(model.getEndDate(), null))
                .setActive(
                        Optional.ofNullable(model.getEndDate())
                                .map(endDate -> endDate.isBefore(LocalDate.now()))
                                .orElse(model.getStartDate().isAfter(LocalDate.now())) ? "false" : "true"
                )
                .setStudyDescription(model.getDescription());

        if (Objects.nonNull(model.getLastUpdateDate())) {
            Faidarev1LastUpdateDTO updateDTO = new Faidarev1LastUpdateDTO();
            updateDTO.setTimestamp(model.getLastUpdateDate().toString());
            dto.setLastUpdate(updateDTO);
        }

        if (Objects.nonNull(model.getEndDate())) {
            dto.setSeasons(
                    IntStream.rangeClosed(model.getStartDate().getYear(), model.getEndDate().getYear())
                            .mapToObj(String::valueOf)
                            .collect(Collectors.toList())
            );
        } else {
            dto.setSeasons(
                    IntStream.rangeClosed(model.getStartDate().getYear(), LocalDate.now().getYear())
                            .mapToObj(String::valueOf)
                            .collect(Collectors.toList())
            );
        }

        if (!model.getProjects().isEmpty()) {
            dto.setTrialDbIds(model.getProjects().stream().map(projectModel -> SPARQLDeserializers.getExpandedURI(projectModel.getUri()))
                    .collect(Collectors.toList()));
        }

        List<FacilityModel> facilitiesList = model.getFacilities();
        if (!facilitiesList.isEmpty()){
            FacilityModel facility = facilitiesList.get(0);
            dto.setLocationDbId(SPARQLDeserializers.getExpandedURI(facility.getUri()))
                    .setLocationName(facility.getName());
        }

        List<Faidarev1ContactDTO> studyContacts = new ArrayList<>();
        Faidarev1ContactDTOBuilder contactDTOBuilder = new Faidarev1ContactDTOBuilder();
        List<PersonModel> experimentScientificSupervisors = model.getScientificSupervisors();
        if (!experimentScientificSupervisors.isEmpty()) {
            studyContacts.addAll(experimentScientificSupervisors.stream()
                    .map(personModel -> contactDTOBuilder.fromModel(personModel, SCIENTIFIC_SUPERVISOR))
                    .collect(Collectors.toList()));
        }
        List<PersonModel> experimentTechnicalSupervisors = model.getTechnicalSupervisors();
        if (!experimentTechnicalSupervisors.isEmpty()) {
            studyContacts.addAll(experimentTechnicalSupervisors.stream()
                    .map(personModel -> contactDTOBuilder.fromModel(personModel, TECHNICAL_SUPERVISOR))
                    .collect(Collectors.toList()));
        }
        dto.setContacts(studyContacts);

        Set<URI> variablesSet = this.dataDAO.getUsedVariablesByExpeSoDevice(
                currentAccount,
                Collections.singletonList(model.getUri()),
                null,
                null
        );
        if (!variablesSet.isEmpty()) {
            dto.setObservationVariableDbIds(variablesSet.stream().map(SPARQLDeserializers::getExpandedURI).collect(Collectors.toList()));
        }


        // SPARQL request to get experiment accessions
        // select {
        //     graph <experimentUri> {
        //         ?scientificObject a ?rdfType ;
        //         vocabulary:hasGermplasm ?germplasm;
        //     }
        //     ?rdfType rdfs:subClassOf* vocabulary:ScientificObject .
        //     ?germplasm a/rdfs:subClassOf* vocabulary:Accession .
        // }

        // Vars
        Var scientificObjectVar = makeVar("scientificObject");
        Var germplasmVar = makeVar("germplasm");
        Var rdfTypeVar = makeVar("rdfType");

        // Uris
        Node experimentUriNode = SPARQLDeserializers.nodeURI(model.getUri());

        WhereBuilder whereInExperiment = new WhereBuilder();
        whereInExperiment.addWhere(scientificObjectVar, RDF.type.asNode(), rdfTypeVar);
        whereInExperiment.addWhere(scientificObjectVar, Oeso.hasGermplasm.asNode(), germplasmVar);
        SelectBuilder accessionSelect = new SelectBuilder()
                .addGraph(experimentUriNode, whereInExperiment)
                .addWhere(rdfTypeVar, Ontology.subClassAny, Oeso.ScientificObject.asNode())
                .addWhere(germplasmVar, Ontology.typeSubClassAny, Oeso.Accession.asNode());

        try {
            List<SPARQLResult> selectedAccessions = sparql.executeSelectQuery(accessionSelect);

            if (!selectedAccessions.isEmpty()) {
                dto.setGermplasmDbIds(new ArrayList<>(selectedAccessions
                        .stream().map(accession -> SPARQLDeserializers.getExpandedURI(accession.getStringValue("germplasm")))
                        .collect(Collectors.toSet())));
            }
        } catch (SPARQLException e) {
            LOGGER.error("Error while fetching the accessions of study " + dto.getStudyDbId(), e);
        }

        dto.setDocumentationURL(experimentDetailsPath.replace(":uri", URLEncoder.encode(dto.getStudyDbId(), StandardCharsets.UTF_8)));

        return dto;

    }
}
