<template>
  <TreeView
      :nodes="nodes"
      defaultExpandAll
      @select="displayPropertyNodeDetail($event[0])">
    <template #buttons="{ node }">
      <n-button-group size="small" class="btn-group btn-group-sm">
        <AddChildButton
            v-if="user.isAdmin()"
            @click="emit('createChildProperty' ,node.data.uri)"
            :label="t('OntologyPropertyTreeView.add-child')"
            :small="true"
        ></AddChildButton>
      </n-button-group>
    </template>
  </TreeView>
</template>

<script setup lang="ts">
import {computed, h, inject, onBeforeUnmount, onMounted, ref, watch} from "vue";
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin";
import {useStore} from "vuex";
import {useRoute} from "vue-router";
import {OntologyService} from "opensilex-core/api/ontology.service";
import {ResourceTreeDTO} from "opensilex-core/model/resourceTreeDTO";
import OWL from "@/ontologies/OWL";
import {useI18n} from "vue-i18n";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import {RDFPropertyGetDTO} from "opensilex-core/model/rDFPropertyGetDTO";
import TreeView from "@/components/common/views/TreeView.vue";
import AddChildButton from "@/components/common/buttons/AddChildButton.vue";

//#region Public
const props = defineProps<{
  domain: string
}>()

const emit = defineEmits<{
  selectionChange: [selected: RDFPropertyGetDTO]
  createChildProperty: [uri: string]
}>();

defineExpose({
  getTree,
  refresh
})
//#endregion

//#region Private


const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const ontologyService = opensilex.getService<OntologyService>("opensilex-core.OntologyService");
const store = useStore();
const route = useRoute();
const user = computed(() => store.state.user);
const {t} = useI18n();

const nodes = ref([]);
const selected = ref<RDFPropertyGetDTO | undefined>();

watch(() => props.domain, onDomainChange);

onMounted(() => {
  let preselected = route.query.selected;
  if (typeof preselected === "string") {
    displayPropertyDetail(preselected, undefined);
  }
  onDomainChange();
})

const unwatchLang = store.watch(
    () => store.getters.language,
    () => {
      onDomainChange();
      if (selected.value) {
        displayPropertyDetail(selected.value.uri, selected.value.rdf_type);
      }
    }
);

onBeforeUnmount(() => {
  unwatchLang();
})


function getTree() {
  return nodes;
}

function onDomainChange() {
  if (props.domain) {
    refresh(undefined);
  }
}

function refresh(nameFilter) {
  // get properties on domain, including those from sub-classes
  ontologyService.getProperties(props.domain, nameFilter, true)
      .then(http => {
        if (http && http.response.result) {
          let treeNode = [];
          http.response.result.forEach((resourceTree: ResourceTreeDTO) => {
            let node = dtoToNode(resourceTree);
            treeNode.push(node);
          });
          nodes.value = treeNode;
        } else {
          nodes.value = [];
        }
        emit("selectionChange", selected.value);
      }).catch(opensilex.errorHandler);
}


function displayPropertyNodeDetail(node) {
  if (!selected.value || node.data.uri != selected.value.uri) {
    displayPropertyDetail(node.data.uri, node.data.rdf_type);
  }
}


function displayPropertyDetail(uri, type) {
  ontologyService.getProperty(uri, type, props.domain).then(http => {
    selected.value = http.response.result;
    //This updates or adds a url parameter, permitting refresh and navigation to specific elements
    opensilex.updateURLParameter("selected", selected.value.uri);
    emit("selectionChange", selected.value);
  });
}

function getPropertyIcon(rdfType: string) {
  if (OWL.isDatatypeProperty(rdfType)) {
    return "database";
  } else {
    return "link";
  }
}

function dtoToNode(dto: ResourceTreeDTO) {
  let isLeaf = dto.children.length == 0;

  let childrenDTOs = [];
  if (!isLeaf) {
    for (let i in dto.children) {
      childrenDTOs.push(dtoToNode(dto.children[i]));
    }
  }

  return {
    key: dto.uri,
    title: dto.name,
    data: dto,
    isLeaf: isLeaf,
    children: childrenDTOs,
    isExpanded: true,
    isSelected: selected.value && selected.value.uri == dto.uri,
    isDraggable: false,
    isSelectable: !dto.disabled,
    prefix: () => h(FontAwesomeIcon, {icon: getPropertyIcon(dto.rdf_type)})
  };
}

//#endregion
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  OntologyPropertyTreeView:
    edit: Edit property
    add-child: Add sub-property
    delete: Delete property

fr:
  OntologyPropertyTreeView:
    edit: Editer la propriété
    add-child: Ajouter une sous-propriété
    delete: Supprimer la propriété

</i18n>
