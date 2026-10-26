<template>
  <div>
    <PageContent
        class="pagecontent"
    >
      <!-- Toggle Sidebar -->
      <div class="searchMenuContainer"
           v-on:click="searchFiltersToggle = !searchFiltersToggle"
           :title="searchFiltersPannel()">
        <div class="searchMenuIcon">
          <i class="ik ik-search"></i>
        </div>
      </div>
      <Transition>
        <div v-show="searchFiltersToggle">
          <SearchFilterField
              @search="refresh()"
              @clear="reset()"
              label="component.germplasms.list.filter.description"
              :showAdvancedSearch="true"
              class="searchFilterField"
              advanced-search-label="">
            <template v-slot:filters>
              <!-- Type -->
              <div>
                <FilterField>
                  <TypeForm
                      v-model:type="filter.rdf_type"
                      :baseType="$opensilex.Oeso.GERMPLASM_TYPE_URI"
                      placeholder="component.germplasms.list.filter.rdfType-placeholder"
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                  ></TypeForm>
                </FilterField>
              </div>

              <!-- Species -->
              <div>
                <FilterField>
                  <FormSelector
                      label="component.germplasms.list.filter.species"
                      placeholder="component.germplasms.list.filter.species-placeholder"
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
                  <label>{{ $t('component.germplasms.list.filter.year') }}</label>
                  <StringFilter
                      v-model:filter="filter.production_year"
                      placeholder="component.germplasms.list.filter.year-placeholder"
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
                  <label>{{ $t('component.germplasms.list.filter.institute') }}</label>
                  <StringFilter
                      v-model:filter="filter.institute"
                      placeholder="component.germplasms.list.filter.institute-placeholder"
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                  ></StringFilter>
                </FilterField>
                <br>
              </div>

              <!-- Name -->
              <div>
                <FilterField>
                  <label>{{ $t('component.germplasms.list.filter.label') }}</label>
                  <StringFilter
                      v-model:filter="filter.name"
                      placeholder="component.germplasms.list.filter.label-placeholder"
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
                      label="component.germplasms.list.filter.experiment"
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
                      label="component.germplasms.list.filter.parents"
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
                      label="component.germplasms.list.filter.germplasm-group"
                      :multiple="false"
                      v-model:selected="filter.germplasm_group"
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                  ></GermplasmGroupSelector>
                </FilterField>
              </div>

              <!-- URI -->
              <div>
                <FilterField>
                  <label>{{ $t('component.germplasms.list.filter.uri') }}</label>
                  <StringFilter
                      v-model:filter="filter.uri"
                      placeholder="component.germplasms.list.filter.uri-placeholder"
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
                      label="component.germplasms.list.filter.metadataKey"
                      class="searchFilter"
                      @handlingEnterKey="refresh()"
                  ></GermplasmAttributesSelector>
                </FilterField>
              </div>

<!--              <div>-->
<!--                <FilterField>-->
<!--                  <GermplasmAttributesValueSelector-->
<!--                      ref="attributesValueSelector"-->
<!--                      v-model:attributeKey="filter.metadataKey"-->
<!--                      v-model:attributeValue="filter.metadataValue"-->
<!--                      class="searchFilter"-->
<!--                      @handlingEnterKey="refresh()"-->
<!--                  ></GermplasmAttributesValueSelector>-->
<!--                </FilterField>-->
<!--              </div>-->

              <!-- Germplasm Visibility -->
              <div>
                <FilterField :fullWidth="true">
                  <FormSelector
                      :label="$t('component.germplasms.list.filter.is_public')"
                      :options="[
                                                { id: true, label: $t('component.germplasms.list.filter.is_public_true') },
                                                { id: false, label: $t('component.germplasms.list.filter.is_public_false') }
                                              ]"
                      v-model:selected="filter.is_public"
                      :async="false"
                      :multiple="false"
                      :showCount="false"
                      :placeholder="$t('component.germplasms.list.filter.is_public-placeholder')"
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
          :fieldKeyToSortableModelLabelMap="{[nameFieldLabel]:'label'}"
          :isSelectable="true"
          @refreshed="onRefreshed"
          @select="$emit('select', $event)"
          @unselect="$emit('unselect', $event)"
          @selectall="$emit('selectall', $event)"
          defaultSortBy="label"
          labelNumberOfSelectedRow="component.germplasms.list.selected"
          iconNumberOfSelectedRow="fa#seedling"
      >
        <template v-slot:selectableTableButtons="{ numberOfSelectedRows }">

          <b-dropdown
              dropright
              class="mb-2 mr-2"
              :small="true"
              :text="$t('component.germplasms.list.display')">

            <b-dropdown-item-button @click="clickOnlySelected()">
              {{ onlySelected ? $t('component.germplasms.list.selected-all') : $t("component.common.selected-only") }}
            </b-dropdown-item-button>
            <b-dropdown-item-button @click="resetSelected()">{{ $t("component.common.resetSelected") }}
            </b-dropdown-item-button>
          </b-dropdown>

          <b-dropdown
              dropright
              class="mb-2 mr-2"
              :small="true"
              :disabled="numberOfSelectedRows == 0"
              :text="$t('component.common.actions')">
            <b-dropdown-item-button
                v-if="user.hasCredential(credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID)"
                @click="createDocument()"
            >{{ $t('component.common.addDocument') }}
            </b-dropdown-item-button>
            <b-dropdown-item-button
                @click="exportCSV(false)"
            >{{ $t('component.germplasms.list.export') }}
            </b-dropdown-item-button>
          </b-dropdown>

          <CreateButton
              v-if="!noActions"
              class="mb-2 mr-2"
              @click="exportCSV(true)"
              :disabled="tableRef?.getTotalRow() === 0"
              label="component.germplasms.list.export-all"
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
              :title="$t('component.germplasms.list.filter.is_public_false')"
          />
        </template>

        <template v-slot:cell(actions)="{data}">
          <b-button-group size="sm">
            <EditButton
                v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID)"
                @click="emit('onEdit', data.item.uri)"
                label="component.germplasms.list.update"
                :small="true"
            ></EditButton>
            <DeleteButton
                v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_DELETE_ID)"
                @click="deleteGermplasm(data.item.uri)"
                label="component.germplasms.list.delete"
                :small="true"
            ></DeleteButton>
          </b-button-group>
        </template>
      </TableAsyncView>
      <ModalForm
          v-if="user.hasCredential(credentials.CREDENTIAL_GERMPLASM_MODIFICATION_ID)"
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

