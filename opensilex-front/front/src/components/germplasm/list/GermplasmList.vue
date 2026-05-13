<template>
  <!-- Barre Actions / Counts / Selection -->
  <n-space
      class="listActionButtons"
      :class="[searchFiltersToggle ? 'filtersNotCollapsed' : 'filtersCollapsed']"
  >
    <!-- Dropdown Affichage / sélection -->
    <n-dropdown
        :options="displayDropdownOptions"
        @select="(key: string) => displayDropdownOptionsMap.get(key).clicked()"
        trigger="click"
        placement="bottom-end"
    >
      <n-button size="small" class="greenThemeColor">
        {{ t('display') }}
      </n-button>
    </n-dropdown>

    <!-- Dropdown Actions -->
    <n-dropdown
        :options="actionDropdownOptions"
        @select="(key: string) => actionDropdownOptionsMap.get(key).clicked()"
        trigger="click"
        placement="bottom-end"
        :disabled="selectedCount === 0"
    >
      <n-button
          size="small"
          :disabled="selectedCount === 0"
          :class="selectedCount === 0 ? 'btn-disabled' : 'greenThemeColor'"
      >
        {{ t('actions') }}
      </n-button>
    </n-dropdown>

    <!-- Export all -->
    <n-button
      size="small"
      class="greenThemeColor"
      @click="exportCSV(true)"
      :disabled="tableRef?.totalRow === 0"
    >
      {{ t('export-all') }}
    </n-button>

    <!-- Affichage Counts / sélection -->
    <div class="displayAndListSelectionCount">
      <div v-if="paginationInfo.hasResults">
        <strong>
          <span class="ml-1">
            {{
              t('component.common.list.pagination.nbEntries', {
                limit: paginationInfo.start,
                offset: paginationInfo.end,
                totalRow: n(paginationInfo.total)
              })
            }}
          </span>
        </strong>
      </div>
      <div v-else>
        <strong>
          <span class="ml-1">
            {{ t('component.common.list.pagination.noEntries') }}
          </span>
        </strong>
      </div>
      <span> | </span>
      <span>
        {{ t('DeviceList.selected') }} :
        <span class="badge badge-pill greenThemeColor">{{ selectedCount }}</span>
      </span>
    </div>
  </n-space>

  <n-layout has-sider class="germplasm-layout">
    <!-- Bouton loupe -->
    <n-space class="mb-2 me-1" align="start">
      <n-button
          quaternary
          circle
          @click="searchFiltersToggle = !searchFiltersToggle"
          :title="searchFiltersPanel"
          :class="{ greenThemeColor: searchFiltersToggle }"
          class="globalFiltersSearchButton"
      >
        <i class="bi bi-search filtersGlobalSearchIcon"></i>

        <div
            v-show="searchFiltersToggle && activeFiltersCount > 0"
            class="filters-count-badge"
        >
          ( {{ activeFiltersCount }} )
        </div>
      </n-button>
    </n-space>


    <!-- Sidebar / Filtres -->
    <n-layout-sider
        v-model:collapsed="searchFiltersToggle"
        :collapsed-width="0"
        :width="360"
        collapse-mode="width"
        show-trigger
        bordered
        class="device-sider"
    >
      <n-space class="p-3" vertical>
        <n-form label-placement="top" size="small" @submit.prevent.stop="refresh">
          <!-- Type -->
          <n-form-item class="compact-form-item">
            <TypeForm
                v-model:type="filter.rdf_type"
                :baseType="opensilex.Oeso.GERMPLASM_TYPE_URI"
                :placeholder="t('filter.rdfType-placeholder')"
                class="searchFilter"
                @handlingEnterKey="refresh()"
            ></TypeForm>
          </n-form-item>

          <!-- Species -->
          <n-form-item class="compact-form-item">
            <FormSelector
                :label="t('filter.species')"
                :placeholder="t('filter.species-placeholder')"
                :multiple="false"
                v-model:selected="filter.species"
                :options="species"
                class="searchFilter"
                @handlingEnterKey="refresh()"
            ></FormSelector>
          </n-form-item>

          <!-- Year -->
          <n-form-item :label="t('filter.year')" class="compact-form-item">
            <StringFilter
                v-model:filter="filter.production_year"
                :placeholder="t('filter.year-placeholder')"
                type="number"
                class="searchFilter"
                @handlingEnterKey="refresh()"
            ></StringFilter>
          </n-form-item>

          <!-- Institute -->
          <n-form-item :label="t('filter.institute')" class="compact-form-item">
            <StringFilter
                v-model:filter="filter.institute"
                :placeholder="t('filter.institute-placeholder')"
                class="searchFilter"
                @handlingEnterKey="refresh()"
            ></StringFilter>
          </n-form-item>

          <!-- Name -->
          <n-form-item :label="t('filter.label')" class="compact-form-item">
            <StringFilter
                v-model:filter="filter.name"
                :placeholder="t('filter.label-placeholder')"
                class="searchFilter"
                @handlingEnterKey="refresh()"
            ></StringFilter>
          </n-form-item>

          <!-- Experiments -->
          <n-form-item class="compact-form-item" v-if="!experimentUri">
            <ExperimentSelector
                :label="t('filter.experiment')"
                v-model:experiments="filter.experiment"
                class="searchFilter"
                @handlingEnterKey="refresh()"
                :key="resetExperimentSelectorKey"
            ></ExperimentSelector>
          </n-form-item>

          <!-- Germplasm Parents filter -->
          <n-form-item class="compact-form-item">
            <GermplasmSelector
                :label="t('filter.parents')"
                :multiple="true"
                v-model:germplasm="filter.parent_germplasms"
                class="searchFilter"
                @handlingEnterKey="refresh()"
            ></GermplasmSelector>
          </n-form-item>

          <!-- Germplasm Group -->
          <n-form-item class="compact-form-item">
            <GermplasmGroupSelector
                :label="t('filter.germplasm-group')"
                :multiple="false"
                v-model:germplasmGroup="filter.germplasm_group"
                class="searchFilter"
                @handlingEnterKey="refresh()"
            ></GermplasmGroupSelector>
          </n-form-item>

          <!-- URI -->
          <n-form-item :label="t('filter.uri')" class="compact-form-item">
            <StringFilter
                v-model:filter="filter.uri"
                :placeholder="t('filter.uri-placeholder')"
                class="searchFilter"
                @handlingEnterKey="refresh()"
            ></StringFilter>
            <br>
          </n-form-item>

          <!-- Germplasm Attributes -->
          <n-form-item class="compact-form-item">
            <GermplasmAttributesSelector
                v-model:germplasmAttribute="filter.metadataKey"
                :label="t('filter.metadataKey')"
                class="searchFilter"
                @handlingEnterKey="refresh()"
            ></GermplasmAttributesSelector>
          </n-form-item>

          <n-form-item class="compact-form-item">
            <GermplasmAttributesValueSelector
                v-model:attributeKey="filter.metadataKey"
                v-model:attributeValue="filter.metadataValue"
                class="searchFilter"
                @handlingEnterKey="refresh()"
            ></GermplasmAttributesValueSelector>
          </n-form-item>

          <!-- Germplasm Visibility -->
          <n-form-item class="compact-form-item">
            <FormSelector
                :label="t('filter.is_public')"
                :options="[
                        { id: true, label: t('filter.is_public_true') },
                        { id: false, label: t('filter.is_public_false') }
                      ]"
                v-model:selected="filter.is_public"
                :async="false"
                :multiple="false"
                :showCount="false"
                :placeholder="t('filter.is_public-placeholder')"
            />
          </n-form-item>
        </n-form>
      </n-space>
    </n-layout-sider>
    <!-- Contenu Liste -->
    <n-layout-content class="germplasm-content">
      <TableAsyncView
          ref="tableRef"
          :searchMethod="searchGermplasm"
          :fields="fields"
          :fieldKeyToSortableModelLabelMap="{name: 'label'}"
          :isSelectable="true"
          @select="$emit('select', $event)"
          @unselect="$emit('unselect', $event)"
          @selectall="$emit('selectall', $event)"
          defaultSortBy="label"
          :labelNumberOfSelectedRow="t('selected')"
          :showHeaderCount="false"
          iconNumberOfSelectedRow="fa#seedling"
      >
        <template v-slot:cell(name)="{data}">
          <UriLink
              :uri="data.item.uri"
              :value="data.item.name"
              :to="{path: '/germplasm/details/'+ encodeURIComponent(data.item.uri)}"
          ></UriLink>
        </template>

        <template v-slot:cell(germplasm_is_public)="{ data }">
          <Icon
              v-if="data.item.is_public === false"
              icon="ik#ik-lock"
              class="text-secondary"
              style="font-size: 1.2em"
              :title="$t('GermplasmList.filter.is_public_false')"
          />
        </template>

        <template v-slot:cell(actions)="{data}">
          <EditButton
              v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID)"
              @click="$emit('onEdit', data.item.uri)"
              :label="t('update')"
              :small="true"
          ></EditButton>
          <DeleteButton
              v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_DELETE_ID)"
              @click="deleteGermplasm(data.item.uri)"
              :label="t('delete')"
              :small="true"
          ></DeleteButton>
        </template>
      </TableAsyncView>
    </n-layout-content>
  </n-layout>
  <ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID)"
      ref="documentForm"
      component="opensilex-DocumentForm"
      :createTitle="t('component.common.addDocument')"
      modalSize="lg"
      :initForm="initForm"
      icon="ik#ik-file-text"
  ></ModalForm>
