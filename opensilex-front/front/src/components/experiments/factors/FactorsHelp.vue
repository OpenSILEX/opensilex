<template>
  <div class="modal-overlay">
    <div class="modal-container">
        <div v-html="renderedMarkdown" class="markdown-content" />
      </div>
      <div class="modal-footer">
        <button type="button" class="btn greenThemeColor" @click="closeModal">
          {{ t('component.common.ok') }}
        </button>
      </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, }  from 'vue';
import { useStore } from 'vuex';
import { useI18n } from 'vue-i18n';
import { marked } from 'marked';
import FactorHelpFR from '../factors/doc/FactorHelpFR.md?raw'
import FactorHelpEN from '../factors/doc/FactorHelpEN.md?raw'

const emit = defineEmits(['close'])

function closeModal() {
  emit('close')
}

const store = useStore();
const { t } = useI18n();

const lang = computed(() => store.state.lang);

// Conversion en HTML du md
const renderedMarkdown = computed(() =>
    marked.parse(lang.value === 'fr' ? FactorHelpFR : FactorHelpEN)
);

</script>

<style scoped lang="scss">
.dataVisuHelpOkButton {
  float: right;
  height: 35px;
}
</style>