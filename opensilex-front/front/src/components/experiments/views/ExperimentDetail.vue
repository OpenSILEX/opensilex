<template>
  <div>
    <ExperimentForm
      v-if="
        user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)
      "
      ref="experimentForm"
      @onUpdate="loadExperiment()"
    ></ExperimentForm>

    <div v-if="experiment" class="row">
      <div class="col col-xl-6" style="min-width: 400px">
        <Card
          icon="ik#ik-clipboard"
          :label="t('component.common.informations')"
        >
          <template v-slot:rightHeader>
            <b-button-group
              v-if="
                user.hasCredential(
                  credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID
                )
              "
            >
              <FavoriteButton
                  :uri="experiment.uri"
              ></FavoriteButton>
              
              <EditButton
                v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID
                  )
                "
                @click="showEditForm()"
                label="component.experiment.update"
              ></EditButton>

              <DeleteButton
                v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_EXPERIMENT_DELETE_ID
                  )
                "
                @click="deleteExperiment(experiment.uri)"
                label="component.experiment.delete"
               small></DeleteButton>
            </b-button-group>
          </template>

          <template v-slot:body>
            <StringView
              label="component.common.name"
              :value="experiment.name"
            ></StringView>
            <StringView
              label="component.common.altName"
              :value="experiment.alternative_name"
            ></StringView>
            <div class="static-field">
              <span class="field-view-title">{{
                t("component.common.state")
              }}</span>
              <span class="static-field-line">
                <span
                  v-if="!isEnded(experiment)"
                  class="badge badge-pill badge-info-phis"
                  :title="t('component.experiment.view.status.in-progress')"
                >
                  <i class="ik ik-activity mr-1"></i>
                  {{ t("component.experiment.common.status.in-progress") }}
                </span>
                <span
                  v-else
                  class="badge badge-pill badge-light"
                  :title="t('component.experiment.view.status.finished')"
                >
                  <i class="ik ik-archive"></i>
                  {{ t("component.experiment.common.status.finished") }}
                </span>

                <span
                  v-if="experiment.is_public"
                  class="badge badge-pill badge-info"
                  :title="t('component.experiment.view.status.public')"
                >
                  <i class="ik ik-users mr-1"></i>
                  {{ t("component.experiment.common.status.public") }}
                </span>
              </span>
            </div>
            <StringView
              label="component.common.date-time.period"
              :value="period"
            ></StringView>
            <UriView :uri="experiment.uri"></UriView>
            <TextView
              label="component.experiment.objective"
              :value="experiment.objective"
            ></TextView>
            <TextView
              label="component.experiment.comment"
              :value="experiment.description"
            ></TextView>
            <MetadataView
              v-if="experiment.publisher && experiment.publisher.uri"
              :publisher="experiment.publisher"
              :publicationDate="experiment.publication_date"
              :lastUpdatedDate="experiment.last_updated_date" 
            ></MetadataView>
          </template>
          
          <template v-slot:footer v-if="experiment.funding">
            <div 
              :label="t('component.experiment.funding')"
              :value="experiment.funding"
              class="text-right"
              >
              <img
                v-for="fundingUri in experiment.funding.slice(0, 3)"
                :key="fundingUri"
                v-bind:src="opensilex.getResourceURI('images/'+fundingUri, ['png', 'svg', 'jpg'])"
                class="funding-tag"
                :title="fundingUri"
                >
            </div>
          </template>

        </Card>
      </div>

      <div class="col col-xl-6">
        <Card
          icon="ik#ik-box"
          :label="t('component.experiment.context')"
        >
          <template v-slot:body>
            <UriListView
              label="component.experiment.projects"
              :list="projectsList"
            ></UriListView>
            <UriListView
              label="component.experiment.organizations"
              :list="organizationsListURIs"
            ></UriListView>
            <UriListView
              label="component.experiment.facilities"
              :list="facilityListUris"
            ></UriListView>
            <UriListView
              v-if="!isGermplasmMenuExcluded"
              label="component.experiment.species"
              :list="speciesList"
            ></UriListView>
            <UriListView
              label="component.menu.experimentalDesign.factors"
              :list="factorsList"
            ></UriListView>
            <UriListView
              label="component.experiment.groups"
              :list="groupsList"
            ></UriListView>
          </template>
        </Card>

        <Card
          icon="ik#ik-users"
          :label="t('component.experiment.contacts')"
        >
          <template v-slot:body>
            <ContactsList
              label="component.experiment.scientificSupervisors"
              :list="scientificSupervisorsList"
            ></ContactsList>
            <ContactsList
              label="component.experiment.technicalSupervisors"
              :list="technicalSupervisorsList"
            ></ContactsList>
            <UriView
              title="component.experiment.record_author"
              v-if="recordAuthor"
              :uri="recordAuthor.uri"
              :value="recordAuthor.linked_person ? recordAuthor.person_first_name + ' ' + recordAuthor.person_last_name : recordAuthor.email"
          >
          </UriView>
          </template>
        </Card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {computed, inject, onMounted, onBeforeUnmount, ref, useTemplateRef} from "vue";
