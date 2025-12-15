
<template>
  <div class="entitiesOfInterestContainer">

    <div class="entitiesOfInterestList">
      <!-- Recherche d'entité d'intérêt -->
      <n-input
        v-model:value="search"
        :placeholder="t('EntityOfInterest.filter-placeholder')"
        clearable
        class="mb-4"
      />

      <!-- Liste des entités d'intérêt -->
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

    <!-- Détails de l'entité d'intéret selectionné -->
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

      <!-- Formulaire édition entité d'intếret -->
      <opensilex-AgroportalEntityOfInterestForm
        v-if="showForm"
        ref="entityFormRef"
        :createTitle="'component.variable.entityOfInterest.add-entityOfInterest'"
        :editTitle="'component.variable.entityOfInterest.edit'"
        :editData="editData"
        @onCreate="onFormSuccess"
        @onUpdate="onFormSuccess"
        @onClose="closeForm"
      ></opensilex-AgroportalEntityOfInterestForm>
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
console.log("elementType reçu dans EntityOfInterest:", props.elementType);

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
    ...selectedEntityDetails,
    name: selectedEntityDetails.name || selectedEntityDetails.label || '',
    comment: selectedEntityDetails.description || '',
    publisher: selectedEntityDetails.publisher || '',
    description: selectedEntityDetails.description || '',
    publication_date: selectedEntityDetails.publication_date || '',
    last_update_date: selectedEntityDetails.last_update_date || '',
     exact_match: selectedEntityDetails.exact_match ?? [],
    close_match: selectedEntityDetails.close_match ?? [],
    broad_match: selectedEntityDetails.broad_match ?? [],
    narrow_match: selectedEntityDetails.narrow_match ?? [],
    from_shared_resource_instance: selectedEntityDetails.from_shared_resource_instance ?? null
  };
}

async function fetchEntityDetails (uri: string) {
  try {
    const res = await service.getInterestEntity(uri);
    return res.response.result;
  } catch (e) {
    opensilex.errorHandler(e);
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

async function onFormSuccess(form: any) {
  // On garde en mémoire l'URI à reselectionner :
  // - priorité à form.uri si présent
  // - sinon, l'entité déjà sélectionnée avant la sauvegarde
  const previousUri = selected.value?.uri
  const targetUri = form?.uri || previousUri || null

  // On ferme le formulaire
  showForm.value = false
  editData.value = null

  // On recharge la liste
  await fetchEntities()
  await nextTick()

  if (!targetUri) {
    // pas d'uri à reselectionner (pas censé arriver)
    return
  }

  // Si l'entité est dans la liste, on l'utilise
  const selectedEntity = entities.value.find(e => e.uri === targetUri)

  if (selectedEntity) {
    await updateSelected(selectedEntity)
  } else {
    // Fallback on construit un mini-objet avec l'URI juste pour updateSelected
    await updateSelected({ uri: targetUri })
  }
}

// Requête de recherche
const searchElements = async (filter = ''): Promise<NamedResourceDTO[]> => {
  const orderBy = ['name=asc'];
  let response: HttpResponse<OpenSilexResponse<NamedResourceDTO[]>>;

    response = await service.searchInterestEntity(filter, orderBy);

console.log("response : ", response)
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

// Try to select whenever the query changes
watch(
  () => route.query.selected,
  async () => { await selectFromQuery() },
  { immediate: false }
)

// If the element type changes (switching tabs), clear old selection,
// but also try to get a new ?selected=... for the new type.
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

  // router gives decoded string in Vue 3; if not, decode once
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
    exact_match: details.exact_match ?? [],
    close_match: details.close_match ?? [],
    broad_match: details.broad_match ?? [],
    narrow_match: details.narrow_match ?? [],
    // provenance partagée
    from_shared_resource_instance: details.from_shared_resource_instance ?? null
  }
}

defineExpose({ showCreateForm, onFormSuccess });
</script>

<style>

.entitiesOfInterestContainer {
    display: flex;
}

.entitiesOfInterestList, .entityDetails {
    width: 45%;
    padding: 10px;
}
</style>
<i18n>
en:
    EntityOfInterest:
        filter-placeholder: Search entities by name

fr:
    EntityOfInterest:
        filter-placeholder: Rechercher des entités par nom
</i18n>

