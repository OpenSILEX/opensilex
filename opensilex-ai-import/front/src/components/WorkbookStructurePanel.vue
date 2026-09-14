<template>
  <div class="card">
    <div class="card-header d-flex align-items-center justify-content-between">
      <span class="fw-semibold">{{ $t('AiImport.structure.title') }}</span>
      <span class="text-body-secondary small">
        {{ structure.file_name }} — {{ structure.sheets.length }} {{ $t('AiImport.structure.sheets') }}
      </span>
    </div>

    <div class="card-body">
      <ul class="nav nav-pills flex-wrap gap-1 mb-3">
        <li v-for="sheet in structure.sheets" :key="sheet.name" class="nav-item">
          <button
            type="button"
            class="nav-link py-1 px-2 small"
            :class="{ active: sheet.name === selected }"
            @click="selected = sheet.name"
          >
            {{ sheet.name }}
          </button>
        </li>
      </ul>

      <div v-if="current">
        <p v-if="!current.is_tabular" class="mb-2">
          <span class="badge text-bg-secondary">{{ $t('AiImport.structure.textSheet') }}</span>
        </p>
        <pre v-if="!current.is_tabular" class="ai-import-structure__text">{{ current.text }}</pre>

        <template v-else>
          <p class="text-body-secondary small mb-2">
            {{ current.data_row_count }} {{ $t('AiImport.structure.rows') }} ·
            {{ current.headers.length }} {{ $t('AiImport.structure.columns') }}
          </p>

          <p v-if="!current.sample_rows || !current.sample_rows.length" class="mb-0">
            {{ $t('AiImport.structure.noSample') }}
          </p>

          <div v-else class="table-responsive">
            <table class="table table-sm table-bordered align-middle small mb-0">
              <thead>
                <tr>
                  <th v-for="(header, index) in current.headers" :key="index" class="text-nowrap">
                    {{ header }}
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, rowIndex) in current.sample_rows" :key="rowIndex">
                  <td v-for="(header, columnIndex) in current.headers" :key="columnIndex">
                    {{ row[columnIndex] }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

interface SheetStructure {
  name: string
  is_tabular: boolean
  headers?: string[]
  data_row_count?: number
  sample_rows?: string[][]
  text?: string
}

const props = defineProps<{
  structure: { file_name: string; sheets: SheetStructure[] }
}>()

const selected = ref<string | null>(null)

const current = computed<SheetStructure | undefined>(() =>
  props.structure.sheets.find((sheet) => sheet.name === selected.value)
)

// Open on the first tabular sheet: a ReadMe is the first sheet of these templates, and the data is
// what the user came to look at.
watch(
  () => props.structure,
  (structure) => {
    const preferred = structure.sheets.find((sheet) => sheet.is_tabular) ?? structure.sheets[0]
    selected.value = preferred ? preferred.name : null
  },
  { immediate: true }
)
</script>

<style scoped>
.ai-import-structure__text {
  max-height: 20rem;
  overflow-y: auto;
  white-space: pre-wrap;
  font-size: 0.8rem;
  padding: 0.5rem;
  border-radius: 0.25rem;
  background-color: rgba(127, 127, 127, 0.08);
  margin-bottom: 0;
}
</style>