import {
  ExperimentsService,
  FactorsService,
  OrganizationsService,
  ProjectsService,
  SpeciesDTO,
  SpeciesService
} from "opensilex-core/index";
import {AccountGetDTO, GroupDTO, SecurityService} from "opensilex-security/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import DTOConverter from "../../../models/DTOConverter";
import {PersonDTO} from "opensilex-security/index";
import ExperimentForm from "@/components/experiments/form/ExperimentForm.vue";
import Card from "@/components/common/views/Card.vue";
import FavoriteButton from "@/components/common/buttons/FavoriteButton.vue";
import StringView from "@/components/common/views/StringView.vue";
import UriView from "@/components/common/views/UriView.vue";
import TextView from "@/components/common/views/TextView.vue";
import MetadataView from "@/components/common/views/MetadataView.vue";
import UriListView from "@/components/common/views/UriListView.vue";
import ContactsList from "@/components/persons/ContactsList.vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useI18n} from "vue-i18n";
import {useRouter} from "vue-router";
import {useRoute} from "vue-router";
import {useStore} from "vuex";
import {ExperimentGetDTO} from "opensilex-core/model/experimentGetDTO";
import {FactorGetDTO} from "opensilex-core/model/factorGetDTO";
import DeleteButton from "@/components/common/buttons/DeleteButton.vue";
import EditButton from "@/components/common/buttons/EditButton.vue";

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const store = useStore()
const uri = ref<string>()
const period = ref<string>()
const experimentForm = useTemplateRef<InstanceType<typeof ExperimentForm>>('experimentForm')
const experiment = ref<any>()
let experimentService = opensilex.getService<ExperimentsService>("opensilex.ExperimentsService")


const speciesList = ref<any[]>([]);
const factorsList = ref<any[]>([]);
const groupsList = ref<any[]>([]);
const projectsList = ref<any[]>([]);
const scientificSupervisorsList = ref<PersonDTO[]>([]);
const technicalSupervisorsList = ref<PersonDTO[]>([]);
const installationsList = ref<any[]>([]);
const organizationsList = ref<any[]>([]);

const recordAuthor = ref<AccountGetDTO | null>(null)
const routeArr = ref<string[]>(route.path.split("/"))

  function created() {
    experimentService = opensilex.getService("opensilex.ExperimentsService");
    uri.value = decodeURIComponent(route.params.uri as string) as any;
    localStorage.setItem("tabPath", routeArr.value[2]);
    localStorage.setItem("tabPage", "1");
    loadExperiment();
  }

let langWatcher: (() => void) | undefined;

onMounted(() => {
  created();
  langWatcher = store.watch(
      () => store.getters.language,
      () => {
        loadSpecies();

        if (experiment.value) {
          period.value = opensilex.$dateTimeFormatter.formatPeriod(
              experiment.value.start_date,
              experiment.value.end_date
          );
        }
      }
  );
});

