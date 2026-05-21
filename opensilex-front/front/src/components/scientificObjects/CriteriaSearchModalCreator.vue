<template>
  <opensilex-ModalListBuilder
    ref="listBuilderRef"
    :fieldLabel="t('CriteriaSearchModalCreator.filter-label')"
    :helpMessage="t('CriteriaSearchModalCreator.help-message')"
    :modalTitle="t('CriteriaSearchModalCreator.title')"
    :placeholder="t('CriteriaSearchModalCreator.placeholder')"
    :modalExplanation="t('CriteriaSearchModalCreator.modal-explanation')"
    :required="required"
    :requiredBlue="requiredBlue"
    :parseSingleLineForTreeselect="parseSingleCriteriaForTreeselect"
    :generateEmptyLine="generateEmptySingleCriteria"
    :filterIncompleteLines="filterIncompleteCriteria"
    :convertLineToOutputObject="convertFrontSingleCriteriaToSingleCriteriaDto"
    lineComponent="opensilex-CriteriaSearchModalLine"
    @validateList="setOutputList"
  />
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'

interface SelectableItem {
  id: string
  label: string
}

interface SingleCriteriaDTO {
  variable_uri: string
  criteria_operator: string
  value: any
}

export interface SingleCriteriaAttributesUsedInFront {
  variable_uri: string
  criteria_operator: string
  value: any
  datatype: string
  variable_name: string
  criteria_symbol: string
  id: string
  criteria_rules: string
}

interface CriteriaDTO {
  criteria_list: Array<SingleCriteriaDTO>
}

type ModalListBuilderExposed = {
  resetLineListWithInitialLabels: (lines: SingleCriteriaAttributesUsedInFront[]) => void
  resetCriteriaListAndSave: () => void
}

defineProps<{
  disabled?: boolean
  required?: boolean
  requiredBlue?: boolean
}>()

const criteriaDto = defineModel<CriteriaDTO>('criteria_dto', {
  required: true
})

const { t } = useI18n()

const listBuilderRef = ref<ModalListBuilderExposed | null>(null)

function filterIncompleteCriteria(
  criteriaListOnLastValidate: Array<SingleCriteriaAttributesUsedInFront>
): Array<SingleCriteriaAttributesUsedInFront> {
  return criteriaListOnLastValidate.filter((singleCriteria) => {
    return (
      !!singleCriteria.criteria_operator &&
      singleCriteria.criteria_operator.trim().length > 0 &&
      (
        String(singleCriteria.value ?? '').trim().length > 0 ||
        singleCriteria.criteria_operator === 'NotMeasured'
      ) &&
      !!singleCriteria.variable_uri &&
      singleCriteria.variable_uri.trim().length > 0
    )
  })
}

function parseSingleCriteriaForTreeselect(
  singleCriteria: SingleCriteriaAttributesUsedInFront
): SelectableItem {
  const label =
    singleCriteria.variable_name +
    ' ' +
    singleCriteria.criteria_symbol +
    ' ' +
    (singleCriteria.value ? singleCriteria.value : '')

  return {
    id: singleCriteria.id,
    label
  }
}

function convertFrontSingleCriteriaToSingleCriteriaDto(
  frontSingleCriteria: SingleCriteriaAttributesUsedInFront
): SingleCriteriaDTO {
  return {
    variable_uri: frontSingleCriteria.variable_uri,
    criteria_operator: frontSingleCriteria.criteria_operator,
    value: frontSingleCriteria.value
  }
}

function generateEmptySingleCriteria(
  lineId: number
): SingleCriteriaAttributesUsedInFront {
  return {
    variable_uri: '',
    criteria_operator: '',
    value: '',
    datatype: '',
    variable_name: '',
    criteria_symbol: '',
    id: lineId.toString(),
    criteria_rules: ''
  }
}

function setOutputList(outputList: Array<SingleCriteriaDTO>) {
  criteriaDto.value = {
    ...(criteriaDto.value ?? { criteria_list: [] }),
    criteria_list: outputList.map((e) => e)
  }
}

onMounted(() => {
  listBuilderRef.value?.resetLineListWithInitialLabels([
    generateEmptySingleCriteria(0)
  ])
})

function resetCriteriaListAndSave() {
  listBuilderRef.value?.resetCriteriaListAndSave()
}

defineExpose({
  resetCriteriaListAndSave
})
</script>

<style scoped>
</style>

<i18n>
en:
  CriteriaSearchModalCreator:
    title: Criteria Creation
    placeholder: Click to create criteria list
    filter-label: Criteria on data
    help-message: Add an undefined amount of data criteria that the Scientific objects must validate to be counted in the result.
    modal-explanation: Validate all of the following conditions.

fr:
  CriteriaSearchModalCreator:
    title: Création de critère
    placeholder: Cliquez pour créer une liste de critères
    filter-label: Critères par données
    help-message: Ajouter un nombre indéfini de critères sur les données que les Objets scientifiques doivent valider pour être prise en compte dans le résultat.
    modal-explanation: Valider toutes les conditions suivantes.
</i18n>