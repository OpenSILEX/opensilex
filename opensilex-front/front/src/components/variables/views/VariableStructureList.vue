<template>
  <div class="card">
    <div class="card-body">
      <opensilex-StringFilter
        v-model="nameFilter"
        @update="updateFilter"
        :placeholder="t('EntityList.filter-placeholder')"
      />

      <opensilex-TreeView
        :nodes="nodes"
        :noButtons="false"
        @select="displayNodesDetail"
        @toggle="() => {}"
        ref="tree"
      >
      
        <template #node="{ node }">
        <span class="item-icon">
            <!-- <opensilex-Icon :icon="$opensilex.getRDFIcon(node.data.rdf_type)" /> -->
        </span>&nbsp;
        <!-- test : {{ node.title}} -->

        <strong v-if="node.selected">
            {{ node.variables ? node.title + ' ' + $tc('VariableStructureList.variable', node.variables.length, { count: node.variables.length }) : node.title }}
        </strong>

        <span v-else>
            {{ node.variables ? node.title + ' ' + $tc('VariableStructureList.variable', node.variables.length, { count: node.variables.length }) : node.title }}
        </span>
        </template>

        <template #buttons="{ node }">
          <opensilex-EditButton
            v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
            @click="() => console.log('node uri     ',node.uri) || edit(node.uri)"

            label="Edit"
            :small="true"
          />
            <!-- @click="edit(node.uri)" -->
          <opensilex-DeleteButton
            v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_DELETE_ID) && props.elementType === 'GROUP_VARIABLE_TYPE'"
            @click="deleteVariablesGroup(node.uri)"
            label="Delete"
            :small="true"
          />
        </template>
      </opensilex-TreeView>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeMount, watch, inject, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useStore } from 'vuex';
import VariablesView from '../VariablesView.vue';
import { VariablesService, NamedResourceDTO } from 'opensilex-core/index';
import HttpResponse, { OpenSilexResponse } from '../../../lib/HttpResponse';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import { useI18n } from 'vue-i18n';

const props = defineProps<{
  elementType: string;
}>();

console.log("elementType reçu dans VariableStructureList :", props.elementType);
console.log('props.elementType:', props.elementType);


const emit = defineEmits(['onSelect', 'onEdit', 'onDelete']);

const $store = useStore();
const $route = useRoute();
const $router = useRouter();
const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const { t } = useI18n();


const nameFilter = ref('');
const nodes = ref([]);
const selected = ref<any>(null);
const tree = ref(null);
let service: VariablesService;

const user = computed(() => $store.state.user);
const credentials = computed(() => $store.state.credentials);

const updateFilter = () => {
  opensilex.updateURLParameter('name', nameFilter.value, '');
  refresh(false);
};

onBeforeMount(() => {
  service = opensilex.getService('opensilex-core.VariablesService');

  const query = $route.query;
  if (query && query.selected) {
    refresh(false, decodeURIComponent(query.selected as string));
  } else {
    refresh(false);
  }
});

onMounted(() => {
  watch(
    () => $store.getters.language,
    () => {
      if (selected.value) {
        displayNodeDetail(selected.value.uri, true);
      }
    }
  );
});

const searchElements = (filter: string): Promise<HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>> => {
  const orderBy = ['name=asc'];
      console.log("result" , props.elementType)
  switch (props.elementType) {
    case 'ENTITY_TYPE':
      return service.searchEntities(filter, orderBy);
    case 'INTEREST_ENTITY_TYPE':
      return service.searchInterestEntity(filter, orderBy);
    case 'CHARACTERISTIC_TYPE':
      return service.searchCharacteristics(filter, orderBy);
    case 'METHOD_TYPE':
      return service.searchMethods(filter, orderBy);
    case 'UNIT_TYPE':
      return service.searchUnits(filter, orderBy);
    case 'GROUP_VARIABLE_TYPE':
         console.log('⚠️ searchElements for groups');
      return service.searchVariablesGroups(filter, undefined, orderBy);
    default:
         console.log('⚠️ searchElements By Default...');
      return service.searchEntities(filter, orderBy);
  }
};

