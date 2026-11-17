<template>
  <div class="graph-import-understanding mb-4">
    <div class="d-flex align-items-center gap-2 mb-2">
      <h5 class="mb-0">{{ sheet.name }}</h5>
      <span class="text-body-secondary small">{{ $t('GraphImport.understand.rows', { count: sheet.row_count ?? 0 }) }}</span>
      <n-tag v-if="sheet.declared" size="small" type="info" :bordered="false">{{ $t('GraphImport.declared.tag') }}</n-tag>
      <n-button v-if="hasDecisions && !sheet.declared" size="tiny" tertiary @click="emit('forget')">
        {{ $t('GraphImport.understand.forget') }}
      </n-button>
    </div>
    <p v-for="(note, index) in sheet.notes ?? []" :key="'note' + index" class="small text-warning-emphasis mb-1">
      {{ reportText($opensilex, note) }}
    </p>

    <div v-if="sheet.declared" class="graph-import-understanding__declared">
      <div v-for="(id, index) in sheet.declared_ids ?? []" :key="id" class="d-flex align-items-center gap-2 mb-1">
        <span class="text-body-secondary small">{{ index + 1 }}</span>
        <span>{{ declaredName(declaredById(id)) }}</span>
        <n-button size="tiny" tertiary :disabled="busy" @click="emit('editDeclared', id)">{{ $t('GraphImport.declared.edit') }}</n-button>
      </div>
    </div>

    <template v-else-if="sheet.columns?.length">
      <SheetChoices class="mb-2" :sheet="sheet" :classes="classes" @decide="(decision) => emit('decide', decision)" />
      <MissingValues
        class="mb-2"
        :sheet="sheet"
        :validation="validation"
        :classes="classes"
        :busy="busy"
        @decide="(decision) => emit('decide', decision)"
        @declare="(classUri, name) => emit('declare', classUri, name)"
      />

      <n-collapse class="mb-2">
        <n-collapse-item :title="$t('GraphImport.understand.why')" name="why">
          <ReasonList :choice="sheet.sheet_class" :title="$t('GraphImport.understand.class')" />
          <ReasonList :choice="sheet.identifier" :title="$t('GraphImport.understand.identifier')" />
        </n-collapse-item>
        <n-collapse-item v-if="rowCheck.counts.length" :title="rowProblemsTitle" name="rows">
          <ProblemList :check="rowCheck" :headers="headers" />
        </n-collapse-item>
      </n-collapse>

      <n-data-table
        size="small"
        :columns="tableColumns"
        :data="sheet.columns"
        :row-key="rowKey"
        :bordered="true"
        :single-line="false"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, h, inject } from 'vue'
import type { DataTableColumns } from 'naive-ui'
import type {
  ClassOptionDTO,
  ColumnInterpretationDTO,
  DeclaredResourceDTO,
  SheetInterpretationDTO,
  ValidationDTO
} from '../lib'
import type { OpenSilexPlugin } from '../opensilexPlugin'
import { columnTitle, declaredName } from '../declared'
import {
  hasDecisions as sheetHasDecisions,
  isDataSheet,
  leftOut,
  type SheetDecision,
  withColumn
} from '../decisions'
import { type DateReading, ofColumn, type SheetCheck, tally } from '../problems'
import { reportText } from '../reasons'
import ChoiceStatusTag from './ChoiceStatusTag.vue'
import ColumnEvidence from './ColumnEvidence.vue'
import DateReadingForm from './DateReadingForm.vue'
import MeasureVariable from './MeasureVariable.vue'
import MissingValues from './MissingValues.vue'
import ProblemList from './ProblemList.vue'
import ProblemTally from './ProblemTally.vue'
import ReasonList from './ReasonList.vue'
import SheetChoices from './SheetChoices.vue'

//#region Public
const emit = defineEmits<{
  /** The whole decision about the sheet, which replaces the previous one. */
  decide: [decision: SheetDecision]
  /** Give the whole sheet back to the suggestions. */
  forget: []
  /** How the dates of a column are written. */
  read: [column: number, reading: DateReading]
  /** Declare a resource of this class under this name, since nothing of that name exists. */
  declare: [classUri: string, name: string]
  /** Open a declared resource of the sheet to change it. */
  editDeclared: [id: string]
}>()

const props = defineProps<{
  sheet: SheetInterpretationDTO
  /** The classes a sheet may hold, as trees. */
  classes: ClassOptionDTO[]
  /** What the check of the graph says about the sheet. */
  check: SheetCheck
  /** The whole check, which tells where the links of a given value land. */
  validation?: ValidationDTO
  /** The resources the user declared in the assistant. */
  declared: DeclaredResourceDTO[]
  /** Whether a request is running. */
  busy: boolean
}>()
//#endregion

