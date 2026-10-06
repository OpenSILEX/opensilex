<template>
  <div>
    <PageContent class="pagecontent">
      <n-layout has-sider class="experiment-layout">
        <!-- FILTERS -->
        <SearchFiltersSidebar
          v-model:filtersCollapsed="filtersCollapsed"
          :activeFiltersCount="activeFiltersCount"
          searchButtonLabelTranslationKey="component.experiment.search.label"
          @refresh="refresh()"
          @reset="reset()"
        >
          <!-- Name -->
          <n-form-item class="compact-form-item">
            <StringFilter
              id="name"
              label="component.common.name"
              v-model:filter="filter.name"
              placeholder="component.experiment.filter-label-placeholder"
              class="searchFilter"
              @handlingEnterKey="refresh()"
            />
          </n-form-item>

          <!-- Species -->
            <FormSelector
              :label="t('component.experiment.species')"
              :placeholder="t('component.variable.species.species-placeholder')"
              :multiple="true"
              v-model:selected="filter.species"
              :options="species"
              class="searchFilter"
            />

          <!-- factorCategories -->
          <n-form-item class="compact-form-item">
            <FactorCategorySelector
              ref="factorCategorySelector"
              :label="t('component.experiment.factors-categories')"
              helpMessage="component.experiment.category-factor-help"
              :multiple="true"
              v-model:category="filter.factorCategories"
              class="searchFilter"
            />
          </n-form-item>

          <!-- Facilities -->
            <FormSelector
              :label="t('component.experiment.facilities')"
              :placeholder="t('component.experiment.facilities-placeholder')"
              :multiple="true"
              v-model:selected="filter.facilities"
              :options="facilities"
              class="searchFilter"
            />

          <!-- Year -->
            <StringFilter
              label="component.document.date"
              placeholder="component.project.filter-year-placeholder"
              v-model:filter="filter.yearFilter"
              type="number"
              class="searchFilter"
              @handlingEnterKey="refresh()"
            />

          <!-- ADVANCED SEARCH -->
          <n-collapse :accordion="false" class="advancedFiltersSearch">
            <n-collapse-item :title="t('component.common.advanced-search-title')" name="adv">
              <!-- Projects -->
              <n-form-item class="compact-form-item">
                <ModalFormSelector
                  ref="projectSelector"
                  :label="t('component.experiment.projects')"
                  :placeholder="t('component.experiment.project-placeholder')"
                  v-model:selected="filter.projects"
                  modalComponent="opensilex-ProjectModalList"
                  :clearable="true"
                  :multiple="true"
                  @clear="refreshProjectSelector"
                  :limit="1"
                  class="searchFilter"
                  @handlingEnterKey="refresh()"
                />
              </n-form-item>

              <!-- State -->
                <FormSelector
                  :label="t('component.common.state')"
                  :placeholder="t('component.experiment.state-placeholder')"
                  :multiple="false"
                  v-model:selected="filter.state"
                  :options="experimentStates"
                  class="searchFilter"
                  @handlingEnterKey="refresh()"
                />

              <!-- funding -->
                <FundingSelector
                  :label="t('component.experiment.funding')"
                  :multiple="true"
                  v-model:fundinguri="filter.funding"
                  class="searchFilter"
                />
            </n-collapse-item>
          </n-collapse>
        </SearchFiltersSidebar>

        <n-layout-content class="experiment-content">
      <TableAsyncView
        ref="tableRef"
        :searchMethod="searchExperiments"
        :fields="fields"
        @isSelectable="true"
        @refreshed="onRefreshed"
        labelNumberOfSelectedRow="component.experiment.selected"
        iconNumberOfSelectedRow="ik#ik-layers"
      >
        <template v-slot:selectableTableButtons="{ numberOfSelectedRows }">
          <b-dropdown
            dropright
            class="mb-2 mr-2"
            :small="true"
            :text="$t('component.document.display')"
          >
            <b-dropdown-item-button @click="clickOnlySelected()">{{
              onlySelected
                ? $t('component.experiment.all')
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
          <n-button-group size="small" class="btn-group btn-group-sm">
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
          </n-button-group>
        </template>
      </TableAsyncView>
      <DocumentForm
        v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
        ref="documentForm"
        component="DocumentForm"
        createTitle="component.common.addDocument"
        modalSize="lg"
        :initForm="initForm"
        icon="ik#ik-file-text"
       edit-title="">
      </DocumentForm>
        </n-layout-content>
      </n-layout>
    </PageContent>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, inject, onMounted, onUnmounted, useTemplateRef } from 'vue';
import { useRoute } from 'vue-router';
import { SpeciesDTO, SpeciesService } from 'opensilex-core/index';
import HttpResponse, { OpenSilexResponse } from 'opensilex-core/HttpResponse';
import { User } from '../../models/User';
import { OrganizationsService } from 'opensilex-core/api/organizations.service';
import OpenSilexVuePlugin from '../../models/OpenSilexVuePlugin';
import { ExperimentsService } from 'opensilex-core/api/experiments.service';
import { useI18n } from 'vue-i18n';
import { useStore } from 'vuex';
import DocumentForm from '../documents/DocumentForm.vue';
import TableAsyncView from '../common/views/TableAsyncView.vue';
import ModalFormSelector from '../variables/form/ModalFormSelector.vue';
import { NamedResourceDTO } from 'opensilex-core';
import DateView from '../common/views/DateView.vue';
import UriLink from '../common/views/UriLink.vue';
import FundingSelector from './FundingSelector.vue';
import EditButton from "@/components/common/buttons/EditButton.vue";
import DeleteButton from "@/components/common/buttons/DeleteButton.vue";
import FormSelector from "@/components/common/forms/FormSelector.vue";
import StringFilter from "@/components/common/filters/StringFilter.vue";
import PageContent from "@/components/layout/PageContent.vue";
import FactorCategorySelector from "@/components/experiments/factors/FactorCategorySelector.vue";
import {NButtonGroup, NCollapse, NCollapseItem, NFormItem, NLayout, NLayoutContent} from "naive-ui";
import SearchFiltersSidebar from "@/components/common/filters/SearchFiltersSidebar.vue";

//#region Public
interface Props {
  isSelectable?: boolean;
  noActions?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  isSelectable: false,
  noActions: false,
});