const refresh = (updateType: boolean, uri?: string) => {
  if (props.elementType === 'VARIABLE_TYPE') return;

  if (updateType) nameFilter.value = '';

  searchElements(nameFilter.value)
    .then(http => {
      const treeNode = [];
      let first = true;
      let uriFound = false;

      for (const dto of http.response.result) {
        const node = dtoToNode(dto, first, uri);
        console.log("node ", node)
        treeNode.push(node);

        if (uri && !uriFound && opensilex.checkURIs(dto.uri, uri)) {
          uriFound = true;
        }
        if (first) first = false;
      }

      if (uri && !uriFound) {
        displayNodeDetail(uri, true, true);
      }

      if (http.response.result.length === 0) {
        selected.value = null;
      }

      nodes.value = treeNode;

      if (nodes.value.length > 0) {
        selected.value = nodes.value[0].data;
      }
    })
    .catch(opensilex.errorHandler);
};

const dtoToNode = (dto: NamedResourceDTO, first: boolean, uri?: string) => {
  const isSelected = uri ? dto.uri === uri : first;
  return {
    key: dto.uri,
    title: dto.name,
    uri: dto.uri,
    rdf_type: dto.rdf_type,
    variables: dto.variables,
    data: dto,
    isLeaf: true,
    selected: isSelected
  };
};


const displayNodesDetail = (node: any) => {
  displayNodeDetail(node.data.uri);
};

const getDetails = (uri: string): Promise<HttpResponse<OpenSilexResponse>> => {
  switch (props.elementType) {
    case 'ENTITY_TYPE':
      return service.getEntity(uri);
    case 'INTEREST_ENTITY_TYPE':
      return service.getInterestEntity(uri);
    case 'CHARACTERISTIC_TYPE':
      return service.getCharacteristic(uri);
    case 'METHOD_TYPE':
      return service.getMethod(uri);
    case 'UNIT_TYPE':
      return service.getUnit(uri);
    case 'GROUP_VARIABLE_TYPE':
      return service.getVariablesGroup(uri);
    default:
      return service.getEntity(uri);
  }
};

const displayNodeDetail = (uri: string, forceRefresh?: boolean, appendNodeToList?: boolean) => {
  if ((forceRefresh || !selected.value || selected.value.uri !== uri) && props.elementType !== 'VARIABLE_TYPE') {
    getDetails(uri)
      .then(http => {
        if (appendNodeToList) {
          const node = dtoToNode(http.response.result, true, uri);
          nodes.value.unshift(node);
        }
        selected.value = http.response.result;
        opensilex.updateURLParameter('selected', selected.value.uri);
        emit('onSelect', selected.value);
      })
      .catch(opensilex.errorHandler);
  }
};

const edit = (uri: string) => {
  if (props.elementType !== 'VARIABLE_TYPE') {
    getDetails(uri).then(http => {
        console.log("emit edit avec : ", http.response.result)
      emit('onEdit', http.response.result);
    });
  }
};

const deleteVariablesGroup = (uri: string) => {
  if (props.elementType === 'GROUP_VARIABLE_TYPE') {
    service.deleteVariablesGroup(uri).then(http => {
      const message = `${t(http.response.result)} ${t('component.common.success.delete-success-message')}`;
      opensilex.showSuccessToast(message);
      emit('onDelete', http.response.result);

      if (nodes.value.length > 0) {
        selected.value = nodes.value[0].data;
        emit('onDelete', selected.value);
      } else {
        selected.value = undefined;
      }

      refresh(true);
    });
  }
};

const firstObjectOfList = () => {
  return nodes.value.length > 0 ? nodes.value[0].data : undefined;
};
</script>

<style scoped lang="scss">
.sl-vue-tree-root {
  min-height: 100px;
  max-height: 300px;
  overflow-y: auto;
}

.leaf-spacer {
  display: inline-block;
  width: 23px;
}

@media (max-width: 768px) {
  .sl-vue-tree-root {
    min-height: auto;
  }
}
</style>

<i18n>
en:
    VariableStructureList:
        variable: "(0 variables) | (1 variable) | ({count} variables)"
    EntityList:
        filter-placeholder: Search objects by name

fr:
    VariableStructureList:
        variable: "(0 variables) | (1 variable) | ({count} variables)"
    EntityList:
        filter-placeholder: Rechercher des élements par nom
</i18n>