//#region Private
//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
//#endregion

//#region Data & Computed
const headers = computed<string[]>(() => (props.sheet.columns ?? []).map((column) => column.header ?? ''))

/** The problems about whole rows, such as a required value no sheet gives. */
const rowCheck = computed<SheetCheck>(() => ofColumn(props.check, undefined))

const rowProblemsTitle = computed(() => {
  const counted = tally(rowCheck.value.counts)
  return $opensilex.$i18n.t('GraphImport.check.rowProblems', { count: counted.errors + counted.warnings })
})

const hasDecisions = computed(() => sheetHasDecisions(props.sheet))

const tableColumns = computed<DataTableColumns<ColumnInterpretationDTO>>(() => [
  {
    type: 'expand',
    renderExpand: (column) => {
      const check = ofColumn(props.check, column.index)
      return h('div', {}, [
        isDataSheet(props.sheet) && !column.type_column && !column.identifier
          ? h(MeasureVariable, {
              sheet: props.sheet,
              column,
              busy: props.busy,
              onDecide: (decision: SheetDecision) => emit('decide', decision)
            })
          : null,
        h(ColumnEvidence, {
          column,
          properties: props.sheet.properties ?? [],
          onChoose: (property: string | undefined) => onColumnEvidenceChoose(column, property),
          onLeaveOut: () => onColumnEvidenceLeaveOut(column)
        }),
        check.dateColumns.length
          ? h(DateReadingForm, {
              dateColumn: check.dateColumns[0],
              onRead: (reading: DateReading) => emit('read', column.index!, reading)
            })
          : null,
        check.counts.length ? h(ProblemList, { check, headers: headers.value }) : null
      ])
    }
  },
  {
    key: 'header',
    title: $opensilex.$i18n.t('GraphImport.understand.column'),
    render: (column) =>
      h('span', {}, [
        h('span', { class: 'text-body-secondary me-1' }, String((column.index ?? 0) + 1)),
        columnTitle(column, props.sheet),
        column.given ? h('span', { class: 'ms-1 small text-info' }, $opensilex.$i18n.t('GraphImport.missing.givenTag')) : null,
        column.identifier
          ? h('span', { class: 'ms-1 small text-info' }, $opensilex.$i18n.t('GraphImport.understand.identifierTag'))
          : null
      ])
  },
  {
    key: 'status',
    title: $opensilex.$i18n.t('GraphImport.understand.status'),
    width: 130,
    render: (column) => h(ChoiceStatusTag, { status: column.type_column ? 'DECIDED' : column.property?.status })
  },
  {
    key: 'property',
    title: $opensilex.$i18n.t('GraphImport.understand.property'),
    render: (column) => propertyLabel(column)
  },
  {
    key: 'problems',
    title: $opensilex.$i18n.t('GraphImport.check.problemsColumn'),
    width: 140,
    render: (column) => h(ProblemTally, { tally: tally(ofColumn(props.check, column.index).counts) })
  }
])
//#endregion

//#region Event handlers
function onColumnEvidenceChoose(column: ColumnInterpretationDTO, property: string | undefined) {
  emit('decide', withColumn(props.sheet, column.index!, property))
}

function onColumnEvidenceLeaveOut(column: ColumnInterpretationDTO) {
  emit('decide', leftOut(props.sheet, column.index!))
}
//#endregion

//#region Methods
/** The property the column holds, by its label, or what is still to do. */
function propertyLabel(column: ColumnInterpretationDTO): string {
  if (column.type_column) {
    return $opensilex.$i18n.t('GraphImport.understand.typeColumn')
  }
  if (column.property?.status === 'LEFT_OUT') {
    return $opensilex.$i18n.t('GraphImport.understand.leftOut')
  }
  if (column.variable) {
    return $opensilex.$i18n.t('GraphImport.measure.of', { variable: column.variable_label ?? column.variable })
  }
  const uri = column.property?.chosen_uri
  if (!uri) {
    return $opensilex.$i18n.t('GraphImport.understand.toDecide')
  }
  const known = (props.sheet.properties ?? []).find((property) => property.uri === uri)
  return known?.label ?? uri
}

function declaredById(id: string): DeclaredResourceDTO | undefined {
  return props.declared.find((resource) => resource.id === id)
}

function rowKey(column: ColumnInterpretationDTO): number {
  return column.index ?? 0
}
//#endregion
//#endregion
</script>

