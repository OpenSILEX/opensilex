<template>
  <opensilex-TreeView
      :nodes="nodes"
      @select="displayClassDetail($event.data.uri)">
    <template #node="{ node }">
      <span class="item-icon">
        <opensilex-Icon v-if="classParametersByURI[node.data.uri] && classParametersByURI[node.data.uri].icon"
                        :icon="classParametersByURI[node.data.uri].icon"/>
      </span>&nbsp;
      <strong v-if="node.data.selected">{{ node.title }}</strong>
      <span v-if="!node.data.selected">{{ node.title }}</span>
    </template>

    <template #buttons="{ node }">
      <opensilex-AddChildButton
          v-if="user.isAdmin()"
          @click="emit('createChildClass' ,node.data.uri)"
          label="OntologyClassTreeView.add-child"
          :small="true"
      ></opensilex-AddChildButton>
      <opensilex-DeleteButton
          v-if="isManagedClass(node.data.uri) && user.isAdmin()"
          @click="emit('deleteRDFType' ,node.data)"
          label="OntologyClassTreeView.delete"
          :small="true"
      ></opensilex-DeleteButton>
    </template>
  </opensilex-TreeView>
</template>

<script setup lang="ts">

import {computed, inject, onBeforeUnmount, onMounted, reactive, ref, watch, watchEffect} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useStore} from "vuex";
import {OntologyService} from "opensilex-core/api/ontology.service";
import {useRoute} from "vue-router";
import {ResourceTreeDTO} from "opensilex-core/model/resourceTreeDTO";
import {VueJsOntologyExtensionService} from "@/lib";

const props = defineProps<{
  rdfType: string
}>();

const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const store = useStore();
const route = useRoute();
const user = computed(() => store.state.user);
const credentials = computed(() => store.state.credentials);

const nodes = ref([]);
const selected = ref();
const resourceTree = ref<Array<ResourceTreeDTO>>();
const classParametersByURI = ref({});

const ontologyService = opensilex.getService<OntologyService>("opensilex-core.OntologyService");
const vueJsOntologyService = opensilex.getService<VueJsOntologyExtensionService>("opensilex-front.VueJsOntologyExtensionService");

onMounted(() => {
  let preselected = route.query.selected;
  if (typeof preselected === "string") {
    displayClassDetail(preselected);
  }
  onRootClassChange();
});

const unwatchLang = store.watch(
    () => store.getters.language,
    () => {
      if (selected.value) {
        displayClassDetail(selected.value.uri);
      }
    }
);

onBeforeUnmount(() => {
  unwatchLang();
})

const emit = defineEmits<{
  selectionChange: [selected: any],
  createChildClass: [uri: string],
  deleteRDFType: [nodeData: any]
}>()

defineExpose({
  getTree,
  refresh
})

function displayClassDetail(uri: string) {
  vueJsOntologyService
      .getRDFTypeProperties(uri, props.rdfType)
      .then(http => {
        selected.value = http.response.result;
        //This updates or adds a url parameter, permitting refresh and navigation to specific elements
        opensilex.updateURLParameter("selected", selected.value.uri);
        emit('selectionChange', selected.value);
      }).catch(opensilex.errorHandler);
}

const onRootClassChange = watchEffect(() => {
  if (props.rdfType) {
    refresh(undefined, undefined);
  }
});

function refresh(selection, nameFilter) {
  Promise.all([
    ontologyService.searchSubClassesOf(props.rdfType, nameFilter, false),
    vueJsOntologyService.getRDFTypesParameters()
  ]).then(results => {
    let classesParameters = results[1].response.result;
    let classParamByURI = {};
    for (let i in classesParameters) {
      classParamByURI[classesParameters[i].uri] = classesParameters[i];
    }
    classParametersByURI.value = classParamByURI;

    if (results[0].response.result.length > 0) {
      resourceTree.value = results[0].response.result;

      // push the root class on the first tree level, and recursively build nodes for descendant
      nodes.value = [dtoToNode(resourceTree.value[0], selection)];

    } else {
      nodes.value = [];
    }

    if (selection) {
      displayClassDetail(selection.uri);
    }
  }).catch(opensilex.errorHandler);
}

function getTree() {
  return resourceTree.value;
}

function dtoToNode(dto: ResourceTreeDTO, selection) {
  let isLeaf = dto.children.length == 0;

  let childrenDTOs = [];
  if (!isLeaf) {
    for (let i in dto.children) {
      childrenDTOs.push(dtoToNode(dto.children[i], selection));
    }
  }

  if (selection && selection.uri == dto.uri) {
    selected.value = selection;
  }

  let isSelected = selected.value && selected.value.uri == dto.uri;

  return {
    key: dto.uri,
    title: dto.name,
    data: dto,
    isLeaf: isLeaf,
    children: childrenDTOs,
    isExpanded: true,
    isSelected: isSelected,
    isDraggable: false,
    isSelectable: !dto.disabled
  };
}

function isManagedClass(rdfClassURI) {
  return !!classParametersByURI.value[rdfClassURI];
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  OntologyClassTreeView:
    edit: Edit object type
    add-child: Add sub-object type
    delete: Delete object type

fr:
  OntologyClassTreeView:
    edit: Editer le type d'objet
    add-child: Ajouter un sous-type d'objet
    delete: Supprimer le type d'objet


</i18n>