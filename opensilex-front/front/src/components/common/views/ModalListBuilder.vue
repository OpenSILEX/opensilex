<template>
  <opensilex-FormField
    :required="required"
    :requiredBlue="requiredBlue"
    :label="fieldLabel"
    :helpMessage="helpMessage"
  >
    <template #field>
      <div class="builder-summary">
        <div
          class="summary-box form-control d-flex align-items-center justify-content-between gap-2"
          :class="{ 'is-disabled': disabled }"
          @click="!disabled && show()"
        >
          <div class="summary-content">
            <template v-if="parseListForTreeselect.length > 0">
              <div class="d-flex flex-wrap gap-2">
                <n-tag
                  v-for="item in parseListForTreeselect"
                  :key="item.id"
                  closable
                  size="small"
                  @close="removeLineFromTag(item, $event)"
                >
                  {{ item.label }}
                </n-tag>
              </div>
            </template>

            <template v-else>
              <span class="text-muted">
                {{ placeholder }}
              </span>
            </template>
          </div>

          <div class="summary-actions d-flex align-items-center gap-2">
            <button
              type="button"
              class="btn greenThemeColor createButton"
              :disabled="disabled"
              @click.stop="show"
            >
              >>
            </button>
          </div>
        </div>
      </div>

      <n-modal
        v-model:show="showModal"
        preset="card"
        :title="modalTitle"
        size="huge"
        :mask-closable="false"
        :closable="false"
        :segmented="{ content: true, footer: 'soft' }"
        class="modal-list-builder-modal"
      >
        <template #header>
          <div class="d-flex align-items-center gap-2">
            <i class="bi bi-search"></i>
            <span>{{ modalTitle }}</span>
          </div>
        </template>

        <div class="card">
          <div v-if="modalExplanation">
            <p>{{ modalExplanation }}</p>
          </div>

            <div
            v-for="(singleLine, index) in lineList"
            :key="singleLine.id"
            class="linerow criteria-border"
            >
            <component
                :is="lineComponent"
                :lineData="singleLine"
                :lineIndex="index"
                :extraProps="extraProps"
                class="row col"
                @updateLine="updateLine"
            />

            <div class="remove-criteria-button-container">
                <n-button
                size="small"
                class="greenThemeColor add-criteria-button"
                @click="removeLine(singleLine.id)"
                >
                <font-awesome-icon icon="minus" />
                </n-button>
            </div>
            </div>

            <div class="add-criteria-button-container">
            <n-button
                size="small"
                class="greenThemeColor add-criteria-button"
                @click="addNewSingleLineToList(null)"
            >
                <font-awesome-icon icon="plus" />
            </n-button>
            </div>
        </div>

        <template #footer>
          <div class="d-flex justify-content-end align-items-center gap-2 w-100">
            <button
              type="button"
              class="btn btn-secondary"
              @click="hide(false)"
            >
              {{ t('ModalListBuilder.cancel-button') }}
            </button>

            <button
              type="button"
              class="btn greenThemeColor"
              @click="hide(true)"
            >
              {{ t('ModalListBuilder.validate-button') }}
            </button>

            <n-tooltip trigger="hover">
              <template #trigger>
                <span class="validateHelp d-inline-flex align-items-center">
                  <font-awesome-icon
                    tabindex="0"
                    icon="question-circle"
                  />
                </span>
              </template>
              {{ t('ModalListBuilder.validate-explanation') }}
            </n-tooltip>
          </div>
        </template>
      </n-modal>
    </template>
  </opensilex-FormField>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { NModal, NSteps, NStep, NButton, NSpace, NTooltip} from 'naive-ui'
import { useI18n } from 'vue-i18n'

export interface SelectableItem {
  id: string
  label: string
}

export interface LineData {
  id: string
  [key: string]: any
}

const emit = defineEmits<{
  (e: 'validateList', value: any[]): void
}>()

const props = withDefaults(defineProps<{
  helpMessage?: string
  modalExplanation?: string
  fieldLabel?: string
  modalTitle?: string
  placeholder?: string
  disabled?: boolean
  required?: boolean
  requiredBlue?: boolean
  lineComponent: string | object
  extraProps?: any
  parseSingleLineForTreeselect: (singleLine: LineData) => SelectableItem
  generateEmptyLine: (lineId: number) => LineData
  filterIncompleteLines: (lines: Array<LineData>) => Array<LineData>
  convertLineToOutputObject: (singleLine: LineData) => any
}>(), {
  disabled: false,
  required: false,
  requiredBlue: false
})

const showModal = ref(false)

const listToOutput = ref<any[]>([])
const lineList = ref<LineData[]>([])
const lineListOnLastValidate = ref<LineData[]>([])

const idCount = ref(1)
const idCountOnLastValidate = ref(0)
const { t } = useI18n()

