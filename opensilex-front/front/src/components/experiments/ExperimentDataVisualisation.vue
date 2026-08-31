<template>
  <div>
    <div>
      <ExperimentDataVisualisationView
        :soFilter="soFilter" 
        @graphicCreated="onGraphicCreated"
        :selectedScientificObjects="selectedScientificObjects"
        :elementName="elementName"
      ></ExperimentDataVisualisationView> 
    </div>     
  </div>
</template>

<script setup lang="ts">
import Vue, {computed, inject, nextTick, ref} from "vue";
// @ts-ignore
import { ProvenanceGetDTO } from "core/index";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "core/HttpResponse";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useRouter} from "vue-router";
import {useStore} from "vuex";
import {useI18n} from "vue-i18n";
import {ScientificObjectsService} from "opensilex-core/api/scientificObjects.service";
import {VariablesService} from "opensilex-core/api/variables.service";

const opensilex = inject<OpenSilexVuePlugin>('$opensilex')
const route = useRouter()
const store = useStore()
const{ t } = useI18n()
const uri = ref('')
const soService = ref<any>()
const varService = ref<any>()
const visibleDetails = ref(false)
const searchVisible = ref(false)
const usedVariables  = ref<any[]>([])
const numberOfSelectedRows = ref(0);
const refreshKey = ref(0);
const selectedProvenance = ref<any>(null)

defineProps<{
  elementName?: string
}>()

  function refreshTypeSelectorComponent(){
    refreshKey.value += 1
  }

 const soFilter = ref({
    name: "",
    experiment: uri.value,
    germplasm: undefined,
    factorLevels: [],
    types: [],
    existenceDate: undefined,
    creationDate: undefined,
  });

const dataList = ref<any>()
const dataForm = ref<any>()
const searchField = ref<any>()
const provSelector = ref<any>()
const resultModal = ref<any>()
const soSelector = ref<any>()
const soForm = ref<any>()
const soTree = ref()
const selectedObjects = ref<string[]>([]);
const selected = ref<any>(null);
const showDataVisuView = ref(false);

const customColumns = computed(() => [
  {
    id: 'geometry',
    name: t('ExperimentScientificObjects.geometry-label'),
    type: 'WKT',
    comment: t('ExperimentScientificObjects.geometry-comment'),
    is_required: false,
    is_list: false
  }
])

    const credentials = computed(() => {
      return store.state.credentials;
    })

    const user = computed(() => {
      return store.state.user;
    } )

  const selectedScientificObjects = computed(() => {
    showDataVisuView.value = true;
    return selectedObjects.value.map(objectUri => {
      return {
        uri: objectUri,
        name: namedObjectsArray.value[objectUri]
      }
    });
  })

  const nodes = ref<any[]>([]);

  const filters = ref({
    name: "",
    types: [],
    parent: undefined,
    factorLevels: []
  });

  const selectedVariables = ref<any[]>([]);
  const namedObjectsArray = ref<any>({});

  const facilitySelector = ref<any>()
  const page = ref<any>()


uri.value = decodeURIComponent(route.params.uri as string);

soFilter.value = {
  name: "",
  experiment: uri.value,
  germplasm: undefined,
  factorLevels: [],
  types: [],
  existenceDate: undefined,
  creationDate: undefined,
};

soService.value = opensilex.getService(
    "opensilex.ScientificObjectsService"
);

varService.value = opensilex.getService(
    "opensilex.VariablesService"
);

  function onGraphicCreated() {
    let that = this;
    setTimeout(function() {
      that.page.scrollIntoView({
        behavior: "smooth",
        block: "end",
        inline: "nearest"
      });
    }, 500);
  }

  function resetSearch() {
    refresh();
  }

function clear() {
  searchVisible.value = false;
  selectedProvenance.value = null;
  refresh();
}

 function refreshProvComponent() {
    refreshKey.value += 1;
  }



  function refresh() {
    searchVisible.value = true;
    dataList.value.refresh();
    //remove experiments filter from URL
    nextTick(() => {
    opensilex.updateURLParameter("experiments", null, "");
    });
  }

  function loadAllChildren(nodeURI,page,pageSize) {
    return soService.value.getScientificObjectsChildren(
      nodeURI,
      uri.value,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      page,
      pageSize
    );
  }

  function refreshSoSelector() {
    soSelector.value.refreshModalSearch();
    refreshProvComponent();
  }

  function successMessage() {
    return t("ResultModalView.data-imported");
  }

  const getSelectedProv = computed(() => {
    return selectedProvenance})

