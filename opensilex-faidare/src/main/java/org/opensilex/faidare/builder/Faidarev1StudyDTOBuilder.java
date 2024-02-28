package org.opensilex.faidare.builder;

import org.opensilex.core.experiment.dal.ExperimentModel;
import org.opensilex.core.organisation.dal.OrganizationDAO;
import org.opensilex.core.organisation.dal.facility.FacilityDAO;
import org.opensilex.core.organisation.dal.facility.FacilityModel;
import org.opensilex.core.project.dal.ProjectModel;
import org.opensilex.faidare.model.Faidarev1ContactDTO;
import org.opensilex.faidare.model.Faidarev1LocationDTO;
import org.opensilex.faidare.model.Faidarev1StudyDTO;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.security.person.dal.PersonModel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Faidarev1StudyDTOBuilder {
    private final String SCIENTIFIC_SUPERVISOR = "ScientificSupervisor";
    private final String TECHNICAL_SUPERVISOR = "TechnicalSupervisor";

    private final FacilityDAO facilityDAO;
    private final OrganizationDAO organizationDAO;

    public Faidarev1StudyDTOBuilder(FacilityDAO facilityDAO, OrganizationDAO organizationDAO) {
        this.facilityDAO = facilityDAO;
        this.organizationDAO = organizationDAO;
    }

    public Faidarev1StudyDTO fromModel(ExperimentModel model, AccountModel currentAccount) throws Exception {
        Faidarev1StudyDTO dto = new Faidarev1StudyDTO();

        dto.setStudyDbId(model.getUri().toString())
                .setStudyName(model.getName())
                .setStartDate(Objects.toString(model.getStartDate(), null))
                .setEndDate(Objects.toString(model.getEndDate(), null))
                .setActive(
                        Optional.ofNullable(model.getEndDate())
                                .map(endDate -> endDate.isBefore(LocalDate.now()))
                                .orElse(model.getStartDate().isAfter(LocalDate.now())) ? "false" : "true"
                ) // TODO : for review : shouldn't the "Active" information be in the ExperimentModel?
                .setStudyDescription(model.getDescription());

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


        // TODO : Remove this? and map projects to trials instead?
        if (!model.getProjects().isEmpty()) {
            // ProgramName not a list, so only the first one is kept
            ProjectModel firstProject = model.getProjects().get(0);
            dto.setProgramName(firstProject.getName());
            dto.setProgramDbId(firstProject.getUri().toString());
        }

        List<FacilityModel> facilitiesList = model.getFacilities();
        Faidarev1LocationDTOBuilder locationDTOBuilder = new Faidarev1LocationDTOBuilder(facilityDAO, organizationDAO);
        if (!facilitiesList.isEmpty()){
            FacilityModel facility = facilitiesList.get(0);
            Faidarev1LocationDTO locationDTO = locationDTOBuilder.fromModel(facility, currentAccount);
            dto.setLocation(locationDTO);
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

        // TODO : Add mapping for last update (add in docs too)

        return dto;

    }
}
