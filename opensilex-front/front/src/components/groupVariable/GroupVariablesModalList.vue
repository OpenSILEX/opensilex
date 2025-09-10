<template>
  <div>
    <!-- MODAL -->
    <div
      class="modal fade"
      :class="{ show: isVisible }"
      style="display: block;"
      tabindex="-1"
      role="dialog"
      v-if="isVisible"
    >
      <div class="modal-dialog modal-xl" role="document">
        <div class="modal-content">

          <!-- Modal Body -->
          <div class="modal-body">
            <div class="card">
              <opensilex-GroupVariablesList
                v-if="loadList"
                ref="groupVariableSelection"
                :maximumSelectedRows="maximumSelectedRows"
                iconNumberOfSelectedRow="fa#fa-flask-vial"
              />
            </div>
          </div>

          <!-- Modal Footer -->
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="hide(false)">
              {{ $t('component.common.close') }}
            </button>
            <button type="button" class="btn greenThemeColor" @click="hide(true)">
              {{ $t('component.common.validateSelection') }}
            </button>
          </div>

        </div>
      </div>
    </div>

    <!-- Backdrop -->
    <div v-if="isVisible" class="modal-backdrop fade show"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, defineExpose, nextTick } from 'vue';

const emit = defineEmits<{
  (e: 'onValidate', value: any): void
  (e: 'onValidateEmpty'): void
}>()

const isVisible = ref(false);
const loadList = ref(false);

const groupVariableSelection = ref();

function show() {
  loadList.value = true;
  nextTick(() => {
    isVisible.value = true;
  });
}

function hide(validate: boolean) {
  if (!validate) {
    // Cas bouton "Fermer" -> fermer directement
    isVisible.value = false;
    return;
  }
  // Cas bouton "Valider" -> passe par validateAndSubmit
  validateAndSubmit();
}

function validateAndSubmit() {
  const selected = groupVariableSelection.value?.getSelected?.() ?? [];
  if (!selected.length) {
    // Pas de sélection -> ne pas fermer, juste prévenir le parent
    emit('onValidateEmpty');
    return;
  }
  emit('onValidate', selected);
  isVisible.value = false;
}

// Appelés depuis l’extérieur
function selectItem(row: any) {
  groupVariableSelection.value?.onItemSelected(row);
}

function unSelect(row: any) {
  groupVariableSelection.value?.onItemUnselected(row);
}

// Expose API pour le parent
defineExpose({
  show,
  hide,
  selectItem,
  unSelect,
});
</script>


<style scoped>
.modal-xl {
  max-width: 1140px;
}
</style>
