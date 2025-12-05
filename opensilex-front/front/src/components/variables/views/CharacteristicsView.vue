
<template>
  <div class="characteristicsContainer">

    <div class="characteristicsList">
      <!-- Recherche d'une caracteristique' -->
      <n-input
        v-model:value="search"
        :placeholder="t('Characteristic.filter-placeholder')"
        clearable
        class="mb-4"
      />

      <!-- Liste des caractéristiques -->
      <n-list bordered hoverable class="rounded-lg shadow">
        <n-list-item
          v-for="characteristic in characteristics"
          :key="characteristic.uri"
          :class="{ 'bg-gray-100': selected?.uri === characteristic.uri }"
          @click="updateSelected(characteristic)"
        >
          <template #default>
            <div>
              <div class="font-medium text-base">{{ characteristic.title }}</div>
            </div>
          </template>

          <template #suffix>
            <n-space :wrap="false" :size="[0, 0]">
              <opensilex-EditButton
                label="Edit"
                :small="true"
                @click="(e) => { e?.stopPropagation?.(); showEditForm(characteristic); }"
              />
            </n-space>
          </template>
        </n-list-item>
      </n-list>
    </div>

    <!-- Détails de la caracteristique selectionnée -->
    <div v-if="selected" class="characteristicDetails">
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

      <!-- Formulaire édition caracteristique -->
      <opensilex-AgroportalCharacteristicForm
        v-if="showForm"
        ref="characteristicFormRef"
        :createTitle="'component.variable.characteristic.add-characteristic'"
        :editTitle="'component.variable.characteristic.edit'"
        :editData="editData"
        @onSuccess="onFormSuccess"
        @onClose="closeForm"
      ></opensilex-AgroportalCharacteristicForm>
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

const selected = ref<any | null>(null);
const editData = ref(null);
const showForm = ref(false);
const characteristicFormRef = ref(null);

const characteristics = ref<any[]>([]);
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

async function fetchCharacteristics() {
  try {
    const rawCharacteristics = await searchElements('');
    characteristics.value = rawCharacteristics.map(characteristic => ({
      ...characteristic,
      title: characteristic.name,
      variables: characteristic.variables || []
    }));
  } catch (e) {
    opensilex.errorHandler(e);
  }
}

onMounted(fetchCharacteristics);


// Affiche les détails
async function updateSelected(characteristic: any) {
   
  const selectedCharacteristicDetails = await fetchCharacteristicDetails(characteristic.uri);
  if (!selectedCharacteristicDetails) return;

console.log("selecteeeed details ", selectedCharacteristicDetails)
  selected.value = {
    ...selectedCharacteristicDetails,
    name: selectedCharacteristicDetails.name || selectedCharacteristicDetails.label || '',
    comment: selectedCharacteristicDetails.description || '',
    publisher: selectedCharacteristicDetails.publisher || '',
    description: selectedCharacteristicDetails.description || '',
    publication_date: selectedCharacteristicDetails.publication_date || '',
    last_update_date: selectedCharacteristicDetails.last_update_date || '',
    exact_match: selectedCharacteristicDetails.exact_match ?? [],
    close_match: selectedCharacteristicDetails.close_match ?? [],
    broad_match: selectedCharacteristicDetails.broad_match ?? [],
    narrow_match: selectedCharacteristicDetails.narrow_match ?? [],
    from_shared_resource_instance: selectedCharacteristicDetails.from_shared_resource_instance ?? null
  };
}

async function fetchCharacteristicDetails (uri: string) {
  try {
    const res = await service.getCharacteristic(uri);
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
async function showEditForm(characteristic: any) {
  const selectedCharacteristicDetails = await fetchCharacteristicDetails(characteristic.uri);
  editData.value = selectedCharacteristicDetails;
  showForm.value = true;
  nextTick(() => {
    characteristicFormRef.value?.showEditForm?.(selectedCharacteristicDetails);
  });
}

function onFormSuccess() {
  showForm.value = false;
  selected.value = null;
  fetchCharacteristics();
}

function closeForm() {
  showForm.value = false;
  editData.value = null;
}

// Requête de recherche
const searchElements = async (filter = ''): Promise<NamedResourceDTO[]> => {
  const orderBy = ['name=asc'];
  let response: HttpResponse<OpenSilexResponse<NamedResourceDTO[]>>;

    response = await service.searchCharacteristics(filter, orderBy);
  return response.response.result;
};

const relationsFields = [
  { key: 'name', label: t("VariableView.name") },
  { key: 'uri', label: 'URI' }
];

watch(search, async (val) => {
  const result = await searchElements(val);
  characteristics.value = result.map(characteristic => ({
    ...characteristic,
    title: characteristic.name,
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
  await fetchCharacteristics()
  await selectFromQuery()                        // <— try preselect from URL on first load
})

watch(
  () => route.query.selected,
  async () => { await selectFromQuery() },
  { immediate: false }
)

// If the element type changes (switching tabs) clear old selection,
// but also try to fetch a new ?selected=... for the new type.
watch(
  () => props.elementType,
  async () => {
    selected.value = null
    await fetchCharacteristics()
    await selectFromQuery()
  }
)

// --- helpers ---
async function selectFromQuery () {
  const q = route.query.selected
  if (!q) return

  const targetUri = typeof q === 'string' ? q : q[0]

  // If item is in the list, just select it
  const inList = characteristics.value.find(e => e.uri === targetUri)
  if (inList) {
    await updateSelected(inList)
    return
  }

  // Otherwise fetch its details directly and build the "selected" object
  const details = await fetchCharacteristicDetails(targetUri)
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

defineExpose({ showCreateForm });
</script>

<style>

.characteristicsContainer {
    display: flex;
}

.characteristicsList, .characteristicDetails {
    width: 45%;
    padding: 10px;
}
</style>
<i18n>
en:
    Characteristic:
        filter-placeholder: Search characteristics by name

fr:
    Characteristic:
        filter-placeholder: Rechercher des caractéristiques par nom
</i18n>