</template>

<script setup lang="ts">
import PageContent from "@/components/layout/PageContent.vue";
import TypeForm from "@/components/common/forms/TypeForm.vue";
import FormSelector from "@/components/common/forms/FormSelector.vue";
import StringFilter from "@/components/common/filters/StringFilter.vue";
import ExperimentSelector from "@/components/experiments/ExperimentSelector.vue";
import GermplasmSelector from "@/components/germplasm/selector/GermplasmSelector.vue";
import GermplasmGroupSelector from "@/components/germplasm/selector/GermplasmGroupSelector.vue";
import GermplasmAttributesSelector from "@/components/germplasm/selector/GermplasmAttributesSelector.vue";
import GermplasmAttributesValueSelector from "@/components/germplasm/selector/GermplasmAttributesValueSelector.vue";
import TableAsyncView from "@/components/common/views/TableAsyncView.vue";
import CreateButton from "@/components/common/buttons/CreateButton.vue";
import UriLink from "@/components/common/views/UriLink.vue";
import Icon from "@/components/common/views/Icon.vue";
import EditButton from "@/components/common/buttons/EditButton.vue";
import DeleteButton from "@/components/common/buttons/DeleteButton.vue";
import ModalForm from "@/components/common/forms/ModalForm.vue";
import {computed, inject, onBeforeUnmount, onMounted, ref, useTemplateRef, VNodeChild} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useStore} from "vuex";
import {useI18n} from "vue-i18n";
import {useRoute, useRouter} from "vue-router";
import {GermplasmService} from "opensilex-core/api/germplasm.service";
import {SpeciesDTO} from "opensilex-core/model/speciesDTO";
import {GermplasmSearchFilter} from "opensilex-core/model/germplasmSearchFilter";
import {OrderBy} from "opensilex-core/model/orderBy";
import {SpeciesService} from "opensilex-core/api/species.service";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import SearchFilterField from "@/components/common/filters/SearchFilterField.vue";
import FilterField from "@/components/common/filters/FilterField.vue";
import {NamedResourceDTO} from "opensilex-core/model/namedResourceDTO";
import {
  DropdownOption,
  NButton,
  NDropdown,
  NSpace,
  NLayout,
  NLayoutSider,
  NForm,
  NFormItem,
  NLayoutContent
} from "naive-ui";

