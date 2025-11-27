
<template>
  <div class="entitiesContainer">

    <div class="entitiesList">
      <!-- Recherche d'entité -->
      <n-input
        v-model:value="search"
        :placeholder="t('EntitiesView.filter-placeholder')"
        clearable
        class="mb-4"
      />

      <!-- Liste des entités -->
      <n-list bordered hoverable class="rounded-lg shadow">
        <n-list-item
          v-for="entity in entities"
          :key="entity.uri"
          :class="{ 'bg-gray-100': selected?.uri === entity.uri }"
          @click="updateSelected(entity)"
        >
          <template #default>
            <div>
              <div class="font-medium text-base">{{ entity.title }}</div>
            </div>
          </template>

          <template #suffix>
            <n-space :wrap="false" :size="[0, 0]">
              <opensilex-EditButton
                label="Edit"
                :small="true"
                @click="(e) => { e?.stopPropagation?.(); showEditForm(entity); }"
              />
            </n-space>
          </template>
        </n-list-item>
      </n-list>
    </div>

    <!-- Détails de l'entité selectionné -->
    <div v-if="selected" class="entityDetails">
        <opensilex-VariableStructureDetails 
            :selected="selected"
        ></opensilex-VariableStructureDetails>

       <!-- Références SKOS -->
      <div>
        <opensilex-Card label="component.skos.ontologies-references-label" icon="fa#globe-americas" :no-footer="true">
          <template #body>
            <opensilex-ExternalReferencesDetails :skosReferences="selected" />
          </template>
        </opensilex-Card>
      </div>


    <!-- Formulaire édition entité -->
    <opensilex-AgroportalEntityForm
      v-if="showForm"
      ref="entityFormRef"
      :createTitle="'component.variable.entity.add-entity'"
      :editTitle="'component.variable.entity.edit'"
      :editData="editData"
      @onSuccess="onFormSuccess"
      @onClose="closeForm"
    ></opensilex-AgroportalEntityForm>
    
  </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onBeforeMount, resolveComponent, onMounted, inject, defineExpose, nextTick, watch, h} from 'vue';
import { useI18n } from 'vue-i18n';
import { VariablesService, NamedResourceDTO } from 'opensilex-core/index';
import HttpResponse, { OpenSilexResponse } from '../../../lib/HttpResponse';
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import { useRoute } from 'vue-router';
import { NInput, NList, NListItem, NSpace } from 'naive-ui';

const { t } = useI18n();
const opensilex = inject<OpenSilexVuePlugin>("$opensilex");
const route = useRoute()

const props = defineProps<{ elementType: string }>();
console.log("elementType reçu dans EntitiesView:", props.elementType);

const selected = ref<any | null>(null);
const editData = ref(null);
const showForm = ref(false);
const entityFormRef = ref(null);

const entities = ref<any[]>([]);
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

async function fetchEntities() {
  try {
    const rawEntities = await searchElements('');
    entities.value = rawEntities.map(entity => ({
      ...entity,
      title: entity.name,
      variables: entity.variables || []
    }));
  } catch (e) {
    opensilex.errorHandler(e);
  }
}

onMounted(fetchEntities);


// Affiche les détails
async function updateSelected(entity: any) {
  const selectedEntityDetails = await fetchEntityDetails(entity.uri);
  if (!selectedEntityDetails) return;

  selected.value = {
    // on garde ce qui vient du backend dontexact_match, close_match, etc.
    ...selectedEntityDetails,

    name: selectedEntityDetails.name || selectedEntityDetails.label || '',
    comment: selectedEntityDetails.description || '',
    publisher: selectedEntityDetails.publisher || '',
    description: selectedEntityDetails.description || '',
    publication_date: selectedEntityDetails.publication_date || '',
    last_update_date: selectedEntityDetails.last_update_date || '',
    type: selectedEntityDetails.type || '',
    typeLabel: selectedEntityDetails.typeLabel || '',
    variables: selectedEntityDetails.variables ?? []
  };
}

async function fetchEntityDetails(uri: string) {
  try {
    const response = await service.getEntity(uri);
    return response.response.result;
  } catch (error) {
    opensilex.errorHandler(error);
    return null;
  }
}

// Formulaire crea
function showCreateForm() {
  editData.value = null;
  showForm.value = true;
}

// Formulaire édit
async function showEditForm(entity: any) {
  const selectedEntityDetails = await fetchEntityDetails(entity.uri);
  editData.value = selectedEntityDetails;
  showForm.value = true;
  nextTick(() => {
    entityFormRef.value?.showEditForm?.(selectedEntityDetails);
  });
}

function onFormSuccess() {
  showForm.value = false;
  selected.value = null;
  fetchEntities();
}


function closeForm() {
  showForm.value = false;
  editData.value = null;
}

// Requête de recherche
const searchElements = async (filter = ''): Promise<NamedResourceDTO[]> => {
  const orderBy = ['name=asc'];
  let response: HttpResponse<OpenSilexResponse<NamedResourceDTO[]>>;

    response = await service.searchEntities(filter, orderBy);

console.log("responsey ", response)
  return response.response.result;
};

const relationsFields = [
  { key: 'name', label: t("VariableView.name") },
  { key: 'uri', label: 'URI' }
];

watch(search, async (val) => {
  const result = await searchElements(val);
  entities.value = result.map(entity => ({
    ...entity,
    title: entity.name,
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

onMounted(async () => {
  await fetchEntities()
  await selectFromQuery()
})

watch(
  () => route.query.selected,
  async () => { await selectFromQuery() },
  { immediate: false }
)

watch(
  () => props.elementType,
  async () => {
    selected.value = null
    await fetchEntities()
    await selectFromQuery()
  }
)

// --- helpers ---
async function selectFromQuery () {
  const selectedElement = route.query.selected
  if (!selectedElement) return

  const targetUri = typeof selectedElement === 'string' ? selectedElement : selectedElement[0]

  // If item is in the list, just select it
  const inList = entities.value.find(e => e.uri === targetUri)
  if (inList) {
    await updateSelected(inList)
    return
  }

  // Otherwise fetch its details directly and build the "selected" object
  const details = await fetchEntityDetails(targetUri)
  if (!details) return

selected.value = {
  ...details,
  name: details.name || details.label || '',
  comment: details.description || '',
  publisher: details.publisher || '',
  description: details.description || '',
  publication_date: details.publication_date || '',
  last_update_date: details.last_update_date || '',
  type: details.type || '',
  typeLabel: details.typeLabel || '',
  variables: details.variables ?? []
};
}

defineExpose({ showCreateForm });
</script>

<style>

.entitiesContainer {
    display: flex;
}

.entitiesList, .entityDetails {
    width: 45%;
    padding: 10px;
}
</style>
<i18n>
en:
    EntitiesView:
        filter-placeholder: Search entities by name

fr:
    EntitiesView:
        filter-placeholder: Rechercher des entités par nom
</i18n>

