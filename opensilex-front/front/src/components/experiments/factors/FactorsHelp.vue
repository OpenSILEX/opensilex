<template>
  <div>
    <div v-if="lang === 'fr'">
      <FactorHelpFR />
    </div>

    <div v-else>
      <FactorHelpEN />
    </div>

    <button
        type="button"
        class="btn greenThemeColor dataVisuHelpOkButton"
        @click="hideBtnClick"
    >
      {{ $t('component.common.ok') }}
    </button>
  </div>
</template>

<script setup lang="ts">
import {computed} from 'vue'
import { useStore } from 'vuex'
import FactorHelpFR from '../factors/doc/FactorHelpFR.md'
import FactorHelpEN from '../factors/doc/FactorHelpEN.md'

const store = useStore()

const lang = computed(() => store.getters.language)

const emit = defineEmits(['hideBtnIsClicked'])


function hideBtnClick() {
  emit('hideBtnIsClicked')
}
console.log('FactorHelpFR:', FactorHelpFR)
console.log('FactorHelpEN:', FactorHelpEN)
</script>

<style scoped lang="scss">
.dataVisuHelpOkButton {
  float: right;
  height: 35px;
}
</style>