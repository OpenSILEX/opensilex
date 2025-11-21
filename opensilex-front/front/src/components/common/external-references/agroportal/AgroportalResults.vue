<template>
  <opensilex-Overlay :show="isDataLoading">
    <div
      id="agroportal-results"
      class="container-fluid scrollable"
      v-show="terms.length > 0 || isNothingFound || isAgroportalDown"
    >
      <div class="wrapper">
        <div v-if="(isNothingFound || isAgroportalDown) && !isDataLoading">
          {{ t('AgroportalResults.nothing-found') }}
        </div>

        <!-- Liste des résultats -->
        <opensilex-AgroportalResultItem
          v-for="(term, index) in terms"
          :key="term.id"
          :entity="term"
          :ref="el => (resultItems[index] = el)"
          @item-clicked="selectItem(index)"
        >
          <!-- Bouton pour le mode mapping -->
          <template v-if="isMappingMode" #validationButton>
            <opensilex-SkosSelector
              right
              @update:selectedRelation="relation => onRelationSelected(term, relation)"
            />
          </template>

          <!-- Bouton par défaut -->
          <template v-else #validationButton>
            <opensilex-CreateButton
              class="v-step-result-import-button"
              :label="t('AgroportalResults.btn-select')"
              @click="emitImport(term)"
            />
          </template>
        </opensilex-AgroportalResultItem>
      </div>
    </div>
  </opensilex-Overlay>
</template>

<script setup lang="ts">
import { ref, inject, defineExpose } from 'vue'
import { useI18n } from 'vue-i18n'
import type { OpenSilexVuePlugin } from '@/models/OpenSilexVuePlugin'
import type { AgroportalAPIService } from 'opensilex-core/api/agroportalAPI.service'
import type { AgroportalTermDTO } from 'opensilex-core/model/agroportalTermDTO'
import type { UriSkosRelation } from './../../../../models/SkosRelations'
import { CLOSE_MATCH } from './../../../../models/SkosRelations'

/** Props */
const props = withDefaults(defineProps<{
  isMappingMode?: boolean
}>(), {
  isMappingMode: false
})

/** Emits */
const emit = defineEmits<{
  (e: 'import', term: AgroportalTermDTO): void
  (e: 'importMapping', rel: UriSkosRelation): void
}>()

/** i18n */
const { t } = useI18n()

/** Services / plugin */
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const service = opensilex.getService<AgroportalAPIService>('opensilex.AgroportalAPIService')

/** State */
const terms = ref<AgroportalTermDTO[]>([])
const selectedIndex = ref<number | null>(null)
const isAgroportalDown = ref(false)
const isDataLoading = ref(false)
const isNothingFound = ref(false)

/** Refs vers les items rendus (array de refs) */
const resultItems = ref<any[]>([])

/** recherche */
async function search(searchedText: string, withAllOntologies: boolean, ontologies: string[]) {
  if (!searchedText) {
    clear()
    return
  }
  isDataLoading.value = true
  isAgroportalDown.value = false
  isNothingFound.value = false
  terms.value = []
  try {
    opensilex.disableLoader()
    const http: any = await service.searchThroughAgroportal(
      searchedText,
      !withAllOntologies ? ontologies : undefined
    )
    const list = http?.response?.result as AgroportalTermDTO[] | undefined
    terms.value = Array.isArray(list) ? list : []
    isNothingFound.value = terms.value.length === 0
  } catch (e) {
    isAgroportalDown.value = true
    opensilex.errorHandler(e)
  } finally {
    isDataLoading.value = false
  }
}

/** Pour le tutoriel : sélectionner visuellement un item */
function selectItem(index: number) {
  if (selectedIndex.value != null) {
    // chaque item doit exposer setSelected(boolean)
    resultItems.value[selectedIndex.value]?.setSelected?.(false)
  }
  resultItems.value[index]?.setSelected?.(true)
  selectedIndex.value = index
}

/** Pour le tutoriel : sélectionner + importer le 1er item */
function selectAndImportFirstItem() {
  if (!terms.value.length) return
  selectItem(0)
  emit('import', terms.value[0])
}

/** Pour le tutoriel : sélectionner + mapper le 1er item (closeMatch) */
function selectAndMapFirstItem() {
  if (!terms.value.length) return
  selectItem(0)
  emitImportMapping({
    relationDtoKey: CLOSE_MATCH.dtoKey,
    uri: terms.value[0].id
  })
}

/** Clear local */
function clear() {
  terms.value = []
  selectedIndex.value = null
  isNothingFound.value = false
}

/** Emits helpers */
function emitImport(term: AgroportalTermDTO) {
    console.log("emitImport dans agroResults term ", term)
  emit('import', term)
}
function emitImportMapping(rel: UriSkosRelation) {
  emit('importMapping', rel)
}

/** Handler relation choisie dans SkosSelector */
function onRelationSelected(term: AgroportalTermDTO, selectedRelation: string) {
  emitImportMapping({
    relationDtoKey: selectedRelation,
    uri: term.id
  })
}

/** Expose API au parent (tutoriel) */
defineExpose({
  search,
  selectItem,
  selectAndImportFirstItem,
  selectAndMapFirstItem
})
</script>

<style scoped>
#agroportal-results {
  min-height: 50px;
  background: rgba(0, 0, 0, .05);
}
.scrollable {
  max-height: 500px;
  overflow-y: auto;
}
</style>

<i18n>
en:
  AgroportalResults:
    nothing-found: No result was found
    btn-select: Select
fr:
  AgroportalResults:
    nothing-found: Aucun résultat n'a été trouvé
    btn-select: Sélectionner
</i18n>
