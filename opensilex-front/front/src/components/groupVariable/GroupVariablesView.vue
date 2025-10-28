
<template>
  <div class="variablesGroupContainer">

    <div class="variablesGroupList">
      <!-- Recherche de groupe -->
      <n-input
        v-model:value="search"
        :placeholder="t('GroupVariablesView.filter-placeholder')"
        clearable
        class="mb-4"
      />

      <!-- Liste des groupes -->
      <n-list bordered hoverable class="rounded-lg shadow">
        <n-list-item
          v-for="group in groups"
          :key="group.uri"
          :class="{ 'bg-gray-100': selected?.uri === group.uri }"
          @click="updateSelected(group)"
        >
          <template #default>
            <div>
              <div class="font-medium text-base">{{ group.title }}</div>
              <div class="text-sm text-gray-500">
                {{ group.variables?.length ?? 0 }} variables
              </div>
            </div>
          </template>

          <template #suffix>
            <n-space :wrap="false" :size="[0, 0]">
              <opensilex-EditButton
                label="Edit"
                :small="true"
                @click="(e) => { e?.stopPropagation?.(); showEditForm(group); }"
              />
              <opensilex-DeleteButton
                label="Delete"
                :small="true"
                @click="(e) => { e?.stopPropagation?.(); onDeleteGroup(group); }"
              />
            </n-space>
          </template>
        </n-list-item>
      </n-list>
    </div>

    <!-- Détails du groupe selectionné -->
    <div v-if="selected" class="variablesGroupDetails">
        <opensilex-VariableStructureDetails 
            :selected="selected"
        ></opensilex-VariableStructureDetails>

      <!-- Carte Variables du groupe selectionné -->
      <opensilex-Card v-if="selected.variables?.length" :noFooter="true">
        <template #title>
            <opensilex-Icon icon="fa#vials"></opensilex-Icon>
            {{ t("component.menu.variables") }}
        </template>
        <template #body>
          <!-- Recherche de variable dans le groupe -->
          <n-input
            v-model:value="variableSearch"
            :placeholder="t('GroupVariablesView.variable-filter-placeholder')"
            clearable
            class="mb-4"
          />
          
          <!-- Liste des variables du groupe -->
          <opensilex-TableView
          :items="filteredVariables"
          :fields="fields"
          :customRenderers="renderers"
          ></opensilex-TableView>
        </template>
      </opensilex-Card>
      
      <opensilex-DocumentTabList
        v-if="selected && selected.uri"
        :selected="selected"
        :uri="[selected.uri]"
        :search="false"
      ></opensilex-DocumentTabList>
    </div>


    <!-- Formulaire création/édition groupe -->
    <opensilex-ModalForm
      v-if="showForm"
      ref="groupFormRef"
      component="opensilex-GroupVariablesForm"
      :createTitle="'component.variable.groupVariable.add-groupVariable'"
      :editTitle="'component.variable.groupVariable.edit'"
      :editData="editData"
      @onSuccess="onFormSuccess"
      @onClose="closeForm"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onBeforeMount, resolveComponent, onMounted, inject, defineExpose, nextTick, watch, h} from 'vue';
import { useI18n } from 'vue-i18n';
import { VariablesService, NamedResourceDTO } from 'opensilex-core/index';
import HttpResponse, { OpenSilexResponse } from '../../../lib/HttpResponse';
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import { NInput, NList, NListItem, NSpace } from 'naive-ui';

const { t } = useI18n();
const opensilex = inject<OpenSilexVuePlugin>("$opensilex");

const props = defineProps<{ elementType: string }>();
console.log("elementType reçu dans GroupVariablesView:", props.elementType);

const selected = ref<any | null>(null);
const editData = ref(null);
const showForm = ref(false);
const groupFormRef = ref(null);

const groups = ref<any[]>([]);
const search = ref('');
const variableSearch = ref('');
let service: VariablesService;
const UriLink = resolveComponent('opensilex-UriLink');

const fields = computed(() => [
    { 
        key: 'name', 
        label: t('component.common.name'), 
        sortable: true,
    }
 ])

onBeforeMount(() => {
  service = opensilex.getService('opensilex-core.VariablesService');
});

async function fetchGroups() {
  try {
    const rawGroups = await searchElements('');
    groups.value = rawGroups.map(g => ({
      ...g,
      title: g.name,
      variables: g.variables || []
    }));
  } catch (e) {
    opensilex.errorHandler(e);
  }
}

onMounted(fetchGroups);


