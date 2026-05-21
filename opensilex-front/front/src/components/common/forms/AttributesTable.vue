<template>
  <div class="row">
    <div class="col">
      <h6 class="mb-3">
        <strong>{{ t('AttributesTable.title') }}</strong>
      </h6>

      <div class="row ms-2">
        <div class="col-md-4">
          <opensilex-AddChildButton
            class="me-2 addChildButton"
            @click="addEmptyRow"
            :label="t('AttributesTable.add')"
            :small="true"
          />
          <span>{{ t('AttributesTable.add') }}</span>
        </div>
      </div>

      <div class="row">
        <div class="col-10">
          <div ref="tableRef" class="tab"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useStore } from 'vuex'
import { useI18n } from 'vue-i18n'
import { TabulatorFull as Tabulator } from 'tabulator-tables'
import type { ColumnDefinition } from 'tabulator-tables'
import 'tabulator-tables/dist/css/tabulator.min.css'

export type Attribute = {
  attribute?: string
  value?: string
}

export function readAttributes(metadata: Record<string, string> | undefined): Attribute[] {
  const attributesArray: Attribute[] = []

  if (metadata) {
    for (const property in metadata) {
      attributesArray.push({
        attribute: property,
        value: metadata[property]
      })
    }
  }

  return attributesArray
}

export default defineComponent({
  name: 'AttributesTable',

  props: {
    editMode: {
      type: Boolean,
      required: false,
      default: false
    },
    attributesArray: {
      type: Array as () => Attribute[],
      required: false,
      default: () => []
    }
  },

  setup(props, { expose }) {
    const store = useStore()
    const { t } = useI18n()

    const tableRef = ref<HTMLElement | null>(null)
    const tabulator = ref<Tabulator | null>(null)
    const tabulatorData = ref<Attribute[]>([])

    let langUnwatcher: (() => void) | null = null

    const getTableColumns = (): ColumnDefinition[] => [
      {
        title: `${t('AttributesTable.attribute')}<span class="required">*</span>`,
        field: 'attribute',
        editor: 'input',
        validator: ['required', 'unique'],
        widthGrow: 0.5
      },
      {
        title: t('AttributesTable.value').toString(),
        field: 'value',
        editor: 'input',
        validator: ['required', 'unique'],
        widthGrow: 0.5
      },
      {
        title: t('AttributesTable.delete').toString(),
        field: 'actions',
        widthGrow: 0.3,
        formatter: () => {
          return `
            <button
              type="button"
              class="btn btn-link p-0 text-danger"
              aria-label="${t('AttributesTable.delete')}"
              title="${t('AttributesTable.delete')}"
            >
              <i class="bi bi-trash"></i>
            </button>
          `
        }
      }
    ]

    const destroyTabulator = () => {
      if (tabulator.value) {
        tabulator.value.destroy()
        tabulator.value = null
      }
    }

    const removeRow = (attribute?: string) => {
      tabulatorData.value = tabulatorData.value.filter((rowData) => {
        return rowData.attribute !== attribute
      })
    }

    const instantiateTabulator = () => {
      if (!tableRef.value) return

      destroyTabulator()

      tabulator.value = new Tabulator(tableRef.value, {
        data: tabulatorData.value,
        reactiveData: true,
        columns: getTableColumns(),
        layout: 'fitColumns',
        layoutColumnsOnNewData: true,
        index: 'attribute'
      })

      tabulator.value.on('cellClick', (_e: Event, cell: any) => {
        if (cell.getField() === 'actions') {
          removeRow(cell.getRow().getData().attribute)
        }
      })
    }

    const resetTable = () => {
      tabulatorData.value = props.attributesArray ? [...props.attributesArray] : []
      instantiateTabulator()
    }

    const addEmptyRow = () => {
      const emptyRows = tabulatorData.value.filter((rowData) => rowData.attribute === undefined)

      if (emptyRows.length === 0) {
        tabulatorData.value = tabulatorData.value.concat([
          {
            attribute: undefined,
            value: undefined
          }
        ])
      }
    }

    const pushAttributes = (): Record<string, string> => {
      const attributes: Record<string, string> = {}

      for (let y = 0; y < tabulatorData.value.length; y++) {
        const row = tabulatorData.value[y]

        if (row.attribute !== null && row.attribute !== undefined) {
          attributes[row.attribute] = row.value ?? ''
        }
      }

      return attributes
    }

    watch(
      tabulatorData,
      (value) => {
        tabulator.value?.replaceData(value)
      },
      { deep: true }
    )

    watch(
      () => props.attributesArray,
      (value) => {
        tabulatorData.value = value ? [...value] : []
        tabulator.value?.replaceData(tabulatorData.value)
      },
      { deep: true }
    )

    onMounted(async () => {
      tabulatorData.value = props.attributesArray ? [...props.attributesArray] : []

      await nextTick()
      instantiateTabulator()

      langUnwatcher = store.watch(
        () => store.getters.language,
        () => {
          instantiateTabulator()
        }
      )
    })

    onBeforeUnmount(() => {
      if (langUnwatcher) {
        langUnwatcher()
        langUnwatcher = null
      }

      destroyTabulator()
    })

    expose({
      resetTable,
      pushAttributes
    })

    return {
      t,
      tableRef,
      addEmptyRow
    }
  }
})
</script>

<style lang="scss">
.tabulator-row > .tabulator-cell {
  text-align: center;
}
</style>

<i18n>
en:
  AttributesTable:
    title: Additional attributes
    add: Add an attribute
    attribute: Attribute
    value: Value
    delete: Delete

fr:
  AttributesTable:
    title: Attributs supplémentaires
    add: Ajouter un attribut
    attribute: Attribut
    value: Valeur
    delete: Supprimer
</i18n>