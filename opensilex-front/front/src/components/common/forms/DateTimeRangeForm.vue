<template>
  <div>
    <div class="row">
      <div class="col" v-if="canBeInstant">
        <!-- Is instant -->
        <opensilex-FormField
          :required="false"
          label="component.events.is-instant"
          helpMessage="component.events.is-instant-help"
        >
          <template #field>
            <!-- Bootstrap switch -->
            <div class="form-check form-switch">
              <input
                class="form-check-input"
                type="checkbox"
                role="switch"
                v-model="isInstantLocal"
                @change="updateIsInstantFilter"
              />
            </div>
          </template>
        </opensilex-FormField>
      </div>
    </div>

    <div class="row">
      <div class="col" v-if="!isInstantLocal || !canBeInstant">
        <!-- start date -->
        <opensilex-DateTimeForm
          ref="startDateSelector"
          v-model:value="startLocal"
          label="component.events.start"
          :maxDate="endLocal"
          :required="startRequiredLocal"
          helpMessage="component.events.start-help"
        />
      </div>

      <div class="col">
        <!-- end date -->
        <opensilex-DateTimeForm
          ref="endDateSelector"
          v-model:value="endLocal"
          label="component.events.end"
          :minDate="startLocal"
          :required="endRequiredLocal"
          helpMessage="component.events.end-help"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'

const emit = defineEmits<{
  (e: 'update:startDate', v: string | undefined): void
  (e: 'update:endDate', v: string | undefined): void
  (e: 'update:isInstant', v: boolean): void
  (e: 'update:start_required', v: boolean): void
  (e: 'update:end_required', v: boolean): void
  (e: 'change'): void
}>()

const props = withDefaults(
  defineProps<{
    startDate?: string
    endDate?: string
    isInstant?: boolean
    start_required?: boolean
    end_required?: boolean
    canBeInstant?: boolean
    enforceInternalRequiredRules?: boolean
  }>(),
  {
    canBeInstant: true,
    isInstant: false,
    start_required: false,
    end_required: false,
    enforceInternalRequiredRules: true
  }
)

// Local state
const startLocal = ref<string | undefined>(props.startDate)
const endLocal = ref<string | undefined>(props.endDate)
const isInstantLocal = ref<boolean>(!!props.isInstant)
const startRequiredLocal = ref<boolean>(!!props.start_required)
const endRequiredLocal = ref<boolean>(!!props.end_required)

// keep local in sync if parent updates
watch(() => props.startDate, (v) => (startLocal.value = v))
watch(() => props.endDate, (v) => (endLocal.value = v))
watch(() => props.isInstant, (v) => (isInstantLocal.value = !!v))
watch(() => props.start_required, (v) => {
  startRequiredLocal.value = !!v
})
watch(() => props.end_required, (v) => {
  endRequiredLocal.value = !!v
})

// emit changes to parent
watch(startLocal, (v) => emit('update:startDate', v))
watch(endLocal, (v) => emit('update:endDate', v))
watch(isInstantLocal, (v) => emit('update:isInstant', v))
watch(startRequiredLocal, (v) => emit('update:start_required', v))
watch(endRequiredLocal, (v) => emit('update:end_required', v))

onMounted(() => {
  if (!props.canBeInstant && props.enforceInternalRequiredRules) {
    startRequiredLocal.value = true
    emit('update:start_required', true)
  }
  updateRequiredProps()
})

function updateIsInstantFilter() {
  emit('change')
  updateRequiredProps()
}

function normalizeEmpty(v: string | undefined): string | undefined {
  if (v === '') return undefined
  return v
}

function updateRequiredProps() {
  startLocal.value = normalizeEmpty(startLocal.value)
  endLocal.value = normalizeEmpty(endLocal.value)

  // Mode piloté par le parent
  if (!props.enforceInternalRequiredRules) {
    startRequiredLocal.value = !!props.start_required
    endRequiredLocal.value = !!props.end_required

    // Si instantané, on masque simplement le début
    if (isInstantLocal.value) {
      startLocal.value = undefined
    }
    return
  }

  // Mode normal
  if (isInstantLocal.value) {
    endRequiredLocal.value = true
    startLocal.value = undefined
  } else {
    const startEmpty = startLocal.value == null
    const endEmpty = endLocal.value == null

    if (startEmpty && endEmpty) {
      startRequiredLocal.value = true
      endRequiredLocal.value = true
    } else {
      startRequiredLocal.value = !!startLocal.value
      endRequiredLocal.value = !!endLocal.value
    }
  }
}
</script>

<style scoped lang="scss"></style>