onBeforeUnmount(() => {
  langWatcher?.();
});

  function showEditForm() {
    experimentForm.value.showEditForm(DTOConverter.extractURIFromResourceProperties(experiment.value));
  }

  const isGermplasmMenuExcluded = computed(() =>
  {
    return opensilex.getConfig().menuExclusions.includes("germplasm");
  })

  const organizationsListURIs = computed(() => {
  let orgaUris = [];
  for (let orga of organizationsList.value) {
    orga.to = {
      path: "/organization/details/" + encodeURIComponent(orga.uri),
    };
    orgaUris.push(orga);
  }
  return orgaUris;
})

  const facilityListUris = computed(() => {
    return experiment.value.facilities.map((facility: { uri: string | number | boolean; name: any; }) => {
      return {
        uri: facility.uri,
        value: facility.name,
        to: {
          path: "/facility/details/" + encodeURIComponent(facility.uri),
        }
      }
    });
  })

  function deleteExperiment(uri: string) {
    experimentService
      .deleteExperiment(uri)
      .then(() => {
        let message = t("ExperimentList.name") + " " + uri + " " + t("component.common.success.delete-success-message");
        opensilex.showSuccessToast(message);
        router.push({
          path: "/experiments",
        });
      })
      .catch(opensilex.errorHandler);
  }

  const user = computed(() => {
    return store.state.user;
  } )

  const credentials = computed(() => {
    return store.state.credentials;
  })

  function loadExperiment() {
    if (uri.value) {
      experimentService
        .getExperiment(uri.value)
        .then((http: HttpResponse<OpenSilexResponse<ExperimentGetDTO>>) => {
          experiment.value = http.response.result;
          loadExperimentDetails();
        })
        .catch((error) => {
          opensilex.errorHandler(error);
        });
    }
  }

  function loadExperimentDetails() {
    loadProjects();
    loadOrganizations();
    loadPersons();
    loadGroups();
    loadFactors();
    loadSpecies();
    period.value = opensilex.$dateTimeFormatter.formatPeriod(
      experiment.value.start_date,
      experiment.value.end_date
    );
  }

 function loadOrganizations() {
    let service: OrganizationsService = opensilex.getService(
      "opensilex.OrganizationsService"
    );
    organizationsList.value = [];

    if (
      experiment.value.organisations &&
      experiment.value.organisations.length > 0
    ) {
      experiment.value.organisations.forEach((organisation: { uri: any; name: any; }) => {
        organizationsList.value.push({
          uri: organisation.uri,
          value: organisation.name,
        });
      });
    }
  }

  function loadGroups() {
    let service: SecurityService = opensilex.getService(
      "opensilex.SecurityService"
    );
    groupsList.value = [];
    if (experiment.value.groups && experiment.value.groups.length > 0) {
      service
        .getGroupsByURI(experiment.value.groups)
        .then((http: HttpResponse<OpenSilexResponse<GroupDTO[]>>) => {
          groupsList.value = http.response.result.map((group) => {
            return {
              uri: group.uri,
              value: group.name,
            };
          });
        })
        .catch(opensilex.errorHandler);
    }
  }

  function loadPersons() {
    let service: SecurityService = opensilex.getService(
      "opensilex.SecurityService"
    );
    scientificSupervisorsList.value = [];
    if (
      experiment.value.scientific_supervisors &&
      experiment.value.scientific_supervisors.length > 0
    ) {
      service
        .getPersonsByURI(experiment.value.scientific_supervisors)
        .then((http: HttpResponse<OpenSilexResponse<PersonDTO[]>>) => {
          scientificSupervisorsList.value = http.response.result
        })
        .catch(opensilex.errorHandler);
    }
    technicalSupervisorsList.value = [];
    if (
      experiment.value.technical_supervisors &&
      experiment.value.technical_supervisors.length > 0
    ) {
      service
        .getPersonsByURI(experiment.value.technical_supervisors)
        .then((http: HttpResponse<OpenSilexResponse<PersonDTO[]>>) => {
          technicalSupervisorsList.value = http.response.result
        })
        .catch(opensilex.errorHandler);
    }
    if (experiment.value.record_author &&
      experiment.value.record_author.length > 0){
        service
        .getAccount(experiment.value.record_author)
        .then( accountResponse => {
          recordAuthor.value = accountResponse.response.result;
        })
        .catch(opensilex.errorHandler);
      }
  }

  function loadSpecies() {
    let service: SpeciesService = opensilex.getService(
      "opensilex.SpeciesService"
    );
    speciesList.value = [];
    if (experiment.value.species && experiment.value.species.length > 0) {
      service
        .getAllSpecies()
        .then((http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) => {
          for (let i = 0; i < http.response.result.length; i++) {
            if (
              experiment.value.species.find(
                (species: any) => opensilex.compareUris(species, http.response.result[i].uri)
              )
            ) {
              speciesList.value.push(http.response.result[i]);
            }
          }

          speciesList.value = speciesList.value.map((item) => {
            return {
              uri: item.uri,
              value: item.name,
              to: {
                path: "/germplasm/details/" + encodeURIComponent(item.uri),
              },
            };
          });
        })
        .catch(opensilex.errorHandler);
    }
  }

  function loadFactors() {
    let service: FactorsService = opensilex.getService(
      "opensilex.FactorsService"
    );
    factorsList.value = [];
    if (experiment.value.factors && experiment.value.factors.length > 0) {
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
              experiment.value.factors.find(
                (factors) => factors == http.response.result[i].uri
              )
            ) {
              factorsList.value.push(http.response.result[i]);
            }
          }

          factorsList.value = factorsList.value.map((item) => {
            return {
              uri: item.uri,
              value: item.name,
              to: {
                path:
                  "/" +
                  encodeURIComponent(uri.value) +
                  "/factor/details/" +
                  encodeURIComponent(item.uri),
              },
            };
          });
        })
        .catch(opensilex.errorHandler);
    }
  }

  function loadProjects() {
    let service: ProjectsService = opensilex.getService(
      "opensilex.ProjectsService"
    );
    projectsList.value = [];
    if (experiment.value.projects) {
      experiment.value.projects.forEach((project: { uri: string | number | boolean; name: any; }) => {
        projectsList.value.push({
          uri: project.uri,
          value: project.name,
          to: {
            path: "/project/details/" + encodeURIComponent(project.uri),
          },
        });
      });
    }
  }

 function isEnded(experiment: { end_date: string | number | Date; }) {
    if (experiment.end_date) {
      return new Date(experiment.end_date).getTime() < new Date().getTime();
    }
    return false;
  }

</script>

<style scoped lang="scss">
.funding-tag {
  margin: 0 5px;
  width: clamp(20px, 45px, 60px);
  border-radius: 50%;
  transition: box-shadow 0.6s ease-in-out;
}

.funding-tag:hover {
  box-shadow: 0 0 15px #00A3A6;
}

</style>
