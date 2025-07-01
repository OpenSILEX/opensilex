
<template>
  <div class="variablesGroupContainer">

    <!-- Liste des groupes -->
    <div class="variablesGroupList">
      <n-input
        v-model:value="search"
        :placeholder="t('GroupVariablesView.filter-placeholder')"
        clearable
        class="mb-4"
      />

      <n-list bordered hoverable class="rounded-lg shadow">
        <n-list-item
          v-for="group in filteredGroups"
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

    <!-- Détails et variables -->
    <div v-if="selected" class="variablesGroupDetails">
      <!-- Carte détails -->
      <opensilex-Card>
        <template #title>
          {{ t("component.common.details-label") }}
        </template>
        <template #body>
          <opensilex-ExternalReferencesDetails :skosReferences="selected" />
        </template>
      </opensilex-Card>

      <!-- Carte variables -->
<opensilex-Card v-if="selected.variables?.length">
  <template #title>
    {{ t("component.menu.variables") }}
  </template>
  <template #body>
    <opensilex-TableView
      :items="selected.variables"
      :fields="fields"
    >
      <template #cell(name)="{ data }">
    {{selected.variables}}
        
        <opensilex-UriLink
          :uri="data.item.uri"
          :value="data.item.name"
          :to="{ path: '/variable/details/' + encodeURIComponent(data.item.uri) }"
        />
      </template>
    </opensilex-TableView>
  </template>
</opensilex-Card>

    </div>

    <!-- Formulaire création/édition -->
    <opensilex-ModalForm
      v-if="showForm"
      ref="groupFormRef"
      component="GroupVariablesForm"
      createTitle="GroupVariablesForm.add"
      editTitle="GroupVariablesForm.edit"
      :editData="editData"
      @onSuccess="onFormSuccess"
      @onClose="closeForm"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onBeforeMount, onMounted, inject, defineExpose, nextTick, watch } from 'vue';
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
let service: VariablesService;

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
    console.error('Erreur lors de la récupération des groupes :', e);
  }
}

onMounted(fetchGroups);

const filteredGroups = computed(() =>
  groups.value.filter(group =>
    group.title?.toLowerCase().includes(search.value.toLowerCase())
  )
);

// Affiche les détails
function updateSelected(group: any) {
  selected.value = {
    uri: group.uri,
    name: group.name || group.label || '',
    comment: group.description || '',
    type: group.type || '',
    typeLabel: group.typeLabel || '',
    exact_match: group.exact_match ?? [],
    close_match: group.close_match ?? [],
    broad_match: group.broad_match ?? [],
    narrow_match: group.narrow_match ?? [],
    variables: group.variables ?? []
  };
}

// Supprime un groupe
function onDeleteGroup(group: any) {
  groups.value = groups.value.filter(g => g.uri !== group.uri);
  if (selected.value?.uri === group.uri) {
    selected.value = null;
  }
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

  switch (props.elementType) {
    case 'ENTITY_TYPE':
      response = await service.searchEntities(filter, orderBy);
      break;
    case 'INTEREST_ENTITY_TYPE':
      response = await service.searchInterestEntity(filter, orderBy);
      break;
    case 'CHARACTERISTIC_TYPE':
      response = await service.searchCharacteristics(filter, orderBy);
      break;
    case 'METHOD_TYPE':
      response = await service.searchMethods(filter, orderBy);
      break;
    case 'UNIT_TYPE':
      response = await service.searchUnits(filter, orderBy);
      break;
    case 'GROUP_VARIABLE_TYPE':
      response = await service.searchVariablesGroups(filter, undefined, orderBy);
      break;
    default:
      response = await service.searchEntities(filter, orderBy);
      break;
  }

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

fr:
    GroupVariablesView:
        filter-placeholder: Rechercher des groupes par nom
</i18n>

