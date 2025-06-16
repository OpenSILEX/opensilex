<template>
  <div
    v-if="isVisible"
    class="modal fade show d-block"
    tabindex="-1"
    role="dialog"
    style="background: rgba(0, 0, 0, 0.5);"
  >
    <div class="modal-dialog modal-lg" role="document">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title">{{ $t('component.skos.link-external') }}</h5>
          <button type="button" class="btn-close" @click="hide" />
        </div>
        <div class="modal-body">
          <opensilex-ExternalReferencesForm
            v-model:references="skosReferences"
            :includeAgroportalSearch="includeAgroportalSearch"
            :displayInsertButton="false"
            @onUpdate="update"
            @onAdd="update"
            @onDelete="update"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, defineExpose } from 'vue';
import ExternalReferencesForm from './ExternalReferencesForm.vue';

interface Props {
  references: any;
  includeAgroportalSearch?: boolean;
}

// Props
const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'onUpdate', refs: any, callback?: (res: any) => void): void;
}>();

// Internal state
const isVisible = ref(false);
const skosReferences = ref(props.references);

// Keep local copy in sync with parent
watch(
  () => props.references,
  (newVal) => {
    skosReferences.value = newVal;
  }
);

// Show/hide modal
const show = () => {
  isVisible.value = true;
};

const hide = () => {
  isVisible.value = false;
};

// Reset logic (optional - from original class version)
const currentRelation = ref('');
const currentExternalUri = ref('');

const resetForm = () => {
  currentRelation.value = '';
  currentExternalUri.value = '';
};

const resetExternalUriForm = () => {
  currentExternalUri.value = '';
  // validator reset logic if applicable
};

// Update method with optional async handling
const update = async (refs: any) => {
  return new Promise((resolve, reject) => {
    emit('onUpdate', refs, (result?: any) => {
      if (result instanceof Promise) {
        result.then(resolve).catch(reject);
      } else {
        resolve(result);
      }
    });
  });
};

// Expose methods to parent
defineExpose({
  show,
  hide,
});
</script>