function  initFormData(form) {
    form.experiment = uri.value
    return form;
  }

  function showProvenanceDetails() {
    if (selectedProvenance.value != null) {
      visibleDetails.value = !visibleDetails.value;
    }
  }


 function  unselectRefresh() {
    selected.value = null;
    selectedObjects.value = []; // fix bug filtre/selection
    showDataVisuView.value = false;
    refresh();
  }

  const lang = computed(() => {
    return store.state.lang
  })

function  refreshVariables() {
    opensilex
      .getService("opensilex.DataService")
      .getUsedVariables([uri.value], null, null, null)
      .then((http) => {
        let variables = http.response.result;
        usedVariables.value = [];
        for (let i in variables) {
          let variable = variables[i];
          usedVariables.value.push({
            id: variable.uri,
            label: variable.name,
          });
        }
      });
  }

function  getProvenance(uri) {
    if (uri != undefined && uri != null) {
      return opensilex
        .getService("opensilex.DataService")
        .getProvenance(uri)
        .then((http: HttpResponse<OpenSilexResponse<ProvenanceGetDTO>>) => {
          return http.response.result;
        });
    }
  }

  function loadProvenance(selectedValue) {
    if (selectedValue != undefined && selectedValue != null) {
      getProvenance(selectedValue.id).then((prov) => {
        selectedProvenance.value = prov;
      });
    }
  }

function searchMethod(nodeURI, page, pageSize) {
  const orderBy = ["name=asc"];

  if (
      filters.value.parent ||
      filters.value.types.length !== 0 ||
      filters.value.factorLevels.length !== 0 ||
      filters.value.name.length !== 0
  ) {
    return soService.value.searchScientificObjects(
        uri.value,
        filters.value.types,
        filters.value.name,
        filters.value.parent ? filters.value.parent : nodeURI,
        undefined, // Germplasm
        filters.value.factorLevels,
        undefined, // facility
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        orderBy,
        page,
        pageSize
    );
  } else {
    return soService.value.getScientificObjectsChildren(
        nodeURI,
        uri.value,
        undefined,
        undefined,
        undefined,
        undefined,
        orderBy,
        page,
        pageSize
    );
  }
}

  function searchParents(query, page, pageSize) {
    return soService.value
      .searchScientificObjects(
        uri.value, // experiment uri?: string,
        undefined, // rdfTypes?: Array<string>,
        query, // pattern?: string,
        undefined, // parentURI?: string,
        undefined, // Germplasm
        undefined, // factorLevels?: Array<string>,
        undefined, // facility?: string,
        undefined,
        undefined,
        undefined,
        undefined,
          undefined,
        [], // orderBy?: ,
        page, // page?: number,
        pageSize // pageSize?: number
      )
      .then(http => {
        let nodeList = [];
        for (let so of http.response.result) {
          nodeList.push({
            id: so.uri,
            label: so.name + " (" + so.rdf_type_name + ")"
          });
        }
        http.response.result = nodeList;
        return http;
      });
  }

  function initForm() {
    return {
      description: {
        uri: undefined,
        identifier: undefined,
        rdf_type: undefined,
        title: undefined,
        date: undefined,
        description: undefined,
        targets: selectedObjects.value,
        authors: undefined,
        language: undefined,
        deprecated: undefined,
        keywords: undefined
      },
      file: undefined
    }
  }
</script>

<style scoped lang="scss">
.selection-box {
  margin-top: 1px;
  margin-left: 24px;
}

.async-tree-action {
  font-style: italic;
}

.async-tree-action a:hover {
  text-decoration: underline;
  cursor: pointer;
}

.card-header {
  padding-top: 0 !important;
  padding-left: 0 !important;
  padding-right: 0 !important;
}

.card-header .badge {
  margin-left: 5px;
}

.scientificObjectsCards {
  display:contents
}
</style>
