<template>
  <div>
    <!-- Barre de recherche -->
    <div class="input-group">
      <n-input
        v-model:value="searchText"
        type="text"
        :placeholder="t(placeholder)"
        @keyup.enter="emitChange"
        @input="onInputValueChange"
      />
      <button class="btn btn-outline-secondary agroportalCleanSearchBtn" type="button" @click="cleanSearchField">
        <i class="bi-x"></i>
      </button>
      <button class="btn btn-outline-secondary agroportalSearchBarBtn" type="button" @click="toggleAdvanced">
        <i class="bi-funnel"></i>
      </button>
      <button class="btn btn-outline-secondary agroportalSearchBarBtn" type="button" @click="emitChange">
        <i class="bi-search"></i>
      </button>
    </div>

    <!-- Options avancées -->
    <n-collapse :default-expanded-names="showAdvanced ? ['advanced'] : []" class="mt-2">
      <n-collapse-item name="advanced" title="">
        <div class="row align-items-center g-3">
          <div class="col-12 col-lg-8">
            <opensilex-FormSelector
              ref="soSelector"
              label="Ontologies"
              v-model:selected="ontologiesURIsProxy"
              :multiple="true"
              :searchMethod="searchOntologies"
              :itemLoadingMethod="loadOntologies"
              :conversionMethod="ontologyToSelectNode"
              :disabled="isAllOntologiesSelectedProxy"
            />
          </div>
          <div class="col-12 col-lg-4 d-flex align-items-center">
            <input
              id="chk-all-ontologies"
              class="form-check-input me-2"
              type="checkbox"
              :checked="isAllOntologiesSelectedProxy"
             @change="onAllOntologiesToggle($event)"
            />
            <label class="form-check-label" for="chk-all-ontologies">
              {{ t('AgroportalSearch.all-ontologies') }}
            </label>
          </div>
        </div>
      </n-collapse-item>
    </n-collapse>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, inject } from 'vue'
import { useI18n } from 'vue-i18n'
import { NInput, NCollapse, NCollapseItem } from 'naive-ui'
import type { OpenSilexVuePlugin } from '@/models/OpenSilexVuePlugin'
import type { AgroportalAPIService } from 'opensilex-core/api/agroportalAPI.service'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'
import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OntologyAgroportalDTO } from 'opensilex-core/model/ontologyAgroportalDTO'

// ---- Props / Emits
const props = withDefaults(defineProps<{
  placeholder?: string
  selected?: string[]
  isAllOntologies?: boolean
}>(), {
  placeholder: 'AgroportalSearch.enter-search-text',
  selected: () => [],
  isAllOntologies: false
})

const emit = defineEmits<{
  (e: 'change', text: string): void
  (e: 'inputValueHasChanged', text: string): void
  (e: 'update:selected', v: string[]): void
  (e: 'update:isAllOntologies', v: boolean): void
}>()

const { t } = useI18n()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

// ---- State
const searchText = ref<string>('')

// “Collapse” simple en JS pour remplacer <b-collapse>
const showAdvanced = ref(false)
function toggleAdvanced () { showAdvanced.value = !showAdvanced.value }

const ontologiesURIsProxy = computed<string[]>({
  get: () => props.selected ?? [],
  set: (v) => emit('update:selected', Array.isArray(v) ? v : [])
})

const isAllOntologiesSelectedProxy = computed<boolean>({
  get: () => !!props.isAllOntologies,
  set: (v) => emit('update:isAllOntologies', !!v)
})

// ---- Services
function loadOntologies(ontologieAcronyms: string[]): Promise<OntologyAgroportalDTO[]> {
  return opensilex
    .getService<AgroportalAPIService>('opensilex.AgroportalAPIService')
    .getAgroportalOntologies('', ontologieAcronyms)
    .then((http: HttpResponse<OpenSilexResponse<OntologyAgroportalDTO[]>>) => http.response.result)
    .catch(agroportalErrorHandler)
}

function searchOntologies(
  searchQuery: string,
  _page: number,
  _pageSize: number
): Promise<HttpResponse<OpenSilexResponse<OntologyAgroportalDTO[]>>> {
  return opensilex
    .getService<AgroportalAPIService>('opensilex.AgroportalAPIService')
    .getAgroportalOntologies(searchQuery, undefined)
}

function ontologyToSelectNode(dto: OntologyAgroportalDTO) {
  // attendu par FormSelector au minimum : { id, label }
  return { id: dto.acronym, label: `${dto.acronym} (${dto.name})` }
}

function agroportalErrorHandler(error: any): OntologyAgroportalDTO[] {
  if (error?.status === 503) return []
  opensilex.errorHandler(error)
  return []
}

function onAllOntologiesToggle(e: Event) {
  const target = e.target as HTMLInputElement
  isAllOntologiesSelectedProxy.value = !!target.checked
}

function setSearchText(text: string) {
  searchText.value = text
  emitChange()
}
defineExpose({ setSearchText })

// ---- Events
function emitChange() {
  if (searchText.value.trim() !== '') {
    emit('change', searchText.value)
  }
}

function cleanSearchField() {
  searchText.value = ''
}

function onInputValueChange(val: string) {
  searchText.value = val
  if (searchText.value.trim() !== '') {
    emit('inputValueHasChanged', searchText.value)
  }
}
</script>

<style scoped>
/* panneau avancé */
.n-collapse {
  border: 1px solid rgba(0,0,0,.125);
  border-radius: .25rem;
  padding: .5rem .75rem;
}

.agroportalSearchBarBtn, .agroportalCleanSearchBtn {
  color: #00A38D;
  border-color: #00A38D;
}
.agroportalSearchBarBtn:hover {
  background: #00A38D;
  color: white;
}
.agroportalCleanSearchBtn:hover {
  color: white;
  background: red;
  border-color: red;
}
</style>


<i18n>
en:
  AgroportalSearch:
    enter-search-text: Enter a name
    all-ontologies: All ontologies
fr:
  AgroportalSearch:
    enter-search-text: Entrer un nom
    all-ontologies: Toutes les ontologies
</i18n>

