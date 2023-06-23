<template>
  <div>
    <opensilex-ExperimentForm
      v-if="
        user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)
      "
      ref="experimentForm"
      @onUpdate="loadExperiment()"
    ></opensilex-ExperimentForm>

    <div v-if="experiment" class="row">
      <div class="col col-xl-6" style="min-width: 400px">
        <opensilex-Card
          icon="ik#ik-clipboard"
          :label="$t('component.common.informations')"
        >
          <template v-slot:rightHeader>
            <b-button-group
              v-if="
                user.hasCredential(
                  credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID
                )
              "
            >
              <opensilex-FavoriteButton
                  :uri="experiment.uri"
              ></opensilex-FavoriteButton>
              
              <opensilex-EditButton
                v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID
                  )
                "
                @click="showEditForm()"
                label="component.experiment.update"
              ></opensilex-EditButton>

              <opensilex-DeleteButton
                v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_EXPERIMENT_DELETE_ID
                  )
                "
                @click="deleteExperiment(experiment.uri)"
                label="component.experiment.delete"
              ></opensilex-DeleteButton>
            </b-button-group>
          </template>

          <template v-slot:body>
            <opensilex-StringView
              label="component.common.name"
              :value="experiment.name"
            ></opensilex-StringView>
            <div class="static-field">
              <span class="field-view-title">{{
                $t("component.common.state")
              }}</span>
              <span class="static-field-line">
                <span
                  v-if="!isEnded(experiment)"
                  class="badge badge-pill badge-info-phis"
                  :title="$t('component.experiment.view.status.in-progress')"
                >
                  <i class="ik ik-activity mr-1"></i>
                  {{ $t("component.experiment.common.status.in-progress") }}
                </span>
                <span
                  v-else
                  class="badge badge-pill badge-light"
                  :title="$t('component.experiment.view.status.finished')"
                >
                  <i class="ik ik-archive"></i>
                  {{ $t("component.experiment.common.status.finished") }}
                </span>

                <span
                  v-if="experiment.is_public"
                  class="badge badge-pill badge-info"
                  :title="$t('component.experiment.view.status.public')"
                >
                  <i class="ik ik-users mr-1"></i>
                  {{ $t("component.experiment.common.status.public") }}
                </span>
              </span>
            </div>
            <opensilex-StringView
              label="component.common.period"
              :value="period"
            ></opensilex-StringView>
            <opensilex-UriView :uri="experiment.uri"></opensilex-UriView>
            <opensilex-TextView
              label="component.experiment.objective"
              :value="experiment.objective"
            ></opensilex-TextView>
            <opensilex-TextView
              label="component.experiment.comment"
              :value="experiment.description"
            ></opensilex-TextView>
          </template>
        </opensilex-Card>
      </div>

      <div class="col col-xl-6">
        <opensilex-Card
          icon="ik#ik-box"
          :label="$t('component.experiment.context')"
        >
          <template v-slot:body>
            <opensilex-UriListView
              label="component.experiment.projects"
              :list="projectsList"
            ></opensilex-UriListView>
            <opensilex-UriListView
              label="component.experiment.infrastructures"
              :list="infrastructuresListURIs"
            ></opensilex-UriListView>
            <opensilex-UriListView
              label="component.experiment.facilities"
              :list="facilityListUris"
            ></opensilex-UriListView>
            <opensilex-UriListView
              v-if="!isGermplasmMenuExcluded"
              label="component.experiment.species"
              :list="speciesList"
            ></opensilex-UriListView>
            <opensilex-UriListView
              label="component.menu.experimentalDesign.factors"
              :list="factorsList"
            ></opensilex-UriListView>
            <opensilex-UriListView
              label="component.experiment.groups"
              :list="groupsList"
            ></opensilex-UriListView>
          </template>
        </opensilex-Card>

        <opensilex-Card
          icon="ik#ik-users"
          :label="$t('component.experiment.contacts')"
        >
          <template v-slot:body>
            <opensilex-ContactsList
              label="component.experiment.scientificSupervisors"
              :list="scientificSupervisorsList"
            ></opensilex-ContactsList>
            <opensilex-ContactsList
              label="component.experiment.technicalSupervisors"
              :list="technicalSupervisorsList"
            ></opensilex-ContactsList>
            <opensilex-UriView
              title="component.experiment.record_author"
              :uri="recordAuthor.uri"
              :value="recordAuthor.first_name + ' ' + recordAuthor.last_name"
          >
          </opensilex-UriView>
          </template>
        </opensilex-Card>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import {
  ExperimentGetDTO,
  ExperimentsService,
  FactorGetDTO,
  FactorsService,
  OrganizationsService,
  ProjectsService,
  SpeciesDTO,
  SpeciesService
} from "opensilex-core/index";
import {GroupDTO, SecurityService, UserGetDTO} from "opensilex-security/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import DTOConverter from "../../../models/DTOConverter";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {PersonDTO} from "opensilex-security/index";

