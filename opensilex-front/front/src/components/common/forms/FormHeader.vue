<template>
  <div class="flex justify-between items-center">
    <h4>
      <slot name="icon">
        <Icon :icon="icon" class="icon-title" v-if="icon" />
      </slot>
      {{ title }}
    </h4>
    <HelpButton
      v-if="tutorial"
      label="component.tutorial.name"
      class="modalFormTutorial"
      @click="onHelp"
      :small="true"
    />
  </div>
</template>

<script setup lang="ts">
import HelpButton from "@/components/common/buttons/HelpButton.vue";


const props = withDefaults(
    defineProps<{
      title: string,
      icon?: String,
      tutorial?: boolean,
      startTutorial?: Function
    }>(),
    {
      tutorial: false
    }
)



function onHelp() {
  props.startTutorial?.()
}
</script>

<style scoped lang="scss">
.modalFormTutorial {
  position: absolute;
  top: 19px;
  right: 50px;
}
</style>
