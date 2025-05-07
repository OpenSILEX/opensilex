<template>
  <div class="modal-overlay">
    <component :is="lang === 'fr' ? 'VariableHelpFR' : 'VariableHelpEN'" />
    affichage de la modal...
    {{ lang }}
    <div class="VariableHelpOkButton">
      <button type="button" class="btn greenThemeColor" @click="closeModal">
        {{ t('component.common.ok') }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, defineEmits } from 'vue';
import { useStore } from 'vuex';
import { useI18n } from 'vue-i18n';
import VariableHelpFR from './../doc/VariableHelpFR.md';
import VariableHelpEN from './../doc/VariableHelpEN.md';


const emit = defineEmits(['close'])

function closeModal() {
  emit('close')
}

const store = useStore();
const { t } = useI18n();

 const lang = computed(() => store.state.lang);

const components = {
  VariableHelpFR,
  VariableHelpEN
};
</script>

<style scoped lang="scss">
.VariableHelpOkButton {
  float: right;
  height: 30px;
}
</style>
