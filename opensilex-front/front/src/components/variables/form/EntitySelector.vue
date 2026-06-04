<template>
  <n-select
    v-model:value="selectedValue"
    filterable
    clearable
    remote
    :options="options"
    :loading="isLoading"
    :placeholder="placeholder"
    :consistent-menu-width="false"
    @search="onSearch"
    @update:value="onUpdateValue"
    @scroll="onDropdownScroll"
    @keydown.enter.prevent="emit('handlingEnterKey')"
  >
    <template #action>
      <div class="entity-selector-footer" :class="{ greenThemeColor: hasMoreResults }">
        <span v-if="isLoading">
          {{ t('component.common.loading') }}
          <template v-if="totalCount > 0">
            —
            {{ displayedCount }} / {{ totalCount }}
          </template>
        </span>

        <span v-else-if="hasMoreResults && totalCount > 0">
          {{ displayedCount }} / {{ totalCount }}
          —
          {{ t('component.common.scroll-to-load-more') }}
        </span>

        <span v-else-if="totalCount > 0">
          {{ displayedCount }} / {{ totalCount }}
          —
          {{ t('component.common.no-more-results') }}
        </span>

        <span v-else>
          {{ t('component.common.no-results') }}
        </span>
      </div>
    </template>
  </n-select>
</template>

<script setup lang="ts">
import { computed, inject, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { SelectOption } from 'naive-ui'
import { NButton, NTag, NDataTable, DataTableRowKey, NInput, NForm, NFormItem, NSelect, NCheckbox, NCollapse, NCollapseItem, NLayout, NLayoutSider, NLayoutContent, NSpace } from 'naive-ui'
import { VariablesService } from 'opensilex-core'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'

const props = defineProps<{
  selected?: string | null
  placeholder?: string
}>()

const emit = defineEmits<{
  (e: 'update:selected', value: string | null): void
  (e: 'handlingEnterKey'): void
}>()

const { t } = useI18n()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = opensilex.getService<VariablesService>('opensilex.VariablesService')

const PAGE_SIZE = 20

const options = ref<SelectOption[]>([])
const searchText = ref('')
const currentPage = ref(0)
const isLoading = ref(false)
const hasMoreResults = ref(true)
const totalCount = ref(0)
const loadedCount = ref(0)

let searchTimer: number | null = null
let requestId = 0

const selectedValue = computed({
  get: () => props.selected ?? null,
  set: value => emit('update:selected', value as string | null)
})

/**
* Convertit une entité retournée par l’API en option compatible avec n-select.
* Le label affiché privilégie name, puis label/title, puis l’URI en dernier recours.
*/
function mapEntityToOption(entity: any): SelectOption {
  return {
    label: entity.name ?? entity.label ?? entity.title ?? entity.uri,
    value: entity.uri
  }
}

/**
* Charge une page d’entités depuis l’API.
* pageIndex est indexé à partir de 0
*/
async function searchEntitiesPage(filter: string, pageIndex: number) {
  const orderBy = ['name=asc']

  const response = await (service as any).searchEntities(
    filter,
    orderBy,
    pageIndex,
    PAGE_SIZE
  )

  return {
    result: response.response.result ?? [],
    total: response.response.metadata?.pagination?.totalCount ?? 0
  }
}

/**
* Nombre d’éléments affiché dans le footer.
* borné à totalCount pour éviter un affichage du type 22 / 21.
*/
const displayedCount = computed(() => {
  if (totalCount.value <= 0) {
    return options.value.length
  }

  return Math.min(loadedCount.value, totalCount.value)
})

/**
* Charge les entités du sélecteur.
*/
async function loadEntities(reset = false) {
  if (isLoading.value) return
  if (!hasMoreResults.value && !reset) return

  const currentRequestId = ++requestId
  isLoading.value = true

  try {
    if (reset) {
      currentPage.value = 0
      hasMoreResults.value = true
      options.value = []
      totalCount.value = 0
      loadedCount.value = 0
    }

    const { result, total } = await searchEntitiesPage(searchText.value.trim(), currentPage.value)

    /**
    * Ignore une ancienne réponse API si une requête plus récente a été lancée.
    * Utile quand l’utilisateur tape vite dans le champ de recherche.
    */
    if (currentRequestId !== requestId) return

    totalCount.value = total

    loadedCount.value = reset
      ? result.length
      : loadedCount.value + result.length

    const mapped = result.map(mapEntityToOption)

    const existingValues = new Set(options.value.map(option => option.value))

    /**
    * Options de la nouvelle page qui ne sont pas déjà présentes.
    * Évite les doublons si l’API renvoie deux fois la même entité.
    */
    const newResults = mapped.filter(option => !existingValues.has(option.value))

    options.value = reset
      ? newResults
      : [...options.value, ...newResults]

    hasMoreResults.value =
      totalCount.value > 0 &&
      loadedCount.value < totalCount.value &&
      result.length === PAGE_SIZE

    if (result.length > 0) {
      currentPage.value += 1
    }
  } catch (error) {
    opensilex.errorHandler(error)
  } finally {
    if (currentRequestId === requestId) {
      isLoading.value = false
    }
  }
}

/**
* Détecte quand l’utilisateur arrive proche du bas du menu déroulant.
* Si d’autres résultats existent, on charge la page suivante.
*/
function onDropdownScroll(event: Event) {
  const target = event.target as HTMLElement | null
  if (!target) return

  const bottomThreshold = 80
  const position = target.scrollTop + target.clientHeight
  const nearBottom = position >= target.scrollHeight - bottomThreshold

  if (nearBottom && hasMoreResults.value && !isLoading.value) {
    loadEntities(false)
  }
}

/**
* Met à jour le filtre de recherche avec un debounce.
* Évite d’appeler l’API à chaque frappe clavier.
*/
function onSearch(query: string) {
  searchText.value = query ?? ''

  if (searchTimer) {
    window.clearTimeout(searchTimer)
  }

  searchTimer = window.setTimeout(() => {
    loadEntities(true)
  }, 250)
}

/**
* Remonte la nouvelle valeur sélectionnée au parent.
* Lui permet de mettre à jour filter.entity.
*/
function onUpdateValue(value: string | null) {
  emit('update:selected', value)
}

onMounted(() => {
  loadEntities(true)
})

onBeforeUnmount(() => {
  if (searchTimer) {
    window.clearTimeout(searchTimer)
  }
})
</script>

<style scoped>
.entity-selector-footer {
  text-align: center;
}
</style>