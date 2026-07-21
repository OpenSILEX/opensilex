<template>
  <div class="flex justify-between items-center">
    <h4>
      <slot name="icon">
        <opensilex-Icon :icon="icon" class="icon-title" v-if="icon" />
      </slot>
      {{ title }}
    </h4>
    <opensilex-HelpButton
      v-if="tutorial"
      label="component.tutorial.name"
      class="modalFormTutorial"
      @click="onHelp"
      :small="true"
    />
  </div>
</template>

<script setup lang="ts">
const props = defineProps({
  title: { type: String, required: true },
  icon: String,
  tutorial: { type: Boolean, default: false },
  helpAction: Function
})

function onHelp() {
  props.helpAction?.()
}
</script>

<style scoped lang="scss">
.modalFormTutorial {
  position: absolute;
  top: 19px;
  right: 50px;
}
</style>