//#region Public
const props = withDefaults(defineProps<{
  experimentUri: string
  isSelectable?: boolean
  noActions?: boolean
}>(), {
  isSelectable: false,
  noActions: false
});

const emit = defineEmits<{
  onEdit: []
  onDelete: [uri: string]
  select: [item: NamedResourceDTO]
  unselect: [item: NamedResourceDTO]
  selectall: [items: NamedResourceDTO[]]
}>()

defineExpose({
  refresh,
  updateSelectedGermplasm
})
//#endregion

//#region Private
const displayDropdownOptionsMap = ref<Map<string, {
  label: string | (() => VNodeChild),
  clicked: () => void
}>>(new Map([
  ['selectedOnly', {
    label: () => onlySelected.value ? t('selected-all') : t("component.common.selected-only"),
    clicked: () => {
      tableRef.value.clickOnlySelected()
    }
  }],
  ['resetSelected', {
    label: () => t("component.common.resetSelected"),
    clicked: () => {
      tableRef.value.resetSelection()
    }
  }]
]));
const displayDropdownOptions = computed<Array<DropdownOption>>(() => displayDropdownOptionsMap.value.entries().map(([key, option]) => ({key, ...option})).toArray());
const actionDropdownOptionsMap = ref<Map<string, {
  label: string | (() => VNodeChild),
  clicked: () => void
}>>(new Map([
  ['addDocument', {
    label: () => t('component.common.addDocument'),
    clicked: () => {
      documentForm.value.showCreateForm()
    }
  }],
  ['exportCsv', {
    label: () => t('export'),
    clicked: () => {
      exportCSV(false)
    }
  }]
]));
const actionDropdownOptions = computed<Array<DropdownOption>>(() => actionDropdownOptionsMap.value.entries().map(([key, option]) => ({key, ...option})).toArray());

