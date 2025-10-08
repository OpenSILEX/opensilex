<template>
  <div class="form-field" :class="{ required }">
    <div class="helperAndBlueStar">
      <opensilex-FormInputLabelHelper
        v-if="label"
        :label="label"
        :helpMessage="helpMessage"
        :labelFor="id"
        class="form-label"
      />
      <span v-if="requiredBlue" class="blueStar">*</span>
    </div>

    <!-- Le champ est rendu par le slot.
         Les composants enfants (ou NFormItem) gèrent la validation/feedback. -->
    <slot name="field" :id="id"></slot>
  </div>
</template>

<script setup lang="ts">
import { ref, onBeforeMount, inject } from 'vue'
import type OpenSilexVuePlugin from '@/models/OpenSilexVuePlugin'

const props = defineProps<{
  label?: string
  helpMessage?: string
  /** Affiche l’astérisque rouge via CSS, ne déclenche pas de validation ici */
  required?: boolean
  /** Étoile bleue (comportement existant) */
  requiredBlue?: boolean
  /** Conservé pour compat, non utilisé ici (validation déplacée dans les enfants / NFormItem) */
  rules?: string | (() => string)
  vid?: string
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
  color: #007bff; /* bootstrap primary */
  margin-left: 4px;
}

/* On ne colore que l’astérisque */
:deep(label.form-label) {
  color: black;
}

/* Asterisk rouge quand prop `required` = true */
.form-field.required :deep(label.form-label)::after {
  content: ' *';
  color: #dc3545; /* bootstrap danger */
  margin-left: .25rem;
}
</style>
