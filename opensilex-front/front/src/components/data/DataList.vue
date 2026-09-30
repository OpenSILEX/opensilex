<template>
  <div>
    <TableAsyncView
        ref="tableRef"
        :searchMethod="searchDataList"
        :countMethod="countDataList"
        :fields="fields"
        defaultSortBy="date"
        :defaultSortDesc="true"
    >
      <template v-slot:cell(target)="{ data }">
        <UriLink
            :uri="data.item.target"
            :value="objects[data.item.target]"
            :to="{
            path: opensilex.getTargetPath(data.item.target, contextUri, objectsPath[data.item.target])
          }"
        ></UriLink>
      </template>

      <template v-slot:cell(variable)="{ data }">
        <UriLink
            :uri="data.item.variable"
            :value="getVariableName(data.item.variable)"
            :to="{
            path: '/variable/details/' + encodeURIComponent(data.item.variable),
          }"
        ></UriLink>
      </template>

      <template v-slot:cell(provenance)="{ data }">
        <UriLink
            :uri="data.item.provenance.uri"
            :value="provenances[data.item.provenance.uri]"
            :to="{
            path: '/provenances/details/' +
              encodeURIComponent(data.item.provenance.uri),
          }"
        ></UriLink>
      </template>

      <template v-slot:cell(actions)="{data}">
        <n-button-group class="btn-group btn-group-sm">
          <DetailButton
              v-if="user.hasCredential(credentials.CREDENTIAL_DEVICE_MODIFICATION_ID)"
              @click="showDataDetailsModal(data.item)"
              label="DataView.list.details"
              :small="true"
          ></DetailButton>
        </n-button-group>
      </template>

    </TableAsyncView>

    <DataProvenanceModalView
        ref="dataProvenanceModalView"
    ></DataProvenanceModalView>
  </div>
</template>

<script setup lang="ts">
import {computed, inject, ref, useTemplateRef} from "vue";
import {useRoute} from "vue-router";
import {useStore} from "vuex";
import {DataGetSearchDTO} from "opensilex-core/model/dataGetSearchDTO";
import UriLink from "@/components/common/views/UriLink.vue";
import TableAsyncView from "@/components/common/views/TableAsyncView.vue";
import DetailButton from "@/components/common/buttons/DetailButton.vue";
import DataProvenanceModalView from "@/components/data/DataProvenanceModalView.vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {DataService, OntologyService, VariablesService} from "../../../../../opensilex-core/front/src/lib";
import {NButtonGroup} from "naive-ui";

const props = withDefaults(defineProps<{
  contextUri?: string
}>(), {
  contextUri: ""
})

const filter = defineModel<any>("listFilter", {
  default: () => ({
    start_date: null,
    end_date: null,
    variables: [],
    provenance: null,
    experiments: [],
    scientificObjects: [],
    targets: [],
    devices: [],
    facilities: [],
    operators: [],
    batch_uri: null,
  }),
});

const opensilex = inject<OpenSilexVuePlugin>('opensilex')
const store = useStore()
const route = useRoute()

const dataService = opensilex.getService<DataService>("opensilex.DataService");
const ontologyService = opensilex.getService<OntologyService>("opensilex.OntologyService");
const variablesService = opensilex.getService<VariablesService>("opensilex.VariablesService");

const tableRef = useTemplateRef<InstanceType<typeof TableAsyncView>>('tableRef')
const dataProvenanceModalView = useTemplateRef<InstanceType<typeof DataProvenanceModalView>>('dataProvenanceModalView')

const objects = ref<Record<string, string>>({});
const objectsPath = ref<Record<string, string>>({});
const variableNames = ref<Record<string, string>>({});
const provenances = ref<Record<string, string>>({});

opensilex.updateFiltersFromURL(route.query, filter.value);

const user = computed(() => store.state.user)

const credentials = computed(() => store.state.credentials)

const fields = computed(() => {
  let tableFields: any = [
    {
      key: "target",
      label: "component.data.dataTable.target-object",
    },
    {
      key: "date",
      label: "component.data.dataTable.list-data",
      sortable: true,
    },
    {
      key: "variable",
      label: "component.data.dataTable.list-variable",
      sortable: true,
    },
    {
      key: "value",
      label: "component.data.dataTable.list-value",
      sortable: false,
    },
    {
      key: "provenance",
      label: "component.data.dataTable.list-provenance",
      sortable: false
    },
    {
      key: "actions",
      label: "component.data.dataTable.list-action"
    }
  ];
  return tableFields;
})

function getVariableName(variableUri: string): string {
  return variableNames.value[opensilex.getLongUri(variableUri)];
}

function refresh() {
  opensilex.updateURLParameters(filter.value);
  tableRef.value.changeCurrentPage(1);
}

