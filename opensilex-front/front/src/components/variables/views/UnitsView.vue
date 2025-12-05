
<template>
  <div class="unitsContainer">

    <div class="unitsList">
      <!-- Recherche d'une unité/echelle' -->
      <n-input
        v-model:value="search"
        :placeholder="t('Unit.filter-placeholder')"
        clearable
        class="mb-4"
      />

      <!-- Liste des unités/echelles -->
      <n-list bordered hoverable class="rounded-lg shadow">
        <n-list-item
          v-for="unit in units"
          :key="unit.uri"
          :class="{ 'bg-gray-100': selected?.uri === unit.uri }"
          @click="updateSelected(unit)"
        >
          <template #default>
            <div>
              <div class="font-medium text-base">{{ unit.title }}</div>
            </div>
          </template>

          <template #suffix>
            <n-space :wrap="false" :size="[0, 0]">
              <opensilex-EditButton
                label="Edit"
                :small="true"
                @click="(e) => { e?.stopPropagation?.(); showEditForm(unit); }"
              />
            </n-space>
          </template>
        </n-list-item>
      </n-list>
    </div>

    <!-- Détails de l'unité/echelle selectionnée -->
    <div v-if="selected" class="unitDetails">
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

    <!-- Formulaire édition unité/echelle -->
    <opensilex-AgroportalUnitForm
      v-if="showForm"
      ref="unitFormRef"
      :createTitle="'component.variable.unit.add-unit'"
      :editTitle="'component.variable.unit.edit'"
      :editData="editData"
      @onSuccess="onFormSuccess"
      @onClose="closeForm"
    ></opensilex-AgroportalUnitForm>
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
const unitFormRef = ref(null);

const units = ref<any[]>([]);
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

async function fetchUnits() {
  try {
    const rawUnits = await searchElements('');
    units.value = rawUnits.map(cnit => ({
      ...cnit,
      title: cnit.name,
      variables: cnit.variables || []
    }));
  } catch (e) {
    opensilex.errorHandler(e);
  }
}

onMounted(fetchUnits);


// Affiche les détails
async function updateSelected(unit: any) {
   
  const selectedUnitDetails = await fetchUnitDetails(unit.uri);
  if (!selectedUnitDetails) return;

console.log("selecteeeed details ", selectedUnitDetails)
  selected.value = {
    ...selectedUnitDetails,
    name: selectedUnitDetails.name || selectedUnitDetails.label || '',
    comment: selectedUnitDetails.description || '',
    publisher: selectedUnitDetails.publisher || '',
    description: selectedUnitDetails.description || '',
    publication_date: selectedUnitDetails.publication_date || '',
    last_update_date: selectedUnitDetails.last_update_date || '',
    exact_match: selectedUnitDetails.exact_match ?? [],
    close_match: selectedUnitDetails.close_match ?? [],
    broad_match: selectedUnitDetails.broad_match ?? [],
    narrow_match: selectedUnitDetails.narrow_match ?? [],
    from_shared_resource_instance: selectedUnitDetails.from_shared_resource_instance ?? null
  };
}

async function fetchUnitDetails (uri: string) {
  try {
    const res = await service.getUnit(uri);
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
async function showEditForm(unit: any) {
  const selectedUnitDetails = await fetchUnitDetails(unit.uri)
  editData.value = selectedUnitDetails;
  showForm.value = true;
  nextTick(() => {
    unitFormRef.value?.showEditForm?.(selectedUnitDetails);
  });
}

function onFormSuccess() {
  showForm.value = false;
  selected.value = null;
  fetchUnits();
}

function closeForm() {
  showForm.value = false;
  editData.value = null;
}

// Requête de recherche
const searchElements = async (filter = ''): Promise<NamedResourceDTO[]> => {
  const orderBy = ['name=asc'];
  let response: HttpResponse<OpenSilexResponse<NamedResourceDTO[]>>;

    response = await service.searchUnits(filter, orderBy);
  return response.response.result;
};

const relationsFields = [
  { key: 'name', label: t("VariableView.name") },
  { key: 'uri', label: 'URI' }
];

watch(search, async (val) => {
  const result = await searchElements(val);
  units.value = result.map(unit => ({
    ...unit,
    title: unit.name,
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
  await fetchUnits()
  await selectFromQuery()  // <— try preselect from URL on first load
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
    await fetchUnits()
    await selectFromQuery()
  }
)

// --- helpers ---
async function selectFromQuery () {
  const q = route.query.selected
  if (!q) return

  const targetUri = typeof q === 'string' ? q : q[0]

  // If item is in the list, just select it
  const inList = units.value.find(e => e.uri === targetUri)
  if (inList) {
    await updateSelected(inList)
    return
  }

  // Otherwise fetch its details directly and build the "selected" object
  const details = await fetchUnitDetails(targetUri)
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

.unitsContainer {
    display: flex;
}

.unitsList, .unitDetails {
    width: 45%;
    padding: 10px;
}
</style>
<i18n>
en:
    Unit:
        filter-placeholder: Search units by name

fr:
    Unit:
        filter-placeholder: Rechercher des unités par nom
</i18n>