const opensilex = inject<OpenSilexVuePlugin>("$opensilex")
const store = useStore();
const {t, n} = useI18n();
const route = useRoute();
const service = opensilex.getService<GermplasmService>("opensilex.GermplasmService")
const speciesService = opensilex.getService<SpeciesService>("opensilex.SpeciesService")

const documentForm = useTemplateRef<InstanceType<typeof ModalForm>>("documentForm");
const tableRef = useTemplateRef<InstanceType<typeof TableAsyncView>>("tableRef");

const resetExperimentSelectorKey = ref(0);
const species = ref<Array<{ id: string, label: string }>>([])
const speciesByUri = new Map<String, SpeciesDTO>();
const searchFiltersToggle = ref(true);
const filter = ref(initFilters())

const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);
const lang = computed(() => store.state.lang);
const onlySelected = computed(() => tableRef.value.onlySelected);
const selectedCount = computed(() => (tableRef.value?.getSelected() ?? []).length);
const paginationInfo = computed(() => {
  return tableRef.value?.getPaginationInfo() ?? {
    start: 0,
    end: 0,
    total: 0,
    hasResults: false
  }
});
const fields = computed(() => {
  let tableFields = [
    {
      key: "name",
      label: "GermplasmList.name",
      sortable: true
    },
    {
      key: "rdf_type_name",
      label: "GermplasmList.rdfType",
      sortable: true
    },
    {
      key: "species_name",
      label: "GermplasmList.speciesLabel"
    },
    {
      key: "germplasm_is_public",
      label: "GermplasmList.is_public"
    }

  ];
  if (!props.noActions) {
    tableFields.push({
      key: "actions",
      label: "component.common.actions"
    });
  }
  return tableFields;
});

const unwatchLang = store.watch(
    () => store.getters.language,
    () => {
      loadSpecies();
      refresh();
    }
);

function initFilters() {
  return {
    rdf_type: undefined,
    name: undefined,
    species: undefined,
    production_year: undefined,
    institute: undefined,
    experiment: undefined,
    parent_germplasms: [],
    parent_germplasms_m: [],
    parent_germplasms_f: [],
    germplasm_group: undefined,
    uri: undefined,
    is_public: undefined,
    metadataKey: undefined,
    metadataValue: undefined
  };
}

function refresh() {
  updateSelectedGermplasm();
}

function updateSelectedGermplasm() {
  opensilex.updateFiltersFromURL(route.query, filter.value);
  tableRef.value.onlySelected = false;
  tableRef.value.refresh();
}

function reset() {
  filter.value = initFilters();
  resetExperimentSelectorKey.value += 1;
}

function loadSpecies() {
  speciesService.getAllSpecies()
      .then((http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) => {
        const speciesList: Array<{ id: string, label: string }> = [];
        http.response.result.forEach(sp => {
          speciesByUri.set(sp.uri, sp);
          speciesList.push({id: sp.uri, label: sp.name});
        });
        species.value = speciesList;
      })
      .catch(opensilex.errorHandler);
}

function deleteGermplasm(uri: string) {
  tableRef.value.checkSelectedItems(uri);
  emit("onDelete", uri);
}

function searchGermplasm(options: { orderBy: string[], currentPage: number, pageSize: number }) {
  return service.searchGermplasm(
      filter.value.uri,
      filter.value.rdf_type,
      filter.value.name,
      undefined,
      filter.value.production_year,
      filter.value.species,
      undefined,
      undefined,
      filter.value.germplasm_group,
      filter.value.institute,
      props.experimentUri || filter.value.experiment,
      filter.value.parent_germplasms,
      filter.value.parent_germplasms_m,
      filter.value.parent_germplasms_f,
      makeMetadataFilter(),
      filter.value.is_public,
      options.orderBy,
      options.currentPage,
      options.pageSize
  );
}

function makeMetadataFilter(): string | undefined {
  if (!filter.value.metadataKey) {
    return undefined;
  }

  return JSON.stringify({[filter.value.metadataKey]: filter.value.metadataValue ?? ""});
}

