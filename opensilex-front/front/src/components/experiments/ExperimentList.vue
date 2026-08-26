<template>
  <div>
    <PageContent class="pagecontent">
      <!-- Toggle Sidebar-->
      <div
        class="searchMenuContainer"
        v-on:click="searchFiltersToggle = !searchFiltersToggle"
        :title="searchFiltersPannel()"
      >
        <div class="searchMenuIcon">
          <i class="icon ik ik-search"></i>
        </div>
      </div>
      <!-- FILTERS -->
      <Transition>
        <div v-show="searchFiltersToggle">
          <SearchFilterField
            ref="searchFilterField"
            @search="refresh()"
            @clear="reset()"
            label="component.experiment.search.label"
            :showAdvancedSearch="true"
            class="searchFilterField"
          >
            <template v-slot:filters>
              <!-- Name -->
              <div>
                <FilterField>
                  <b-form-group>
                    <label for="name">{{ $t('ExperimentList.filter-label') }}</label>
                    <StringFilter
                      id="name"
                      :filter.sync="filter.name"
                      placeholder="ExperimentList.filter-label-placeholder"
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                    ></StringFilter>
                  </b-form-group>
                </FilterField>
              </div>

              <!-- Species -->
              <div>
                <FilterField>
                  <FormSelector
                    v-if="!isGermplasmMenuExcluded"
                    label="ExperimentList.filter-species"
                    placeholder="ExperimentList.filter-species-placeholder"
                    :multiple="true"
                    :selected.sync="filter.species"
                    :options="species"
                    class="searchFilter"
                  ></FormSelector>
                </FilterField>
              </div>

              <!-- factorCategories -->
              <div>
                <FilterField>
                  <FactorCategorySelector
                    ref="factorCategorySelector"
                    label="ExperimentList.filter-factors-categories"
                    placeholder="ExperimentList.filter-factors-categories-placeholder"
                    helpMessage="component.factor.name-help"
                    :multiple="true"
                    :category.sync="filter.factorCategories"
                    class="searchFilter"
                  ></FactorCategorySelector>
                </FilterField>
              </div>

              <!-- Facilities -->
              <div>
                <FilterField>
                  <FormSelector
                    label="ExperimentList.filter-facilities"
                    placeholder="ExperimentList.filter-facilities-placeholder"
                    :multiple="true"
                    :selected.sync="filter.facilities"
                    :options="facilities"
                    class="searchFilter"
                  ></FormSelector>
                </FilterField>
              </div>

              <!-- Year -->
              <div>
                <FilterField>
                  <label>{{ $t('ExperimentList.filter-year') }}</label>
                  <StringFilter
                    placeholder="ExperimentList.filter-year-placeholder"
                    :filter.sync="filter.yearFilter"
                    type="number"
                    class="searchFilter"
                    @handlingEnterKey="refresh()"
                  ></StringFilter> </FilterField
                ><br />
              </div>
            </template>

            <template v-slot:advancedSearch>
              <!-- Projects -->
              <div>
                <FilterField>
                  <ModalFormSelector
                    ref="projectSelector"
                    label="ExperimentList.filter-project"
                    placeholder="ExperimentList.filter-project-placeholder"
                    :selected.sync="filter.projects"
                    modalComponent="ProjectModalList"
                    :clearable="true"
                    :multiple="true"
                    @clear="refreshProjectSelector"
                    :limit="1"
                    class="searchFilter"
                    @handlingEnterKey="refresh()"
                  ></ModalFormSelector>
                </FilterField>
              </div>

              <!-- State -->
              <div>
                <FilterField>
                  <FormSelector
                    label="ExperimentList.filter-state"
                    placeholder="ExperimentList.filter-state-placeholder"
                    :multiple="false"
                    :selected.sync="filter.state"
                    :options="experimentStates"
                    class="searchFilter"
                    @handlingEnterKey="refresh()"
                  ></FormSelector>
                </FilterField>
              </div>

              <!-- funding -->
              <div>
                <FilterField>
                  <FundingSelector
                    label="ExperimentList.filter-funding"
                    placeholder="ExperimentList.filter-funding-placeholder"
                    :multiple="true"
                    :funding.sync="filter.funding"
                    class="searchFilter"
                  ></FundingSelector>
                </FilterField>
              </div>
            </template>
          </SearchFilterField>
        </div>
      </Transition>

      <TableAsyncView
        ref="tableRef"
        :searchMethod="searchExperiments"
        :fields="fields"
        :isSelectable="true"
        @refreshed="onRefreshed"
        labelNumberOfSelectedRow="ExperimentList.selected"
        iconNumberOfSelectedRow="ik#ik-layers"
      >
        <template v-slot:selectableTableButtons="{ numberOfSelectedRows }">
          <b-dropdown
            dropright
            class="mb-2 mr-2"
            :small="true"
            :text="$t('VariableList.display')"
          >
            <b-dropdown-item-button @click="clickOnlySelected()">{{
              onlySelected
                ? $t('ExperimentList.selected-all')
                : $t('component.common.selected-only')
            }}</b-dropdown-item-button>
            <b-dropdown-item-button @click="resetSelected()">{{
              $t('component.common.resetSelected')
            }}</b-dropdown-item-button>
          </b-dropdown>

          <b-dropdown
            dropright
            class="mb-2 mr-2"
            :small="true"
            :disabled="numberOfSelectedRows == 0"
            text="actions"
            v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
          >
            <b-dropdown-item-button
              v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
              @click="createDocument()"
              >{{ $t('component.common.addDocument') }}</b-dropdown-item-button
            >
          </b-dropdown>
        </template>
        <template v-slot:cell(name)="{ data }">
          <div class="uri-alt-container">
            <div class="uri-texts">
              <UriLink
                :uri="data.item.uri"
                :value="data.item.name"
                :to="{ path: '/experiment/details/' + encodeURIComponent(data.item.uri) }"
              ></UriLink>
              <span class="alt-label">{{ data.item.alternative_name }}</span>
            </div>
            <div class="uri-badges">
              <img
                v-for="fundingUri in data.item.funding.slice(0, 3)"
                :key="fundingUri"
                :src="
                  opensilex.getResourceURI('images/' + opensilex.getShortUri(fundingUri), [
                    'png',
                    'svg',
                    'jpg',
                  ])
                "
                class="funding-badge"
                :title="fundingUri"
              />
            </div>
          </div>
        </template>

        <template
          v-if="!isGermplasmMenuExcluded"
          v-slot:cell(species)="{ data }"
        >
          <span
            class="species-list"
            v-if="data.item.species.length > 0"
          >
            <span
              :key="index"
              v-for="(uri, index) in data.item.species"
            >
              <span :title="uri">{{ getSpeciesName(uri) }}</span>
              <span v-if="index + 1 < data.item.species.length">, </span>
            </span>
          </span>
          <span v-else></span>
        </template>

        <template v-slot:cell(start_date)="{ data }">
          <DateView :value="data.item.start_date"></DateView>
        </template>
        <template v-slot:cell(end_date)="{ data }">
          <DateView :value="data.item.end_date"></DateView>
        </template>

        <template v-slot:cell(state)="{ data }">
          <i
            v-if="!isEnded(data.item)"
            class="bi bi-activity badge-icon badge-info-opensilex"
            :title="t('component.experiment.common.status.in-progress')"
          ></i>
          <i
            v-else
            class="bi bi-archive badge-icon badge-light"
            :title="t('component.experiment.common.status.finished')"
          ></i>
          <i
            v-if="data.item.is_public"
            class="bi bi-people badge-icon badge-info"
            :title="t('component.experiment.common.status.public')"
          ></i>
        </template>

        <template v-slot:cell(actions)="{ data }">
          <b-button-group size="sm">
            <EditButton
              v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_MODIFICATION_ID)"
              @click="$emit('onEdit', data.item.uri)"
              label="component.experiment.update"
              :small="true"
            ></EditButton>
            <DeleteButton
              v-if="user.hasCredential(credentials.CREDENTIAL_EXPERIMENT_DELETE_ID)"
              @click="deleteExperiment(data.item.uri)"
              label="component.experiment.delete"
              :small="true"
            ></DeleteButton>
          </b-button-group>
        </template>
      </TableAsyncView>
      <ModalForm
        v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
        ref="documentForm"
        component="DocumentForm"
        createTitle="component.common.addDocument"
        modalSize="lg"
        :initForm="initForm"
        icon="ik#ik-file-text"
      ></ModalForm>
    </PageContent>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, inject, onMounted, onUnmounted, useTemplateRef } from 'vue';