// Affiche les détails
async function updateSelected(group: any) {
    // réponse du searchElements, récupere tout les groupes mais pas de publisher.
    // -> swagger : on vois que c'est pas un pb de front, le service searchVariablesGroups renvoi bien des publishers nulls

    // donc ici faire un appel avec l'autre service (getVariablesGroup) sur la base group.uri ?
    console.log("le grp : ", group)

      const selectedGroupDetails = await fetchGroupDetails(group.uri);
  if (!selectedGroupDetails) return;


  selected.value = {
    uri: selectedGroupDetails.uri,
    name: selectedGroupDetails.name || selectedGroupDetails.label || '',
    comment: selectedGroupDetails.description || '',
    publisher: selectedGroupDetails.publisher || '',
    description: selectedGroupDetails.description || '',
    publication_date: selectedGroupDetails.publication_date || '',
    last_update_date: selectedGroupDetails.last_update_date || '',
    type: selectedGroupDetails.type || '',
    typeLabel: selectedGroupDetails.typeLabel || '',
    variables: selectedGroupDetails.variables ?? []
    // est ce que les groupes ont ça ? 
    // exact_match: group.exact_match ?? [],
    // close_match: group.close_match ?? [],
    // broad_match: group.broad_match ?? [],
    // narrow_match: group.narrow_match ?? [],
  };
}

async function fetchGroupDetails(uri: string) {
  try {
    const response = await service.getVariablesGroup(uri);
    return response.response.result;
  } catch (error) {
    opensilex.errorHandler(error);
    return null;
  }
}


// Supprime un groupe
function onDeleteGroup(group: any) {
  groups.value = groups.value.filter(g => g.uri !== group.uri);
  if (selected.value?.uri === group.uri) {
    selected.value = null;
  }
  opensilex.showSuccessToast(
    t("component.group.group-deleted")
  );
}

// Formulaire crea
function showCreateForm() {
  editData.value = null;
  showForm.value = true;
}

// Formulaire édit



function showEditForm(group: any) {
  editData.value = group;
  showForm.value = true;
  nextTick(() => {
    groupFormRef.value?.showEditForm?.(group);
  });
}

// async function showEditForm(group: any) {
//   // editData.value = group;
//   // showForm.value = true;
//   // nextTick(() => {
//   //   groupFormRef.value?.showEditForm?.(group);
//   // });
//   const details = await fetchGroupDetails(group.uri);
//   if (!details) return;

//   const vars = details.variables ?? [];
//   // Données pour le formulaire (URIs)
//   const formData = {
//     uri: details.uri,
//     name: details.name || details.label || '',
//     description: details.description || '',
//     variables: vars.map((v: any) => v.uri),
//     // on ajoute une charge utile "labels" pour le sélecteur
//     __variablesWithLabels: vars.map((v: any) => ({
//       id: v.uri,
//       label: v.name || v.uri
//     }))
//   };

//   editData.value = formData;
//   showForm.value = true;
//   nextTick(() => {
//     groupFormRef.value?.showEditForm?.(formData);
//   });
// }

function onFormSuccess() {
  showForm.value = false;
  selected.value = null;
  fetchGroups();
}


function closeForm() {
  showForm.value = false;
  editData.value = null;
}

// Requête de recherche
const searchElements = async (filter = ''): Promise<NamedResourceDTO[]> => {
  const orderBy = ['name=asc'];
  let response: HttpResponse<OpenSilexResponse<NamedResourceDTO[]>>;
  
    response = await service.searchVariablesGroups(filter, undefined, orderBy);

console.log("responsey ", response)
  return response.response.result;
};

const relationsFields = [
  { key: 'name', label: t("VariableView.name") },
  { key: 'uri', label: 'URI' }
];

watch(search, async (val) => {
  const result = await searchElements(val);
  groups.value = result.map(group => ({
    ...group,
    title: group.name,
    variables: group.variables || []
  }));
});

const renderers = {
  name: (row: any) => {
    return h('div', [
      h(UriLink, {
        uri: row.uri,
        value: row.name,
        to: {
          path: '/variable/details/' + encodeURIComponent(row.uri)
        }
      })
    ]);
  }
};

// filtre des var issues d'un groupe selectionné
const filteredVariables = computed(() => {
  const query = variableSearch.value?.toLowerCase().trim();
  if (!query) return selected.value?.variables || [];
  return (selected.value?.variables || []).filter(variable =>
    variable.name?.toLowerCase().includes(query)
  );
});

defineExpose({ showCreateForm });
</script>

<style>

.variablesGroupContainer {
    display: flex;
}


.variablesGroupList, .variablesGroupDetails {
    width: 45%;
    padding: 10px;
}
</style>
<i18n>
en:
    GroupVariablesView:
        filter-placeholder: Search groups by name
        variable-filter-placeholder: Search variables by name

fr:
    GroupVariablesView:
        filter-placeholder: Rechercher des groupes par nom
        variable-filter-placeholder: Rechercher des variables par nom
</i18n>

