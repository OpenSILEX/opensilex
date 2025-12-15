
<template>
  <div class="methodsContainer">

    <div class="methodsList">
      <!-- Recherche d'une methode' -->
      <n-input
        v-model:value="search"
        :placeholder="t('Method.filter-placeholder')"
        clearable
        class="mb-4"
      />

      <!-- Liste des methodes -->
      <n-list bordered hoverable class="rounded-lg shadow">
        <n-list-item
          v-for="method in methods"
          :key="method.uri"
          :class="{ 'bg-gray-100': selected?.uri === method.uri }"
          @click="updateSelected(method)"
        >
          <template #default>
            <div>
              <div class="font-medium text-base">{{ method.title }}</div>
            </div>
          </template>

          <template #suffix>
            <n-space :wrap="false" :size="[0, 0]">
              <opensilex-EditButton
                label="Edit"
                :small="true"
                @click="(e) => { e?.stopPropagation?.(); showEditForm(method); }"
              />
            </n-space>
          </template>
        </n-list-item>
      </n-list>
    </div>

    <!-- Détails de la methode selectionnée -->
    <div v-if="selected" class="methodDetails">
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

      <!-- Liste Documents -->
      <opensilex-DocumentTabList
        v-if="selected && selected.uri"
        :selected="selected"
        :uri="[selected.uri]"
        :search="false"
      ></opensilex-DocumentTabList>


    <!-- Formulaire édition methode -->
    <opensilex-AgroportalMethodForm
      v-if="showForm"
      ref="methodFormRef"
      :createTitle="'component.variable.method.add-method'"
      :editTitle="'component.variable.method.edit'"
      :editData="editData"
      @onCreate="onFormSuccess"
      @onUpdate="onFormSuccess"
      @onClose="closeForm"
    ></opensilex-AgroportalMethodForm>
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
const methodFormRef = ref(null);

const methods = ref<any[]>([]);
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

async function fetchMethods() {
  try {
    const rawMethods = await searchElements('');
    methods.value = rawMethods.map(method => ({
      ...method,
      title: method.name,
      variables: method.variables || []
    }));
  } catch (e) {
    opensilex.errorHandler(e);
  }
}

onMounted(fetchMethods);


// Affiche les détails
async function updateSelected(method: any) {
   
  const selectedMethodDetails = await fetchMethodDetails(method.uri);
  if (!selectedMethodDetails) return;

  selected.value = {
    ...selectedMethodDetails,
    name: selectedMethodDetails.name || selectedMethodDetails.label || '',
    comment: selectedMethodDetails.description || '',
    publisher: selectedMethodDetails.publisher || '',
    description: selectedMethodDetails.description || '',
    publication_date: selectedMethodDetails.publication_date || '',
    last_update_date: selectedMethodDetails.last_update_date || '',
     exact_match: selectedMethodDetails.exact_match ?? [],
    close_match: selectedMethodDetails.close_match ?? [],
    broad_match: selectedMethodDetails.broad_match ?? [],
    narrow_match: selectedMethodDetails.narrow_match ?? [],
    from_shared_resource_instance: selectedMethodDetails.from_shared_resource_instance ?? null
  };
}

async function fetchMethodDetails (uri: string) {
  try {
    const res = await service.getMethod(uri);
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
async function showEditForm(method: any) {
  const selectedMethodDetails = await fetchMethodDetails(method.uri);
  editData.value = selectedMethodDetails;
  showForm.value = true;
  nextTick(() => {
    methodFormRef.value?.showEditForm?.(selectedMethodDetails);
  });
}

async function onFormSuccess(form: any) {
  // On garde en mémoire l'URI à reselectionner :
  // - priorité à form.uri si présent
  // - sinon, la methode déjà sélectionnée avant la sauvegarde
  const previousUri = selected.value?.uri
  const targetUri = form?.uri || previousUri || null

  // On ferme le formulaire
  showForm.value = false
  editData.value = null

  // On recharge la liste
  await fetchMethods()
  await nextTick()

  if (!targetUri) {
    return
  }

  // Si la methode est dans la liste, on l'utilise
  const selectedMethod = methods.value.find(e => e.uri === form.uri)

  if (selectedMethod) {
    await updateSelected(selectedMethod)
  } else {
    // Fallback on construit un mini-objet avec l'URI juste pour updateSelected
    await updateSelected({ uri: targetUri })
  }
}


function closeForm() {
  showForm.value = false;
  editData.value = null;
}

// Requête de recherche
const searchElements = async (filter = ''): Promise<NamedResourceDTO[]> => {
  const orderBy = ['name=asc'];
  let response: HttpResponse<OpenSilexResponse<NamedResourceDTO[]>>;

  response = await service.searchMethods(filter, orderBy);

  return response.response.result;
};

const relationsFields = [
  { key: 'name', label: t("VariableView.name") },
  { key: 'uri', label: 'URI' }
];

watch(search, async (val) => {
  const result = await searchElements(val);
  methods.value = result.map(method => ({
    ...method,
    title: method.name,
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

// -------------------------------------------------------------------

onMounted(async () => {
  await fetchMethods()
  await selectFromQuery()                        // <— try preselect from URL on first load
})

// Try to select whenever the query changes (ex : navigate from VariableDetails)
watch(
  () => route.query.selected,
  async () => { await selectFromQuery() },
  { immediate: false }
)

// If the element type changes (switching tabs), clear old selection,
// but also try to use a new ?selected=... for the new type.
watch(
  () => props.elementType,
  async () => {
    selected.value = null
    await fetchMethods()
    await selectFromQuery()
  }
)

// --- helpers ---
async function selectFromQuery () {
  const selectedElement = route.query.selected
  if (!selectedElement ) return

  // router gives decoded string in Vue 3; if not, decode once
  const targetUri = typeof selectedElement  === 'string' ? selectedElement  : selectedElement [0]

  // If item is in the list, just select it
  const inList = methods.value.find(e => e.uri === targetUri)
  if (inList) {
    await updateSelected(inList)
    return
  }

  // Otherwise fetch its details directly and build the "selected" object
  const details = await fetchMethodDetails(targetUri)
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

.methodsContainer {
    display: flex;
}

.methodsList, .methodDetails {
    width: 45%;
    padding: 10px;
}
</style>
<i18n>
en:
    Method:
        filter-placeholder: Search methods by name

fr:
    Method:
        filter-placeholder: Rechercher des méthodes par nom
</i18n>

