<template>
  <div class="row g-2">
    <div class="col-md-5">
        <opensilex-VariableSelector
        :label="t('CriteriaSearchModalLine.variable')"
        :variables="lineData.variable_uri"
        :multiple="false"
        :required="false"
        class="col-md-5 criteriaFields"
        @update:variables="updateField('variable_uri', $event)"
        @select="loadVariableInformation"
        />
    </div>

    <div class="col-md-2">
        <opensilex-CriteriaOperatorSelector
        :operator="lineData.criteria_operator"
        :rules="lineData.criteria_rules"
        :required="false"
        class="col-md-2"
        @update:operator="updateField('criteria_operator', $event)"
        @select="loadCriteriaInformation"
        />
    </div>

    <div class="col-md-5">
      <opensilex-InputForm
        v-if="$opensilex.compareUris(lineData.datatype, Xsd.INTEGER)"
        :label="t('CriteriaSearchModalLine.value')"
        :value="lineData.value"
        type="number"
        rules="integer"
        :disabled="criteriaOperatorIsNotMeasured()"
        :required="false"
        :placeholder="t('CriteriaSearchModalLine.XSDIntegerInput.placeholder')"
        @update:value="updateField('value', $event)"
      />

      <opensilex-InputForm
        v-else-if="$opensilex.compareUris(lineData.datatype, Xsd.DECIMAL)"
        :label="t('CriteriaSearchModalLine.value')"
        :value="lineData.value"
        type="number"
        rules="decimal"
        :disabled="criteriaOperatorIsNotMeasured()"
        :required="false"
        :placeholder="t('CriteriaSearchModalLine.XSDDecimalInput.placeholder')"
        @update:value="updateField('value', $event)"
      />

      <opensilex-DateForm
        v-else-if="$opensilex.compareUris(lineData.datatype, Xsd.DATE)"
        :label="t('CriteriaSearchModalLine.value')"
        :value="lineData.value"
        :required="false"
        :disabled="criteriaOperatorIsNotMeasured()"
        class="searchFilter"
        @update:value="updateField('value', $event)"
      />

      <opensilex-DateTimeForm
        v-else-if="$opensilex.compareUris(lineData.datatype, Xsd.DATETIME)"
        :label="t('CriteriaSearchModalLine.value')"
        :value="lineData.value"
        :disabled="criteriaOperatorIsNotMeasured()"
        :required="false"
        @update:value="updateField('value', $event)"
      />

      <opensilex-FormSelector
        v-else-if="$opensilex.compareUris(lineData.datatype, Xsd.BOOLEAN)"
        :label="t('CriteriaSearchModalLine.value')"
        :selected="lineData.value"
        :multiple="false"
        :placeholder="t('CriteriaSearchModalLine.boolean-field-placeholder')"
        :showCount="false"
        :required="false"
        :disabled="criteriaOperatorIsNotMeasured()"
        :options="trueFalseList"
        @update:selected="updateField('value', $event)"
      />

      <opensilex-InputForm
        v-else
        :label="t('CriteriaSearchModalLine.value')"
        type="string"
        :disabled="true"
        :required="false"
        :placeholder="t('CriteriaSearchModalLine.value-placeholder')"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { inject, ref } from 'vue'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import Xsd from '../../ontologies/Xsd'
import Oeso from '../../ontologies/Oeso'
import type HttpResponse from 'opensilex-security/HttpResponse'
import type { OpenSilexResponse } from 'opensilex-security/HttpResponse'
import type { VariableDetailsDTO } from 'opensilex-core/model/variableDetailsDTO'
import type { VariablesService } from 'opensilex-core/api/variables.service'
import type { SingleCriteriaAttributesUsedInFront } from './CriteriaSearchModalCreator.vue'
import { useI18n } from 'vue-i18n'

interface SelectableItem {
  id: string
  label: string
}