const parseListForTreeselect = computed<SelectableItem[]>(() => {
  const filteredIncompleteLines = props.filterIncompleteLines(lineListOnLastValidate.value)
  return filteredIncompleteLines.map((singleLine) =>
    props.parseSingleLineForTreeselect(singleLine)
  )
})

function updateLine(fieldsToChange: Record<string, any>, lineListIndex: number) {
  lineList.value = lineList.value.map((line, index) => {
    if (index === lineListIndex) {
      for (const [key, value] of Object.entries(fieldsToChange)) {
        line[key] = value
      }
    }
    return line
  })
}

function setOutputList() {
  listToOutput.value = props
    .filterIncompleteLines(lineListOnLastValidate.value)
    .map((line) => props.convertLineToOutputObject(line))

  emit('validateList', listToOutput.value)
}

function removeLineUsingTreeselectItem(lineIdAndLabel: SelectableItem) {
  removeLine(lineIdAndLabel.id)
}

function removeLineAndSave(lineIdAndLabel: SelectableItem) {
  removeLineUsingTreeselectItem(lineIdAndLabel)
  setOnLastValidateParameters()
  setOutputList()
}

function removeLineFromTag(item: SelectableItem, event: MouseEvent) {
  event.stopPropagation()
  removeLineAndSave(item)
}

function removeLine(id: string) {
  lineList.value = lineList.value.filter((singleLine) => singleLine.id !== id)

  if (lineList.value.length === 0) {
    resetLineList()
  }
}

function resetLineListWithInitialLabels(resetLineList: LineData[]) {
  lineList.value = resetLineList
  setOnLastValidateParameters()
}

function addNewSingleLineToList(optionalProvidedLine: any) {
  idCount.value++

  if (optionalProvidedLine === null) {
    const newOne = props.generateEmptyLine(idCount.value)
    lineList.value.push(newOne)
  } else {
    optionalProvidedLine.id = idCount.value.toString()
    lineList.value.push(optionalProvidedLine)
  }
}

function show() {
  if (props.disabled) return
  showModal.value = true
}

function hide(validate: boolean) {
  if (validate) {
    setOnLastValidateParameters()
    setOutputList()
    showModal.value = false
  } else {
    idCount.value = idCountOnLastValidate.value
    lineList.value = lineListOnLastValidate.value.map((line) =>
      copySingleLineWithAttributesForFront(line)
    )
    showModal.value = false
  }
}

function resetCriteriaListAndSave() {
  resetLineList()
  setOnLastValidateParameters()
  setOutputList()
}

function resetLineList() {
  idCount.value = 0
  const aNewElement = props.generateEmptyLine(idCount.value)
  lineList.value.splice(0, lineList.value.length, aNewElement)
}

function copySingleLineWithAttributesForFront(singleLine: LineData): LineData {
  return JSON.parse(JSON.stringify(singleLine))
}

function setOnLastValidateParameters() {
  lineListOnLastValidate.value = lineList.value.map((line) =>
    copySingleLineWithAttributesForFront(line)
  )
  idCountOnLastValidate.value = idCount.value
}

defineExpose({
  resetLineListWithInitialLabels,
  resetCriteriaListAndSave
})
</script>

<style scoped>
.criteria-border {
  border: solid 2px rgba(192, 194, 193, 0.59);
  border-radius: 5px;
  margin: 5px;
}

.add-criteria-button-container {
  display: flex;
  justify-content: right;
  vertical-align: center;
}

.linerow {
  display: flex;
  align-items: center;
}

.remove-criteria-button-container {
  width: 50px;
  margin-right: 15px;
  vertical-align: middle;
  display: inline;
}

.add-criteria-button {
  width: 50px;
  margin: 1% 1% 0 0;
  align-content: center;
  display: flex;
  justify-content: center;
}

.builder-summary {
  width: 100%;
}

.summary-box {
  min-height: 38px;
  padding: 0.375rem 0.75rem;
  cursor: pointer;
  background-color: #fff;
}

.summary-box.is-disabled {
  cursor: not-allowed;
  background-color: #e9ecef;
  opacity: 1;
}

.summary-content {
  flex: 1;
  min-width: 0;
}

.summary-actions {
  flex-shrink: 0;
}

.createButton {
  white-space: nowrap;
}

.validateHelp {
  font-size: 1.3em;
  border-radius: 50%;
  cursor: help;
}
</style>

<i18n>
en:
  ModalListBuilder:
    validate-button: Validate list
    cancel-button: Revert changes
    validate-explanation: The search will only take the current list into account if you press validate. Press cancel to see current used list.

fr:
  ModalListBuilder:
    validate-button: Validez liste
    cancel-button: Annulez modifications
    validate-explanation: La recherche ne tiendra compte de la liste actuelle que si vous appuyez sur Valider. Appuyez sur Annuler pour voir la liste actuelle qui sera prise en compte.
</i18n>