import { SpeciesDTO, SpeciesService } from 'opensilex-core/index';
import HttpResponse, { OpenSilexResponse } from 'opensilex-core/HttpResponse';
import { User } from '../../models/User';
import { OrganizationsService } from 'opensilex-core/api/organizations.service';
import { FacilityGetDTO } from 'opensilex-core/index';
import OpenSilexVuePlugin from '../../models/OpenSilexVuePlugin';
import { ExperimentsService } from 'opensilex-core/api/experiments.service';
import { useI18n } from 'vue-i18n';
import { useStore } from 'vuex';
import DocumentForm from '../documents/DocumentForm.vue';
import TableAsyncView from '../common/views/TableAsyncView.vue';
import ModalFormSelector from '../variables/form/ModalFormSelector.vue';
import { validEmail } from '@/models/FormFieldsFormatter';
import { NamedResourceDTO } from 'opensilex-core';
import DateView from '../common/views/DateView.vue';
import UriLink from '../common/views/UriLink.vue';

const opensilex = inject<OpenSilexVuePlugin>('opensilex');
const documentForm = useTemplateRef<InstanceType<typeof DocumentForm>>('documentForm');
const { t } = useI18n();
const store = useStore();
const searchFilterToggle = false;

interface Props {
  isSelectable?: boolean;
  noActions?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  isSelectable: false,
  noActions: false,
});