@Component
export default class ExperimentDetail extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $t: any;
  $route: any;
  $store: any;

  uri: string = "";
  period: string = "";

  @Ref("experimentForm") readonly experimentForm!: any;
  experiment: any = null;
  service: ExperimentsService;

  speciesList = [];
  factorsList = [];
  groupsList = [];
  projectsList = [];
  scientificSupervisorsList = [];
  technicalSupervisorsList = [];
  installationsList = [];
  infrastructuresList = [];
  recordAuthor :UserGetDTO = null;

  created() {
    this.service = this.$opensilex.getService("opensilex.ExperimentsService");
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.loadExperiment();
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      (lang) => {
        this.loadSpecies();
        this.period = this.$opensilex.$dateTimeFormatter.formatPeriod(
          this.experiment.start_date,
          this.experiment.end_date
        );
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  showEditForm() {
    this.experimentForm.showEditForm(DTOConverter.extractURIFromResourceProperties(this.experiment));
  }

  get isGermplasmMenuExcluded() {
        return this.$opensilex.getConfig().menuExclusions.includes("germplasm");
  }

  get infrastructuresListURIs() {
    let infraUris = [];
    for (let infra of this.infrastructuresList) {
      infra.to = {
        path: "/infrastructure/details/" + encodeURIComponent(infra.uri),
      };
      infraUris.push(infra);
    }
    return infraUris;
  }

  get facilityListUris() {
    return this.experiment.facilities.map(facility => {
      return {
        uri: facility.uri,
        value: facility.name,
        to: {
          path: "/facility/details/" + encodeURIComponent(facility.uri),
        }
      }
    });
  }

  deleteExperiment(uri: string) {
    this.service
      .deleteExperiment(uri)
      .then(() => {
        let message = this.$i18n.t("ExperimentList.name") + " " + uri + " " + this.$i18n.t("component.common.success.delete-success-message");
        this.$opensilex.showSuccessToast(message);
        this.$router.push({
          path: "/experiments",
        });
      })
      .catch(this.$opensilex.errorHandler);
  }

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  loadExperiment() {
    if (this.uri) {
      this.service
        .getExperiment(this.uri)
        .then((http: HttpResponse<OpenSilexResponse<ExperimentGetDTO>>) => {
          this.experiment = http.response.result;
          this.loadExperimentDetails();
        })
        .catch((error) => {
          this.$opensilex.errorHandler(error);
        });
    }
  }

  loadExperimentDetails() {
    this.loadProjects();
    this.loadInfrastructures();
    this.loadPersons();
    this.loadGroups();
    this.loadFactors();
    this.loadSpecies();
    this.period = this.$opensilex.$dateTimeFormatter.formatPeriod(
      this.experiment.start_date,
      this.experiment.end_date
    );
  }

  loadInfrastructures() {
    let service: OrganizationsService = this.$opensilex.getService(
      "opensilex.OrganizationsService"
    );
    this.infrastructuresList = [];

    if (
      this.experiment.organisations &&
      this.experiment.organisations.length > 0
    ) {
      this.experiment.organisations.forEach((organisation) => {
        this.infrastructuresList.push({
          uri: organisation.uri,
          value: organisation.name,
        });
      });
    }
  }

  loadGroups() {
    let service: SecurityService = this.$opensilex.getService(
      "opensilex.SecurityService"
    );
    this.groupsList = [];
    if (this.experiment.groups && this.experiment.groups.length > 0) {
      service
        .getGroupsByURI(this.experiment.groups)
        .then((http: HttpResponse<OpenSilexResponse<GroupDTO[]>>) => {
          this.groupsList = http.response.result.map((group) => {
            return {
              uri: group.uri,
              value: group.name,
            };
          });
        })
        .catch(this.$opensilex.errorHandler);
    }
  }

  loadPersons() {
    let service: SecurityService = this.$opensilex.getService(
      "opensilex.SecurityService"
    );
    this.scientificSupervisorsList = [];
    if (
      this.experiment.scientific_supervisors &&
      this.experiment.scientific_supervisors.length > 0
    ) {
      service
        .getPersonsByURI(this.experiment.scientific_supervisors)
        .then((http: HttpResponse<OpenSilexResponse<PersonDTO[]>>) => {
          this.scientificSupervisorsList = http.response.result
        })
        .catch(this.$opensilex.errorHandler);
    }
    this.technicalSupervisorsList = [];
    if (
      this.experiment.technical_supervisors &&
      this.experiment.technical_supervisors.length > 0
    ) {
      service
        .getPersonsByURI(this.experiment.technical_supervisors)
        .then((http: HttpResponse<OpenSilexResponse<PersonDTO[]>>) => {
          this.technicalSupervisorsList = http.response.result
        })
        .catch(this.$opensilex.errorHandler);
    }
    if (this.experiment.record_author &&
      this.experiment.record_author.length > 0){
        service
        .getUser(this.experiment.record_author)
        .then((http: HttpResponse<OpenSilexResponse<UserGetDTO>>) => {
          this.recordAuthor = http.response.result;
        })
        .catch(this.$opensilex.errorHandler);
      }
  }

  loadSpecies() {
    let service: SpeciesService = this.$opensilex.getService(
      "opensilex.SpeciesService"
    );
    this.speciesList = [];
    if (this.experiment.species && this.experiment.species.length > 0) {
      service
        .getAllSpecies()
        .then((http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) => {
          for (let i = 0; i < http.response.result.length; i++) {
            if (
              this.experiment.species.find(
                (species) => species == http.response.result[i].uri
              )
            ) {
              this.speciesList.push(http.response.result[i]);
            }
          }

          this.speciesList = this.speciesList.map((item) => {
            return {
              uri: item.uri,
              value: item.name,
              to: {
                path: "/germplasm/details/" + encodeURIComponent(item.uri),
              },
            };
          });
        })
        .catch(this.$opensilex.errorHandler);
    }
  }

  loadFactors() {
    let service: FactorsService = this.$opensilex.getService(
      "opensilex.FactorsService"
    );
    this.factorsList = [];
    if (this.experiment.factors && this.experiment.factors.length > 0) {
      service
        .searchFactors(
          undefined, // name
          undefined, // description
          undefined, // category
          undefined, // experiment
          undefined, // orderBy
          0, // page
          0 // pageSize
        )
        .then((http: HttpResponse<OpenSilexResponse<Array<FactorGetDTO>>>) => {
          for (let i = 0; i < http.response.result.length; i++) {
            if (
              this.experiment.factors.find(
                (factors) => factors == http.response.result[i].uri
              )
            ) {
              this.factorsList.push(http.response.result[i]);
            }
          }

          this.factorsList = this.factorsList.map((item) => {
            return {
              uri: item.uri,
              value: item.name,
              to: {
                path:
                  "/" +
                  encodeURIComponent(this.uri) +
                  "/factor/details/" +
                  encodeURIComponent(item.uri),
              },
            };
          });
        })
        .catch(this.$opensilex.errorHandler);
    }
  }

  loadProjects() {
    let service: ProjectsService = this.$opensilex.getService(
      "opensilex.ProjectsService"
    );
    this.projectsList = [];
    if (this.experiment.projects) {
      this.experiment.projects.forEach((project) => {
        this.projectsList.push({
          uri: project.uri,
          value: project.name,
          to: {
            path: "/project/details/" + encodeURIComponent(project.uri),
          },
        });
      });
    }
  }

  isEnded(experiment) {
    if (experiment.end_date) {
      return new Date(experiment.end_date).getTime() < new Date().getTime();
    }
    return false;
  }
}
</script>

<style scoped lang="scss">
</style>