import TableAsyncView from "../common/views/TableAsyncView.vue";
import GermplasmAttributesValueSelector from "./GermplasmAttributesValueSelector.vue";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import PageContent from "@/components/layout/PageContent.vue";
import FilterField from "@/components/common/filters/FilterField.vue";
import TypeForm from "@/components/common/forms/TypeForm.vue";
import FormSelector from "@/components/common/forms/FormSelector.vue";
import StringFilter from "@/components/common/filters/StringFilter.vue";
import SearchFilterField from "@/components/common/filters/SearchFilterField.vue";
import GermplasmSelector from "@/components/germplasm/GermplasmSelector.vue";
import GermplasmGroupSelector from "@/components/germplasm/GermplasmGroupSelector.vue";
import ExperimentSelector from "@/components/experiments/ExperimentSelector.vue";
import CreateButton from "@/components/common/buttons/CreateButton.vue";
import UriLink from "@/components/common/views/UriLink.vue";
import Icon from "@/components/common/views/Icon.vue";
import EditButton from "@/components/common/buttons/EditButton.vue";
import DeleteButton from "@/components/common/buttons/DeleteButton.vue";
import ModalForm from "@/components/common/forms/ModalForm.vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useStore} from "vuex";
import {computed, inject, onBeforeUnmount, onMounted, ref, useTemplateRef} from "vue";
import {useRoute, useRouter} from "vue-router";
import {useI18n} from "vue-i18n";
import {GermplasmSearchFilter, GermplasmService, SpeciesDTO, SpeciesService} from "opensilex-core/lib";

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const {t} = useI18n()
const store = useStore()
const route = useRoute()
const router = useRouter()
const service = opensilex.getService<GermplasmService>('opensilex.GermplasmService')
const resetExperimentSelectorKey = ref<number>(0)

const tableRef = useTemplateRef<InstanceType<typeof TableAsyncView>>("tableRef")
const documentForm = useTemplateRef<InstanceType<typeof ModalForm>>('documentForm')
const attributesValueSelector = useTemplateRef<InstanceType<typeof GermplasmAttributesValueSelector>>('attributesValueSelector')

const props = withDefaults(defineProps<{
  isSelectable?: boolean
  noActions?: boolean
  /**
   * Set an experiment uri, in this case we don't show experiment filter and show only germplasms of this experiment
   */
  experimentUri?: string
}>(),{
  isSelectable: false,
  noActions: false
});

const emit = defineEmits<{
  (e: 'onEdit', uri: string): void
  (e: 'onDelete', uri: string): void
  (e: 'select', item: any): void
  (e: 'unselect', item: any): void
  (e: 'selectall', items: any[]): void
}>()

const user = computed(() => {
  return store.state.user;
})

const credentials = computed(() => {
  return store.state.credentials;
})


const onlySelected = computed(() => {
      return store.state.onlySelected;
    }
)

const lang = computed(() => {
  return store.state.lang;
})

const species = ref([])
const speciesByUri = ref<Map<string, SpeciesDTO>>(
    new Map<string, SpeciesDTO>()
);

const searchFiltersToggle = ref<boolean>(true)
const nameFieldLabel = ref<string>("name");


const filter = ref({
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
});