const user = computed<User>(() => store.state.user);
const onlySelected = computed(() => store.state.onlySelected);
const credentials = computed(() => store.state.credentials);

const facilities = [];
const species = [];

/**
 * The key is the URI in extended form
 */

const tableRef = useTemplateRef<InstanceType<typeof TableAsyncView>>('tableRef');
const projectSelector = useTemplateRef<InstanceType<typeof ModalFormSelector>>('projectSelector');
const fundingSelector = useTemplateRef<InstanceType<typeof FundingSelector>>('fundingSelector');

const speciesByUri = ref(new Map<string, SpeciesDTO>());

function onItemUnselected(row) {
  tableRef.value?.onItemUnselected(row);
}
function onItemSelected(row) {
  tableRef.value?.onItemSelected(row);
}

function refresh() {
  updateSelectedExperiment();
  tableRef?.value.setPage(1);
}

const filter = ref({
  name: '',
  species: [],
  factorCategories: [],
  projects: [],
  yearFilter: undefined,
  state: '',
  facilities: [],
  funding: [],
});

function reset() {
  filter.value = {
    name: '',
    species: [],
    factorCategories: [],
    projects: [],
    yearFilter: undefined,
    state: '',
    facilities: [],
    funding: [],
  };

  refresh();
}

function clickOnlySelected() {
  tableRef.value.toggleOnlySelected();
}

function resetSelected() {
  tableRef?.value.resetSelection();
}

function refreshProjectSelector() {
  projectSelector?.value.refreshModalSearch();
}

function updateSelectedExperiment() {
  tableRef.value.setOnlySelected(false);
  opensilex.updateURLParameters(filter);
  tableRef?.value.refresh();
}

function searchExperiments(options: any) {
  let isPublic: boolean | undefined;
  let isEnded: boolean | undefined;

  if (filter.value.state) {
    if (filter.value.state === 'public') {
      isPublic = true;
    }

    if (filter.value.state === 'finished') {
      isEnded = true;
    } else if (filter.value.state === 'in-progress') {
      isEnded = false;
    }
  }

  return opensilex
    .getService<ExperimentsService>('opensilex.ExperimentsService')
    .searchExperiments(
      filter.value.name,
      filter.value.yearFilter,
      isEnded,
      filter.value.species,
      filter.value.factorCategories,
      filter.value.projects,
      isPublic,
      filter.value.facilities,
      filter.value.funding,
      options.orderBy,
      options.currentPage,
      options.pageSize
    );
}

let langUnwatcher: (() => void) | undefined;

onMounted(() => {
  langUnwatcher = store.watch(
    (state, getters) => getters.language,
    (lang) => {
      loadSpecies();
      loadFacilities();
      opensilex.loadFactorCategories();
      refresh();
    }
  );
});

onUnmounted(() => {
  langUnwatcher?.();
});

function beforeDestroy() {
  langUnwatcher();
}

const experimentStates = computed(() => [
  {
    id: 'in-progress',
    label: t('component.experiment.common.status.in-progress'),
  },
  {
    id: 'finished',
    label: t('component.experiment.common.status.finished'),
  },
  {
    id: 'public',
    label: t('component.experiment.common.status.public'),
  },
]);

function loadSpecies() {
  let service: SpeciesService = opensilex.getService('opensilex.SpeciesService');
  service
    .getAllSpecies()
    .then((http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) => {
      const species = [];
      for (let i = 0; i < http.response.result.length; i++) {
        speciesByUri.value.set(
          opensilex.getLongUri(http.response.result[i].uri),
          http.response.result[i]
        );
        species.push({
          id: http.response.result[i].uri,
          label: http.response.result[i].name,
        });
      }
    })
    .catch(opensilex.errorHandler);
}

function loadFacilities() {
  let service: OrganizationsService = opensilex.getService('opensilex.OrganizationsService');
  service
    .getAllFacilities()
    .then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) => {
      const facilities = [];
      for (let i = 0; i < http.response.result.length; i++) {
        facilities.push({
          id: http.response.result[i].uri,
          label: http.response.result[i].name,
        });
      }
    })
    .catch(opensilex.errorHandler);
}

