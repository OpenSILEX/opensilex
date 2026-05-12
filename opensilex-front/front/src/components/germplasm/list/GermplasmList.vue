<template>
  <div>
    <PageContent
        class="pagecontent"
    >
      <!-- Toggle Sidebar -->
      <div class="searchMenuContainer"
           @click="searchFiltersToggle = !searchFiltersToggle"
           :title="t('searchfilter.label')">
        <div class="searchMenuIcon">
          <i class="ik ik-search"></i>
        </div>
      </div>
      <Transition>
        <div v-show="searchFiltersToggle">
          <SearchFilterField
              @search="refresh()"
              @clear="reset()"
              :label="t('filter.description')"
              :showAdvancedSearch="true"
              class="searchFilterField"
          >
            <template v-slot:filters>
              <!-- Type -->
              <div>
                <FilterField>
                  <TypeForm
                      v-model:type="filter.rdf_type"
                      :baseType="opensilex.Oeso.GERMPLASM_TYPE_URI"
                      :placeholder="t('filter.rdfType-placeholder')"
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                  ></TypeForm>
                </FilterField>
              </div>

              <!-- Species -->
              <div>
                <FilterField>
                  <FormSelector
                      :label="t('filter.species')"
                      :placeholder="t('filter.species-placeholder')"
                      :multiple="false"
                      v-model:selected="filter.species"
                      :options="species"
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                  ></FormSelector>
                </FilterField>
              </div>

              <!-- Year -->
              <div>
                <FilterField>
                  <label>{{ t('filter.year') }}</label>
                  <StringFilter
                      v-model:filter="filter.production_year"
                      :placeholder="t('filter.year-placeholder')"
                      type="number"
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                  ></StringFilter>
                </FilterField>
                <br>
              </div>

              <!-- Institute -->
              <div>
                <FilterField>
                  <label>{{ t('filter.institute') }}</label>
                  <StringFilter
                      v-model:filter="filter.institute"
                      :placeholder="t('filter.institute-placeholder')"
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                  ></StringFilter>
                </FilterField>
                <br>
              </div>

              <!-- Name -->
              <div>
                <FilterField>
                  <label>{{ t('filter.label') }}</label>
                  <StringFilter
                      v-model:filter="filter.name"
                      placeholder="t('filter.label-placeholder')"
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                  ></StringFilter>
                </FilterField>
                <br>
              </div>

              <!-- Experiments -->
              <div v-if="!experimentUri">
                <FilterField>
                  <ExperimentSelector
                      :label="t('filter.experiment')"
                      v-model:experiments="filter.experiment"
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                      :key="resetExperimentSelectorKey"
                  ></ExperimentSelector>
                </FilterField>
              </div>

              <!-- Germplasm Parents filter -->
              <div>
                <FilterField>
                  <GermplasmSelector
                      :label="t('filter.parents')"
                      :multiple="true"
                      v-model:germplasm="filter.parent_germplasms"
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                  ></GermplasmSelector>
                </FilterField>
              </div>

              <!-- Germplasm Group -->
              <div>
                <FilterField>
                  <GermplasmGroupSelector
                      :label="t('filter.germplasm-group')"
                      :multiple="false"
                      v-model:germplasmGroup="filter.germplasm_group"
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                  ></GermplasmGroupSelector>
                </FilterField>
              </div>

              <!-- URI -->
              <div>
                <FilterField>
                  <label>{{ t('filter.uri') }}</label>
                  <StringFilter
                      v-model:filter="filter.uri"
                      :placeholder="t('filter.uri-placeholder')"
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                  ></StringFilter>
                </FilterField>
                <br>
              </div>
            </template>

            <template v-slot:advancedSearch>

              <!-- Germplasm Attributes -->
              <div>
                <FilterField>
                  <GermplasmAttributesSelector
                      v-model:germplasmAttribute="filter.metadataKey"
                      :label="t('filter.metadataKey')"
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                  ></GermplasmAttributesSelector>
                </FilterField>
              </div>

              <div>
                <FilterField>
                  <GermplasmAttributesValueSelector
                      ref="attributesValueSelector"
                      v-model:attributeKey="filter.metadataKey"
                      v-model:attributeValue="filter.metadataValue"
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                  ></GermplasmAttributesValueSelector>
                </FilterField>
              </div>

              <!-- Germplasm Visibility -->
              <div>
                <FilterField :fullWidth="true">
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
                </FilterField>
              </div>
            </template>
          </SearchFilterField>
        </div>
      </Transition>
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
          :showHeaderCount="true"
          iconNumberOfSelectedRow="fa#seedling"
      >
        <template v-slot:selectableTableButtons="{ numberOfSelectedRows }">
          <b-dropdown
              dropright
              class="mb-2 mr-2"
              :small="true"
              :text="$t('VariableList.display')">

            <b-dropdown-item-button @click="clickOnlySelected()">
              {{ onlySelected ? $t('GermplasmList.selected-all') : $t("component.common.selected-only") }}
            </b-dropdown-item-button>
            <b-dropdown-item-button @click="resetSelected()">{{ $t("component.common.resetSelected") }}
            </b-dropdown-item-button>
          </b-dropdown>

          <b-dropdown
              dropright
              class="mb-2 mr-2"
              :small="true"
              :disabled="numberOfSelectedRows == 0"
              text=actions>
            <b-dropdown-item-button
                v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
                @click="createDocument()"
            >{{ $t('component.common.addDocument') }}
            </b-dropdown-item-button>
            <b-dropdown-item-button
                @click="exportCSV(false)"
            >{{ $t('GermplasmList.export') }}
            </b-dropdown-item-button>
          </b-dropdown>

          <CreateButton
              v-if="!noActions"
              class="mb-2 mr-2"
              @click="exportCSV(true)"
              :disabled="tableRef.totalRow === 0"
              label="ScientificObjectList.export-all"
          ></CreateButton>

        </template>
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
          <b-button-group size="sm">
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
          </b-button-group>
        </template>
      </TableAsyncView>
      <ModalForm
          v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID)"
          ref="documentForm"
          component="opensilex-DocumentForm"
          createTitle="component.common.addDocument"
          modalSize="lg"
          :initForm="initForm"
          icon="ik#ik-file-text"
      ></ModalForm>
    </PageContent>
  </div>

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
import {computed, inject, onBeforeUnmount, onMounted, ref, useTemplateRef} from "vue";
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
const opensilex = inject<OpenSilexVuePlugin>("$opensilex")
const store = useStore();
const {t} = useI18n();
const route = useRoute();
const router = useRouter();
const service = opensilex.getService<GermplasmService>("opensilex.GermplasmService")
const speciesService = opensilex.getService<SpeciesService>("opensilex.SpeciesService")

const documentForm = useTemplateRef<InstanceType<typeof ModalForm>>("documentForm");
const tableRef = useTemplateRef<InstanceType<typeof TableAsyncView>>("tableRef");
const attributesValueSelector = useTemplateRef<InstanceType<typeof GermplasmAttributesValueSelector>>("attributesValueSelector");


const resetExperimentSelectorKey = ref(0);
const species = ref<Array<{ id: string, label: string }>>([])
const speciesByUri = new Map<String, SpeciesDTO>();
const searchFiltersToggle = ref(true);
const filter = ref(initFilters())

const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);
const lang = computed(() => store.state.lang);
const onlySelected = computed(() => tableRef.value.onlySelected)
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
})

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
  opensilex.downloadFilefromPostService(path, filename, "csv", exportDto, this.lang);
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