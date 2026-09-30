<template>
  <div class="form-field" :class="{ required }">
    <div class="helperAndBlueStar">
      <FormInputLabelHelper
        v-if="label"
        :label="label"
        :helpMessage="helpMessage"
        :labelFor="id"
        class="form-label"
      />
      <span v-if="requiredBlue" class="blueStar">*</span>
    </div>

    <slot name="field" :id="id"></slot>
  </div>
</template>

<script setup lang="ts">
import { ref, onBeforeMount, inject } from 'vue'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'
import FormInputLabelHelper from "@/components/common/forms/FormInputLabelHelper.vue";

const props = defineProps<{
  label?: string
  helpMessage?: string
  required?: boolean
  requiredBlue?: boolean
}>()

const $opensilex = inject<OpenSilexVuePlugin>('opensilex')
const id = ref('')

onBeforeMount(() => {
  id.value = $opensilex?.generateID?.() ?? Math.random().toString(36).slice(2)
})
</script>

<style scoped lang="scss">
.form-field {
  display: block;
  width: 100%;
}

.helperAndBlueStar {
  display: flex;
  align-items: center;
}

.blueStar {
  color: #007bff;
  margin-left: 4px;
}

:deep(label.form-label) {
  color: black;
}

.form-field.required :deep(label.form-label)::after {
  content: ' *';
  color: #dc3545;
  margin-left: .25rem;
}
</style>