function exportCSV(exportAll: boolean) {
  const path = "/core/germplasm/export";
  const today = new Date();
  const filename = "export_germplasm_" + today.getFullYear() + String(today.getMonth() + 1).padStart(2, '0') + String(today.getDate()).padStart(2, '0');
  const exportDto: GermplasmSearchFilter = {
    uri: filter.value.uri,
    rdf_type: filter.value.rdf_type,
    name: filter.value.name,
    production_year: filter.value.production_year,
    species: filter.value.species,
    institute: filter.value.institute,
    experiment: filter.value.experiment,
    // The cast to OrderBy is incorrect (this is an array of string), but passing an array of string is actually accepted
    // by the API (the OrderBy class has a constructor that accepts a String). The generated DTO by Swagger is incorrect,
    // the type should be Array<OrderBy | string>.
    order_by: tableRef.value.getOrderBy() as OrderBy[],
    metadata: makeMetadataFilter()
  };

  if (!exportAll) {
    const objectURIs = tableRef.value.getSelected().map(s => s.uri);
    exportDto.uris = objectURIs;
    exportDto.page_size = objectURIs.length;
  } else {
    exportDto.page_size = tableRef.value.getTotalRow();
  }
  opensilex.downloadFilefromPostService(path, filename, "csv", exportDto, lang.value);
}

onMounted(() => {
  loadSpecies();
  opensilex.updateFiltersFromURL(route.query, filter.value);
})

onBeforeUnmount(() => {
  unwatchLang();
})
//#endregion
</script>

<style scoped lang="scss">
// neutralisation des classes injectées par naive dans les <n-form-item> qui créent des espaces indésirés entre les champs
:deep(.compact-form-item) {
  --n-label-height: 0px !important;
  --n-label-padding: 0 !important;
}

.listActionButtons {
  position: relative;
  display: flex;
  gap: 0 !important;
}

.filtersNotCollapsed {
  margin-left: 55px;
}

.filtersCollapsed {
  margin-left: 415px;
}

.germplasm-layout {
  background: transparent;
}

.germplasm-content {
  padding-left: 12px;
}
</style>

<i18n>

en:
  uri: URI
  name: Name
  rdfType: Type
  fromSpecies: Species URI
  speciesLabel: Species
  update: Update Germplasm
  delete: Delete Germplasm
  selectLabel: Select Germplasm
  selected: Selected Germplasm
  export: Export Germplasm list
  selected-all: All Germplasm
  is_public: Visibility
  display: Display
  actions: Actions
  export-all: Export all
  filter:
    description: Germplasm Search
    species: Species
    species-placeholder: Select a species
    year: Production year
    year-placeholder: Enter a year
    institute: Institute code
    institute-placeholder: Enter an institute code
    label: Name
    label-placeholder: Enter germplasm name
    rdfType: Type
    rdfType-placeholder: Select a germplasm type
    experiment: Experiment
    experiment-placeholder: Select an experiment
    uri: URI
    uri-placeholder: Enter a part of an uri
    search: Search
    reset: Reset
    metadataKey: Attribute name
    metadataValue: Attribute value
    germplasm-group: Germplasm Group
    parents: Parents
    is_public: Visibility
    is_public_true: Public
    is_public_false: Private
    is_public-placeholder: Select germplasm visibility

fr:
  uri: URI
  label: Nom
  rdfType: Type
  fromSpecies: URI de l'espèce
  speciesLabel: Espèce
  update: Editer le germplasm
  delete: Supprimer le germplasm
  selectLabel: Sélection de Ressources Génétiques
  selected: Ressource(s) Génétique(s) Sélectionnée(s)
  export: Exporter la liste
  selected-all: Toutes les ressources génétiques
  is_public: Visibilité
  display: Affichage
  actions: Actions
  export-all: Tout exporter
  filter:
    description: Recherche de Ressources Génétiques
    species: Espèce
    species-placeholder: Sélectionner une espèce
    year: Année de production
    year-placeholder: Entrer une année
    institute: Code Institut
    institute-placeholder: Entrer le code d'un institut
    label: Nom
    label-placeholder: Entrer un nom de germplasm
    rdfType: Type
    rdfType-placeholder: Sélectionner un type de germplasm
    experiment: Expérimentation
    experiment-placeholder: Sélectionner une expérimentation
    uri: URI
    uri-placeholder: Entrer une partie d'une uri
    search: Rechercher
    reset: Réinitialiser
    metadataKey: Nom de l'attribut
    metadataValue: Valeur de l'attribut
    germplasm-group: Groupe de ressources génétiques
    parents: Parents
    is_public: Visibilité
    is_public_true: Publique
    is_public_false: Privé
    is_public-placeholder: Sélectionner la visibilité du germplasm
</i18n>