const props = defineProps<{
  lineData: SingleCriteriaAttributesUsedInFront
  lineIndex: number
  extraProps?: any
}>()

const emit = defineEmits<{
  (e: 'updateLine', fieldsToChange: Record<string, any>, lineIndex: number): void
}>()

const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!
const { t } = useI18n()
const variableService = $opensilex.getService<VariablesService>('opensilex.VariablesService')

const savedVariablesDatatypes = ref<Map<string, string>>(new Map())

const trueFalseList: SelectableItem[] = [
  { id: 'true', label: 'true' },
  { id: 'false', label: 'false' }
]

function criteriaOperatorIsNotMeasured(): boolean {
  return props.lineData.criteria_operator
    ? props.lineData.criteria_operator === 'NotMeasured'
    : false
}

function updateField(field: string, value: any) {
  emit('updateLine', { [field]: value }, props.lineIndex)
}

function loadCriteriaInformation(criteriaIdAndLabelJson: SelectableItem) {
  emit(
    'updateLine',
    {
      criteria_symbol: criteriaIdAndLabelJson.label
    },
    props.lineIndex
  )
}

async function loadVariableInformation(variableIdAndLabelJson: SelectableItem) {
  const lineFieldsToChange: Record<string, any> = {}
  const variableUri = variableIdAndLabelJson.id

  lineFieldsToChange.variable_name = variableIdAndLabelJson.label

  const alreadyFetchedType = savedVariablesDatatypes.value.get(variableUri)

  if (!alreadyFetchedType) {
    const response: HttpResponse<OpenSilexResponse<VariableDetailsDTO>> =
      await variableService.getVariable(variableUri)

    lineFieldsToChange.datatype = response.response.result.datatype
    savedVariablesDatatypes.value.set(variableUri, lineFieldsToChange.datatype)
  } else {
    lineFieldsToChange.datatype = alreadyFetchedType
  }

  lineFieldsToChange.criteria_rules = ''

  if ($opensilex.compareUris(lineFieldsToChange.datatype, Xsd.BOOLEAN)) {
    lineFieldsToChange.criteria_rules =
      'refuseOperators:vocabulary:LessThan,vocabulary:MoreThan,vocabulary:MoreOrEqualThan,vocabulary:LessOrEqualThan'
  }

  emit('updateLine', lineFieldsToChange, props.lineIndex)
}
</script>

<style scoped>
.label-notinline {
  display: block;
}

.criteriaFields {
    display: flex;
}
</style>

<i18n>
en:
  CriteriaSearchModalLine:
    variable: Variable
    value: Value
    value-placeholder: Enter value
    boolean-field-placeholder: Enter true or false
    XSDDecimalInput:
        placeholder: "Enter a decimal number, ex : 8611.53"
    XSDIntegerInput:
        placeholder: "Enter an integer number, ex : 8611"
    XSDLongStringInput:
        placeholder: "Enter a long text, ex : OpenSILEX is an ontology-driven Information System designed for life science data."
    XSDStringInput:
        placeholder: "Enter text, ex : Opensilex"
    XSDUriInput:
        placeholder: "Enter an URI, ex : http://www.opensilex.org/"


fr:
  CriteriaSearchModalLine:
    variable: Variable
    value: Valeur
    value-placeholder: Entrez valeur
    boolean-field-placeholder: Entrez vrai ou faux
    XSDDecimalInput:
        placeholder: "Saisir un nombre décimal, ex : 8611.53"
    XSDIntegerInput:
        placeholder: "Saisir un nombre entier, ex : 8611"
    XSDLongStringInput:
        placeholder: "Saisissez un texte long, ex : OpenSILEX est un système d'information guidé par des ontologies, conçu pour les données issues des sciences de la vie "
    XSDStringInput:
        placeholder: "Saisir du texte, ex : Opensilex"
    XSDUriInput:
        placeholder: "Saisir un URI, ex : http://www.opensilex.org/"
</i18n>