async function showDataDetailsModal(item: DataGetSearchDTO) {
  opensilex.enableLoader();
  try {
    const provenance = (await dataService.getProvenance(item.provenance.uri)).response.result;
    const batch = item.batchUri
        ? (await dataService.getBatchHistory(item.batchUri)).response.result
        : null;
    dataProvenanceModalView.value.setProvenanceAndBatch({ provenance, data: item, batch });
    dataProvenanceModalView.value.show();
  } catch (error) {
    console.error("Failed to fetch provenance or Batch:", error);
  } finally {
    opensilex.disableLoader();
  }
}

function countDataList() {
  let provUris = opensilex.prepareGetParameter(filter.value.provenance);
  if (provUris != undefined) {
    provUris = [provUris];
  }

  return dataService.countData(
      // Count data, set limit to  since here we want the exact/total data count according the current filter
      opensilex.prepareGetParameter(filter.value.start_date),
      opensilex.prepareGetParameter(filter.value.end_date),
      undefined,
      filter.value.experiments,
      opensilex.prepareGetParameter(filter.value.variables),
      opensilex.prepareGetParameter(filter.value.devices),
      undefined,
      undefined,
      provUris,
      undefined,
      opensilex.prepareGetParameter(filter.value.operators),
      filter.value.germplasm_group,
      filter.value.germplasm,
      0,
      filter.value.batch_uri,
      [].concat(
          filter.value.scientificObjects,
          filter.value.facilities,
          filter.value.targets) // targets & os & facilities
  )
}

function searchDataList(options) {
  let provUris = opensilex.prepareGetParameter(filter.value.provenance);
  if (provUris != undefined) {
    provUris = [provUris];
  }

  return new Promise((resolve, reject) => {
    dataService.searchDataListByTargets(
        opensilex.prepareGetParameter(filter.value.start_date),
        opensilex.prepareGetParameter(filter.value.end_date),
        undefined,
        filter.value.experiments,
        opensilex.prepareGetParameter(filter.value.variables),
        opensilex.prepareGetParameter(filter.value.devices),
        undefined,
        undefined,
        provUris,
        undefined,
        filter.value.germplasm_group,
        opensilex.prepareGetParameter(filter.value.operators),
        filter.value.germplasm,
        filter.value.batch_uri,
        options.orderBy,
        options.currentPage,
        options.pageSize,
        [].concat(filter.value.scientificObjects, filter.value.facilities, filter.value.targets) // targets & os & facilities
    )
        .then((http) => {
          let promiseArray = [];
          let objectsToLoad = [];
          let variablesToLoad = [];
          let provenancesToLoad = [];

          if (http.response.result.length > 0) {
            for (let i in http.response.result) {

              let objectURI = http.response.result[i].target;
              if (objectURI != null && !objectsToLoad.includes(objectURI)) {
                objectsToLoad.push(objectURI);
              }

              let variableURI = http.response.result[i].variable;
              if (!variablesToLoad.includes(variableURI)) {
                variablesToLoad.push(variableURI);
              }

              let provenanceURI = http.response.result[i].provenance.uri;
              if (!provenancesToLoad.includes(provenanceURI)) {
                provenancesToLoad.push(provenanceURI);
              }
            }

            if (objectsToLoad.length > 0) {
              promiseArray.push(opensilex.loadOntologyLabelsWithType(objectsToLoad, props.contextUri, objects.value, ontologyService));
            }

            if (variablesToLoad.length > 0) {
              let promiseVariable = variablesService
                  .searchVariablesByURIs(variablesToLoad)
                  .then((httpObj) => {
                    for (let j in httpObj.response.result) {
                      let variable = httpObj.response.result[j];
                      variableNames.value[opensilex.getLongUri(variable.uri)] = variable.name;
                    }
                  })
                  .catch(reject);
              promiseArray.push(promiseVariable);
            }

            if (provenancesToLoad.length > 0) {
              let promiseProvenance = dataService
                  .searchProvenancesByURIs(provenancesToLoad)
                  .then((httpObj) => {
                    for (let j in httpObj.response.result) {
                      let prov = httpObj.response.result[j];
                      provenances.value[prov.uri] = prov.name;
                    }
                  })
                  .catch(reject);
              promiseArray.push(promiseProvenance);
            }

            Promise.all(promiseArray).then(() => {
              loadObjectsPath().then(() => {
                resolve(http);
              })
            });

          } else {
            resolve(http);
          }
        })
        .catch(reject);
  });
}

/**
 * Construct paths for each target's UriLink components according to their type.
 */
function loadObjectsPath(): Promise<unknown> {
  // ensure that at least one object has been loaded (in case where all data in the page have no target)
  let objectURIs = Object.keys(objects.value);
  if (!objectURIs || objectURIs.length == 0) {
    return Promise.resolve();
  }

  return ontologyService
      .getURITypes(objectURIs)
      .then((httpObj) => {
        for (let j in httpObj.response.result) {
          let obj = httpObj.response.result[j];
          objectsPath.value[obj.uri] = opensilex.getPathFromUriTypes(obj.rdf_types);
        }
      });
}

defineExpose({
  refresh
})
</script>

<style scoped lang="scss">
.exportButton {
  margin-left: 15px;
}
</style>
