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

      <!-- Conteneur scrollable -->
      <div
        ref="scrollEl"
        class="entitiesScroll"
        @scroll.passive="onScroll"
      >
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
                  :label="t('component.common.list.buttons.update')"
                  :small="true"
                  @click="(e) => { e?.stopPropagation?.(); showEditForm(entity); }"
                />
              </n-space>
            </template>
          </n-list-item>
        </n-list>

        <!-- Footer infinite scroll -->
        <div class="listFooter">
          <div v-if="loading" class="py-2">
            {{ t('component.common.loading') }}
          </div>
          <div v-else-if="!hasMore" class="py-2">
            {{ t('component.common.no-more-results') }}
          </div>
        </div>
      </div>
    </div>

    <!-- Détails de l'entité sélectionnée -->
    <div v-if="selected" class="entityDetails">
      <opensilex-VariableStructureDetails :selected="selected" />

      <div>
        <opensilex-Card
          label="component.skos.ontologies-references-label"
          icon="fa#globe-americas"
          :no-footer="true"
        >
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
        @onCreate="onFormSuccess"
        @onUpdate="onFormSuccess"
        @onClose="closeForm"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onBeforeMount, resolveComponent, onMounted, inject, defineExpose, nextTick, watch, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { VariablesService, NamedResourceDTO } from 'opensilex-core/index'
import HttpResponse, { OpenSilexResponse } from '../../../lib/HttpResponse'
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin"
import { useRoute } from 'vue-router'
import { NInput, NList, NListItem, NSpace } from 'naive-ui'

const { t } = useI18n()
const opensilex = inject<OpenSilexVuePlugin>("$opensilex")!
const route = useRoute()

const props = defineProps<{ elementType: string }>()

const selected = ref<any | null>(null)
const editData = ref(null)
const showForm = ref(false)
const entityFormRef = ref(null)

const entities = ref<any[]>([])
const search = ref('')

let service: VariablesService
const UriLink = resolveComponent('opensilex-UriLink')

// ------- Infinite scroll state -------
const PAGE_SIZE = 20
const page = ref(0)
const loading = ref(false)
const hasMore = ref(true)

const scrollEl = ref<HTMLElement | null>(null)

// ------- init -------
onBeforeMount(() => {
  service = opensilex.getService('opensilex-core.VariablesService')
})

// --- API: page/page_size ---
async function searchEntitiesPage(filter: string, pageIndex: number) {
  const orderBy = ['name=asc']
  const response: HttpResponse<OpenSilexResponse<any[]>> =
    await (service as any).searchEntities(filter, orderBy, pageIndex, PAGE_SIZE)

  return response.response.result ?? []
}

async function loadEntities(reset = false) {
  if (loading.value) return
  if (!hasMore.value && !reset) return

  loading.value = true
  try {
    if (reset) {
      page.value = 0
      hasMore.value = true
      entities.value = []
    }

    const filter = search.value?.trim() ?? ''
    const result = await searchEntitiesPage(filter, page.value)

    const mapped = result.map((entity: any) => ({
      ...entity,
      title: entity.name,
      variables: entity.variables || []
    }))

    // append
    entities.value = reset ? mapped : [...entities.value, ...mapped]

    // hasMore si on a rempli une page complète
    hasMore.value = mapped.length === PAGE_SIZE

    // next page
    if (mapped.length > 0) page.value += 1
  } catch (e) {
    opensilex.errorHandler(e)
  } finally {
    loading.value = false
  }
}

// ------- Scroll handler -------
function onScroll() {
  const el = scrollEl.value
  if (!el) return

  const bottomThreshold = 120
  const pos = el.scrollTop + el.clientHeight
  const nearBottom = pos >= el.scrollHeight - bottomThreshold

  if (nearBottom) {
    loadEntities(false)
  }
}

// ------- Details -------
async function fetchEntityDetails(uri: string) {
  try {
    const response = await service.getEntity(uri)
    return response.response.result
  } catch (error) {
    opensilex.errorHandler(error)
    return null
  }
}

async function updateSelected(entity: any) {
  const details = await fetchEntityDetails(entity.uri)
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
  }
}

// ------- Forms -------
function showCreateForm() {
  editData.value = null
  showForm.value = true
}

async function showEditForm(entity: any) {
  const details = await fetchEntityDetails(entity.uri)
  editData.value = details
  showForm.value = true
  nextTick(() => {
    entityFormRef.value?.showEditForm?.(details)
  })
}

async function onFormSuccess(form: any) {
  // garder uri pour reselection
  const previousUri = selected.value?.uri
  const targetUri = form?.uri || previousUri || null

  showForm.value = false
  editData.value = null

  // reload list (pour voir nouveau nom)
  await loadEntities(true)
  await nextTick()

  if (targetUri) {
    // si pas dans la page chargée, on peut quand même rafraîchir la carte via getEntity
    const inList = entities.value.find(e => e.uri === targetUri)
    await updateSelected(inList ?? { uri: targetUri })
  }
}

function closeForm() {
  showForm.value = false
  editData.value = null
}

// ------- Search debounce -------
let searchTimer: number | null = null
watch(search, () => {
  if (searchTimer) window.clearTimeout(searchTimer)
  searchTimer = window.setTimeout(() => {
    loadEntities(true)
  }, 250)
})

// ------- route selection -------
onMounted(async () => {
  await loadEntities(true)
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
    await loadEntities(true)
    await selectFromQuery()
  }
)

async function selectFromQuery() {
  const selectedElement = route.query.selected
  if (!selectedElement) return
  const targetUri = typeof selectedElement === 'string' ? selectedElement : selectedElement[0]

  const inList = entities.value.find(e => e.uri === targetUri)
  if (inList) {
    await updateSelected(inList)
    return
  }

  // sinon, juste charger les détails
  await updateSelected({ uri: targetUri })
}

// ------- expose -------
defineExpose({ showCreateForm, onFormSuccess })
</script>

<style scoped>
.entitiesContainer {
  display: flex;
}
.entitiesList, .entityDetails {
  width: 45%;
  padding: 10px;
}

/* scroll interne */
.entitiesScroll {
  max-height: 70vh;           /* ajuste */
  overflow-y: auto;
  padding-right: 6px;         /* éviter que la scrollbar recouvre */
}

.listFooter {
  display: flex;
  justify-content: center;
  font-size: 0.9rem;
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