//#endregion

//#region Private
const opensilex = inject<OpenSilexVuePlugin>('opensilex');
const documentForm = useTemplateRef<InstanceType<typeof DocumentForm>>('documentForm');
const { t } = useI18n();
const store = useStore();
const route = useRoute();

const user = computed<User>(() => store.state.user);
const onlySelected = computed(() => store.state.onlySelected);
const credentials = computed(() => store.state.credentials);

const facilities = ref([]);
const species = ref([]);
const filtersCollapsed = ref(true);

/**
 * The key is the URI in extended form
 */

const tableRef = useTemplateRef<InstanceType<typeof TableAsyncView>>('tableRef');
const projectSelector = useTemplateRef<InstanceType<typeof ModalFormSelector>>('projectSelector');

const speciesByUri = ref(new Map<string, SpeciesDTO>());

//#endregion
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

const activeFiltersCount = computed(() => {
  return Object.values(filter.value).filter((v) => {
    if (Array.isArray(v)) return v.length > 0;
    return v !== undefined && v !== null && String(v).trim() !== '';
  }).length;
});

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

//#region Event handlers
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

//#region Hooks
onMounted(() => {
  loadSpecies();
  loadFacilities();
  opensilex.updateFiltersFromURL(route.query, filter.value);
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
//#endregion

function beforeDestroy() {
  langUnwatcher();
}


function loadSpecies() {
  let service: SpeciesService = opensilex.getService('opensilex.SpeciesService');
  service
    .getAllSpecies()
    .then((http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) => {
      species.value = [];
      for (let i = 0; i < http.response.result.length; i++) {
        speciesByUri.value.set(
          opensilex.getLongUri(http.response.result[i].uri),
          http.response.result[i]
        );
        species.value.push({
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
      facilities.value = [];
      for (let i = 0; i < http.response.result.length; i++) {
        facilities.value.push({
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
      label: t('component.common.name'),
      sortable: true,
      thStyle: { width: '1%' },
      tdClass: 'text-nowrap',
    },
    {
      key: 'start_date',
      label: t('component.experiment.startDate'),
      sortable: true,
    },
    {
      key: 'end_date',
      label: t('component.experiment.endDate'),
      sortable: true,
    },
    {
      key: 'state',
      label: t('component.experiment.search.column.state'),
    },
  ];

  if (!isGermplasmMenuExcluded.value) {
    tableFields.push({
      key: 'species',
      label: t('component.experiment.species'),
    });
  }

  if (!props.noActions) {
    tableFields.push({
      key: 'actions',
      label: t('component.common.actions'),
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
      const message = `${t('component.experiment.view.title')} ${uri} ${t(
          'component.common.success.delete-success-message'
      )}`;
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

defineExpose({
  refresh,
  updateSelectedExperiment
})

//#endregion
</script>

<style scoped lang="scss">
.species-list {
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
  display: inline-block;
  max-width: 40vw;
}

.experiment-layout {
  background: transparent;
}

.experiment-content {
  padding-left: 12px;
}

.advancedFiltersSearch {
  margin-top: 10px;
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