function reset()
{
  filter.value = {
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

  refresh();
  resetExperimentSelectorKey.value++;
}

function onItemUnselected(row)
{
  tableRef.value.onItemUnselected(row);
}

function onItemSelected(row)
{
  tableRef.value.onItemSelected(row);
}

function clickOnlySelected()
{
  tableRef.value.toggleOnlySelected();
}

function resetSelected()
{
  tableRef.value.resetSelection();
}

function setInitiallySelectedItems(initiallySelectedItems: any)
{
  tableRef.value?.setInitiallySelectedItems?.(initiallySelectedItems);
}

function getSelected()
{
  return tableRef.value.getSelected();
}
let langUnwatcher: (() => void) | undefined;

onMounted(() => {
  langUnwatcher = store.watch(
      () => store.getters.language,
      (_lang) => {
        updateLang();
      }
  );
});

onBeforeUnmount(() => {
  langUnwatcher?.();
});

loadSpecies();
opensilex.updateFiltersFromURL(route.query, filter.value);


const fields = computed(() => {
  let tableFields = [
    {
      key: nameFieldLabel.value,
      label: "component.germplasms.list.name",
      sortable: true
    },
    {
      key: "rdf_type_name",
      label: "component.germplasms.list.rdfType",
      sortable: true
    },
    {
      key: "species_name",
      label: "component.germplasms.list.speciesLabel"
    },
    {
      key: "germplasm_is_public",
      label: "component.germplasms.list.is_public"
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

const speciesSelector = ref<any>()

function refresh()
{
  updateSelectedGermplasm()
  tableRef.value.setPage(1);
}

function updateSelectedGermplasm()
{
  opensilex.updateURLParameters(filter.value);
  tableRef.value.setOnlySelected(false);
  tableRef.value.refresh();
}

function searchGermplasm(options)
{
  // this.updateExportFilters();
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
      addMetadataFilter(),
      filter.value.is_public,
      options.orderBy,
      options.currentPage,
      options.pageSize
  );
}


function exportCSV(exportAll: boolean)
{
  let path = "/core/germplasm/export";
  let today = new Date();
  let filename = "export_germplasm_" + today.getFullYear() + String(today.getMonth() + 1).padStart(2, '0') + String(today.getDate()).padStart(2, '0');

  let exportDto: GermplasmSearchFilter = {
    uri: filter.value.uri,
    rdf_type: filter.value.rdf_type,
    name: filter.value.name,
    production_year: filter.value.production_year,
    species: filter.value.species,
    institute: filter.value.institute,
    experiment: filter.value.experiment,
    order_by: tableRef.value?.getOrderBy?.(),
    metadata: addMetadataFilter()
  };

  // export only selected URIS (+matching with filter)
  if (!exportAll) {
    let objectURIs = [];
    for (let select of tableRef.value.getSelected()) {
      objectURIs.push(select.uri);
    }
    Object.assign(exportDto, {
      uris: objectURIs
    });
    exportDto.page_size = objectURIs.length;
  } else {
    exportDto.page_size = tableRef.value.getTotalRow();
  }

  opensilex
      .downloadFilefromPostService(path, filename, "csv", exportDto, lang.value);
}

function loadSpecies()
{
  let service: SpeciesService = opensilex.getService<SpeciesService>(
      "opensilex.SpeciesService"
  );
  service
      .getAllSpecies()
      .then((http: HttpResponse<OpenSilexResponse<Array<SpeciesDTO>>>) => {
        species.value = [];
        for (let i = 0; i < http.response.result.length; i++) {
          speciesByUri.value.set(
              http.response.result[i].uri,
              http.response.result[i]
          );
          species.value.push({
            id: http.response.result[i].uri,
            label: http.response.result[i].name
          });
        }
      })
      .catch(opensilex.errorHandler);
}

function updateLang()
{
  loadSpecies();
  refresh();
}

function addMetadataFilter()
{

  // undefined, null or empty : https://262.ecma-international.org/5.1/#sec-9.2
  if (!filter.value.metadataKey) {
    return undefined;
  }

  // set empty string for JSON.stringify method, by default undefined fields are not serialized
  let value = filter.value.metadataValue ? filter.value.metadataValue : "";

  // if metadataValue is undefined, match all germplasm which have the given metadata key
  // else, match germplasm with the good key and value
  return JSON.stringify({
    [filter.value.metadataKey]: value
  });
}

function createDocument()
{
  documentForm.value.showCreateForm();
}

function initForm()
{
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
      keywords: undefined
    },
    file: undefined
  }
}

function searchFiltersPannel()
{
  return t("searchfilter.label")
}

function onRefreshed()
{
  setTimeout(() => {
    if (tableRef.value?.selectAll === true && tableRef.value?.selectedItems?.length !== tableRef.value?.totalRow) {
      tableRef.value.selectAll = false;
    }
  }, 1);
}

function deleteGermplasm(uri)
{
  tableRef.value.checkSelectedItems(uri);
  emit('onDelete', uri);
}

defineExpose({
  refresh,
  reset,
  getSelected,
  onItemSelected,
  onItemUnselected,
  setInitiallySelectedItems,
  clickOnlySelected,
  resetSelected
})
</script>

<style scoped lang="scss">
.clear-btn {
  color: rgb(229, 227, 227) !important;
  border-color: rgb(229, 227, 227) !important;
  border-left: none !important;
}
</style>