function getSpeciesName(uri: string): String {
  return speciesByUri.value.get(opensilex.getLongUri(uri))?.name;
}

function isEnded(experiment) {
  if (experiment.end_date) {
    return new Date(experiment.end_date).getTime() < new Date().getTime();
  }
  return false;
}

const isGermplasmMenuExcluded = computed(() => {
  return opensilex.getConfig().menuExclusions.includes('germplasm');
});
const fields = computed(() => {
  const tableFields = [
    {
      key: 'name',
      label: 'component.common.name',
      sortable: true,
      thStyle: { width: '1%' },
      tdClass: 'text-nowrap',
    },
    {
      key: 'start_date',
      label: 'component.experiment.startDate',
      sortable: true,
    },
    {
      key: 'end_date',
      label: 'component.experiment.endDate',
      sortable: true,
    },
    {
      key: 'state',
      label: 'component.experiment.search.column.state',
    },
  ];

  if (!isGermplasmMenuExcluded.value) {
    tableFields.push({
      key: 'species',
      label: 'component.experiment.species',
    });
  }

  if (!props.noActions) {
    tableFields.push({
      key: 'actions',
      label: 'component.common.actions',
    });
  }

  return tableFields;
});

function deleteExperiment(uri: string) {
  opensilex
    .getService<ExperimentsService>('opensilex.ExperimentsService')
    .deleteExperiment(uri)
    .then(() => {
      tableRef.value.checkSelectedItems(uri);
      refresh();
      let message =
        t('ExperimentList.name') +
        ' ' +
        uri +
        ' ' +
        t('component.common.success.delete-success-message');
      opensilex.showSuccessToast(message);
    })
    .catch(opensilex.errorHandler);
}

function createDocument() {
  documentForm.value.showCreateForm();
}

function initForm() {
  let targetURI = [];
  for (let select of tableRef.value.getSelected()) {
    targetURI.push(select.uri);
  }

  return {
    description: {
      uri: undefined,
      identifier: undefined,
      rdf_type: undefined,
      title: undefined,
      date: undefined,
      description: undefined,
      targets: targetURI,
      authors: undefined,
      language: undefined,
      deprecated: undefined,
      keywords: undefined,
    },
    file: undefined,
  };
}

function soGetDTOToSelectNode(dto) {
  if (dto) {
    return {
      id: dto.uri,
      label: dto.name,
    };
  }
  return null;
}

function searchFiltersPannel() {
  return t('searchfilter.label');
}
</script>

<style scoped lang="scss">
.species-list {
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
  display: inline-block;
  max-width: 40vw;
}

.funding-badge {
  width: 24px;
  height: auto;
  margin-right: 4px;
}

.uri-alt-container {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.uri-texts {
  display: flex;
  flex-direction: column;
  align-items: flex-start; /* force same left edge */
}

.uri-texts > * {
  margin: 0; /* kill any weird margins */
}

.alt-label {
  margin-top: 2px; /* optional spacing */
  font-size: 0.9em;
}

.experimentsCheckboxMarginHighSize {
  margin-left: 15px;
}
</style>

<i18n>
en:
  ExperimentList:
    name: The experiment
    filter-label: Name
    filter-label-placeholder: Enter a name
    filter-year: Year
    filter-year-placeholder: Enter a year
    filter-species: Species
    filter-species-placeholder: Select one or more species
    filter-facilities: Facilities
    filter-facilities-placeholder: Select one or more facilities
    filter-project: Project
    filter-project-placeholder: Select a project
    filter-state: State
    filter-state-placeholder: Select an experiment state
    filter-factors-categories: Factors categories
    filter-factors-categories-placeholder: Select one or more categories
    filter-funding: Funding
    filter-funding-placeholder: Select an experiment funding
    selected: Selected experiments
    selected-all: All experiments

fr:
  ExperimentList:
    name: L'expérimentation
    filter-label: Nom
    filter-label-placeholder: Saisir un nom
    filter-year: Année
    filter-year-placeholder: Saisir une année
    filter-species: Espèces
    filter-species-placeholder: Sélectionner une ou plusieurs espèces
    filter-facilities: Installations environnementales
    filter-facilities-placeholder: Sélectionner une ou plusieurs installations
    filter-project: Projet
    filter-project-placeholder: Sélectionner un projet
    filter-state: Etat
    filter-state-placeholder: Sélectionner un état
    filter-factors-categories: Categories de facteurs
    filter-factors-categories-placeholder: Sélectionner une ou plusieurs categories
    filter-funding: Financeur
    filter-funding-placeholder: Sélectionner un financeur
    selected: Expérimentations selectionnées
    selected-all: Toutes les expérimentations

</i18n>
name
