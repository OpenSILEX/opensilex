<template>
  <opensilex-FormField
    :required="required"
    :requiredBlue="requiredBlue"
    :label="label"
    :helpMessage="helpMessage"
  >
    <template #field="{ id }">
      <!-- NFormItem gère l'astérisque, la bordure rouge et le message via les rules du NForm parent -->
      <n-form-item :path="path" :show-label="false">
        <div class="select-button-container">
          <opensilex-CustomTreeselect
            ref="customTreeselect"
            v-bind="$attrs"
            v-model:selected="selectedProxy"
            :searchMethod="searchMethod"
            :resultLimit="resultLimit"
            :multiple="multiple"
            :checkable="checkable"
            :placeholder="placeholder"
            :disabled="disabled"
            :optionsLoadingMethod="optionsLoadingMethod"
            :options="options"
            :viewHandler="viewHandler"
            :conversionMethod="conversionMethod"
            :defaultSelectedValue="defaultSelectedValue"
            :showCount="showCount"
            :actionHandler="actionHandler"
            :disableBranchNodes="disableBranchNodes"
            :itemLoadingMethod="itemLoadingMethod || undefined"
            class="select-main"
            @totalCount="updateTotalCount"
            @resultCount="updateResultCount"
            @close="onBlur"
            @select="(v) => emit('select', v)"
            @deselect="(v) => emit('deselect', v)"
          >
            <template #after-list>
              <n-button
                v-if="resultCount < totalCount && !showAllResults"
                text
                size="small"
                class="refineSearchMessage"
                @mousedown.prevent.stop
                @click="loadMoreItems"
              >
                {{ t('FormSelector.refineSearchMessage', { resultCount, totalCount }) }}
              </n-button>
            </template>
          </opensilex-CustomTreeselect>

          <div v-if="!actionHandler && viewHandler" class="select-side-button">
            <opensilex-DetailButton
              @click="viewHandler"
              :label="viewHandlerDetailsVisible ? t('FormSelector.hideDetails') : t('FormSelector.showDetails')"
              :small="true"
              class="greenThemeColor"
            />
          </div>

          <div v-else-if="actionHandler" class="select-side-button">
            <n-button class="greenThemeColor" @click="actionHandler">+</n-button>
            <opensilex-DetailButton
              v-if="viewHandler"
              @click="viewHandler"
              :label="viewHandlerDetailsVisible ? t('FormSelector.hideDetails') : t('FormSelector.showDetails')"
              :small="true"
            />
          </div>
        </div>
      </n-form-item>
    </template>
  </opensilex-FormField>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch, inject } from 'vue'
import { useI18n } from 'vue-i18n'
import { NButton, NFormItem } from 'naive-ui'
import CustomTreeselect from './CustomTreeselect.vue'

// API interne Naive UI pour accéder au NForm parent
import { formInjectionKey } from 'naive-ui/es/form/src/context'

const { t } = useI18n()

defineOptions({ inheritAttrs: false })

const props = defineProps<{
  path: string
  selected: string | string[] | undefined
  searchMethod?: Function
  multiple?: boolean
  checkable?: boolean
  itemLoadingMethod?: Function
  optionsLoadingMethod?: Function
  options?: any[]
  showCount?: boolean
  conversionMethod?: Function
  label?: string
  helpMessage?: string
  placeholder?: string
  noResultsText?: string
  required?: boolean
  requiredBlue?: boolean
  disabled?: boolean
  actionHandler?: Function
  viewHandler?: Function
  viewHandlerDetailsVisible?: boolean
  defaultSelectedValue?: boolean
  disableBranchNodes?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:selected', value: string | string[] | undefined): void
  (e: 'select', value: any): void
  (e: 'deselect', value: any): void
}>()

// v-model proxy
const selectedProxy = computed<string | string[] | undefined>({
  get: () => props.selected,
  set: (v) => emit('update:selected', v)
})

// Récupère le NForm parent pour valider explicitement le champ
const nForm = inject(formInjectionKey, null)

// Valide le champ après propagation du v-model dans le `form`
watch(
  () => selectedProxy.value,
  async () => {
    await nextTick()
    // force la validation de ce field (règles & messages du NForm parent)
    nForm?.validateField?.(props.path)
  }
)

// Valide aussi au blur (si on veut trigger cet evenement)
function onBlur () {
  nextTick(() => {
    nForm?.validateField?.(props.path)
  })
}

const showAllResults = ref(false)
const totalCount = ref(0)
const resultCount = ref(0)
const resultLimit = ref(10)

const customTreeselect = ref<InstanceType<typeof CustomTreeselect> | null>(null)
const refresh = () => customTreeselect.value?.refresh()
const openTreeselect = () => customTreeselect.value?.openTreeselect()

function loadMoreItems () {
  resultLimit.value = 0
  showAllResults.value = true
  customTreeselect.value?.refresh(resultLimit.value)
  nextTick(() => openTreeselect())
}

function updateTotalCount (n: number) { totalCount.value = n }
function updateResultCount (n: number) { resultCount.value = n }

onMounted(() => {
  console.log('[FormSelector] mounted. searchMethod:', typeof props.searchMethod)
})
</script>



<style scoped lang="scss">
.select-button-container {
  display: flex;
  align-items: stretch;
  width: 100%;
  gap: 0;
  flex-wrap: nowrap;
}

.select-main {
  flex: 1 1 auto;
  min-width: 0;
}

.select-side-button {
  flex: 0 0 auto;
  display: flex;
  align-items: stretch;
  margin-left: 8px;
}

.select-side-button > * {
  height: 100%;
}

.greenThemeColor {
  color: #fff;
}

.refineSearchMessage {
  font-weight: bold;
  background-color: #00A28C;
  color: #FFFFFF;
  cursor: pointer;
  white-space: normal;
}

/* Le composant sélection prend toute la largeur dispo */
:deep(.n-base-selection) {
  width: 100%;
}
</style>

<i18n>
en:
  FormSelector:
    refineSearchMessage: "{resultCount} / {totalCount} results displayed, please refine your search or click HERE to display all results"
    showDetails: "Show details"
    hideDetails: "Hide details"
fr:
  FormSelector:
    refineSearchMessage: "{resultCount} / {totalCount} résultats affichés, merci de préciser votre recherche ou de cliquer ICI pour afficher tous les résultats"
    showDetails: "Afficher les détails"
    hideDetails: "Masquer les détails"
</i18n>
