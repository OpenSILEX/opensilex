<template>
  <div class="form-field">
    <div class="helperAndBlueStar">
      <opensilex-FormInputLabelHelper
        v-if="label"
        :label="label"
        :helpMessage="helpMessage"
        :labelFor="id"
      />
      <span v-if="requiredBlue" class="blueStar">*</span>
    </div>

    <div v-if="required || rules">
      <div :class="{ errors: hasErrors }">
        <slot name="field" :id="id" :validator="validateField"></slot>
      </div>
      <div v-if="firstError" class="error-message alert alert-danger">{{ firstError }}</div>
    </div>
    <template v-else>
      <slot name="field" :id="id"></slot>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, getCurrentInstance, onBeforeMount, inject } from 'vue'
import OpenSilexVuePlugin from "@/models/OpenSilexVuePlugin"


// Props
const props = defineProps<{
  label?: string
  helpMessage?: string
  required?: boolean
  requiredBlue?: boolean
  rules?: string | (() => string)
  vid?: string
}>()

const $opensilex = inject<OpenSilexVuePlugin>('opensilex');

// ID generated via opensilexVuePlugin method
const id = ref<string>('')

onBeforeMount(() => {
  if ($opensilex && typeof $opensilex.generateID === 'function') {
    id.value = $opensilex.generateID()
  } else {
    console.error('opensilex.generateID() is not available')
  }
})

// Validation logic
const errors = ref<string[]>([])

const getRules = () => {
  let rules = ''
  if (props.rules) {
    const rulesValue = typeof props.rules === 'function' ? props.rules() : props.rules
    rules = props.required ? `required|${rulesValue}` : `${rulesValue}`
  } else if (props.required) {
    rules = 'required'
  }
  return rules
}

const validateField = () => {
  errors.value = []
  const inputElement = document.getElementById(id.value) as HTMLInputElement
  if (props.required && inputElement && !inputElement.value) {
    errors.value.push('This field is required')
  }
}

</script>


<style scoped lang="scss">
.helperAndBlueStar {
  display: flex;
  align-items: center;
}
.blueStar {
  color: #007bff;
  margin-left: 4px;
}
.error-message {
  margin-top: 0.5rem;
}
</style>
