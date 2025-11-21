<template>
  <div
    class="result container-fluid"
    :class="{ selectedResult: isSelected }"
    @click="emitClicked"
    role="button"
    tabindex="0"
  >
    <!-- En-tête -->
    <div class="row mx-0">
      <div class="col-12">
        <div id="result-name">
          {{ entity.name }}
          -
          <span id="result-ontology">{{ entity.ontologyName }}</span>
        </div>
        <div id="result-link">
          <a :href="entity.id" target="_blank" rel="noopener noreferrer">{{ entity.id }}</a>
        </div>
      </div>
    </div>

    <!-- Définition -->
    <div class="row mx-0">
      <div class="col-12" id="result-definition">
        {{ entity.definitions?.[0] }}
      </div>
    </div>

    <!-- Bouton de validation (slot) -->
    <div class="row justify-content-end" v-if="isSelected">
      <div class="col-auto">
        <slot name="validationButton"></slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, defineProps, defineEmits, defineExpose } from 'vue'

type AgroportalTermDTO = {
  id: string
  name: string
  ontologyName?: string
  definitions?: string[]
}

const props = defineProps<{
  entity: AgroportalTermDTO
}>()

const emit = defineEmits<{
  (e: 'item-clicked'): void
}>()

const isSelected = ref(false)

function setSelected(selected: boolean) {
  isSelected.value = selected
}
function emitClicked() {
  emit('item-clicked')
}

// Expose au parent (utilisé par AgroportalResults.vue)
defineExpose({ setSelected })
</script>

<style scoped>
#result-name {
  font-weight: bold;
  font-size: large;
  margin-bottom: 5px;
}

#result-ontology {
  font-weight: normal;
  font-size: medium;
}

a {
  color: #00A38D;
}

.result {
  font-size: medium;
  margin-bottom: 10px;
  padding: 10px;
  margin-right: 1px;
  transition: background 0.15s ease;
}

.result:hover {
  background: rgba(0, 0, 0, 0.1);
}

/* style quand sélectionné */
.selectedResult {
  outline: 2px solid rgba(0, 163, 141, 0.5);
  border-radius: 4px;
}
</style>
