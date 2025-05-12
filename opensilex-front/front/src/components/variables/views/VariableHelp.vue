<template>
  <div class="modal-overlay">
    <div class="modal-container">
      <div class="modal-content">
        <div v-html="renderedMarkdown" class="markdown-content" />
      </div>
      <div class="modal-footer">
        <button type="button" class="btn greenThemeColor" @click="closeModal">
          {{ t('component.common.ok') }}
        </button>
      </div>
    </div>
  </div>
</template>


<script setup lang="ts">
import { computed, ref, defineEmits } from 'vue';
import { useStore } from 'vuex';
import { useI18n } from 'vue-i18n';
import { marked } from 'marked';
import VariableHelpFR from './../doc/VariableHelpFR.md?raw';
import VariableHelpEN from './../doc/VariableHelpEN.md?raw';



const emit = defineEmits(['close'])

function closeModal() {
  emit('close')
}

const store = useStore();
const { t } = useI18n();

 const lang = computed(() => store.state.lang);

 // Conversion en HTML du md
const renderedMarkdown = computed(() =>
  marked.parse(lang.value === 'fr' ? VariableHelpFR : VariableHelpEN)
);

</script>

<style scoped lang="scss">
.VariableHelpOkButton {
  float: right;
  height: 30px;
}
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
}

.modal-container {
  background-color: white;
  width: 80vw;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  overflow: hidden;
}

.modal-content {
  padding: 1rem;
  overflow-y: auto;
  flex: 1;
}

.modal-footer {
  padding: 1rem;
  border-top: 1px solid #ddd;
  text-align: right;
}

.markdown-content :deep(*) {
  max-width: 100%;
}
</style>
