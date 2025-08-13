<template>
  <opensilex-FormField
    :rules="rules"
    :required="required"
    :requiredBlue="requiredBlue"
    :label="label"
    :helpMessage="helpMessage"
  >
    <template #field="{ id, validator }">
      <!-- Hidden input for form validation -->
      <input :id="id" type="hidden" :value="selectedProxy" />

      <div class="select-button-container input-group">
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
          :itemLoadingMethod="itemLoadingMethod"
          :conversionMethod="conversionMethod"
          :defaultSelectedValue="defaultSelectedValue"
          :showCount="showCount"
          :actionHandler="actionHandler"
          :disableBranchNodes="disableBranchNodes"
          @close="() => close(validator)"
          @totalCount="updateTotalCount"
          @resultCount="updateResultCount"
          @select="emitSelect"
          @deselect="emitDeselect"
          class="flex-fill"
        >
          <!-- Bouton "afficher plus" rendu après la liste -->
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

        <!-- Detail View -->
        <div v-if="!actionHandler && viewHandler" class="input-group-append">
          <opensilex-DetailButton
            @click="viewHandler"
            :label="viewHandlerDetailsVisible ? 'FormSelector.hideDetails' : 'FormSelector.showDetails'"
            :small="true"
            class="greenThemeColor"
          />
        </div>

        <!-- Create Entity And Show Details -->
        <div v-else-if="actionHandler" class="input-group-append">
          <n-button class="greenThemeColor" @click="actionHandler">+</n-button>
          <opensilex-DetailButton
            v-if="viewHandler"
            @click="viewHandler"
            :label="viewHandlerDetailsVisible ? 'FormSelector.hideDetails' : 'FormSelector.showDetails'"
            :small="true"
          />
        </div>
      </div>
    </template>
  </opensilex-FormField>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import CustomTreeselect from './CustomTreeselect.vue'
import { NButton } from 'naive-ui'

const { t } = useI18n()

// Props
const props = defineProps<{
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
  rules?: string | Function
  actionHandler?: Function
  viewHandler?: Function
  viewHandlerDetailsVisible?: boolean
  defaultSelectedValue?: boolean
  disableBranchNodes?: boolean
}>()

// Emits
const emit = defineEmits<{
  (e: 'update:selected', value: string | string[] | undefined): void
  (e: 'select', value: any): void
  (e: 'deselect', value: any): void
}>()

// Proxy v-model:selected (lecture/écriture)
const selectedProxy = computed({
  get: () => props.selected,
  set: (v) => emit('update:selected', v)
})

// Local state
const showAllResults = ref(false)
const totalCount = ref(0)
const resultCount = ref(0)
const resultLimit = ref(10)

// Refs
const customTreeselect = ref<InstanceType<typeof CustomTreeselect> | null>(null)

// Methods
const refresh = () => customTreeselect.value?.refresh()
const openTreeselect = () => customTreeselect.value?.openTreeselect()

const loadMoreItems = () => {
  resultLimit.value = 0
  showAllResults.value = true
  customTreeselect.value.refresh(resultLimit.value)   // ← passe le nouveau limit à CustomTreeselect
  nextTick(() => openTreeselect())
}



const close = (validator: any) => {
  if (validator) nextTick(() => validator.validate())
}

const emitSelect = (value: any) => emit('select', value)
const emitDeselect = (value: any) => emit('deselect', value)

const updateTotalCount = (newTotal: number) => (totalCount.value = newTotal)
const updateResultCount = (newResult: number) => (resultCount.value = newResult)

onMounted(() => {
  console.log('[FormSelector] mounted. searchMethod:', typeof props.searchMethod)
})
</script>

<style scoped lang="scss">
.select-button-container {
  margin-bottom: 0;
}

.input-group-append > button {
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
  white-space: normal; /* retour à la ligne pour ne pas couper le message */
}

.select-button-container {
    margin-bottom: 0; /* toujours necessaire ?*/
    width: 100%; 
}

 /* s’assure que le group lui-même prend toute la ligne */
.flex-fill { flex: 1 1 auto; min-width: 0; }   /* classe officielle bootstrap qui fait prendre toute la place disponible dans la ligne à un element dans un conteneur display:flex */

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
