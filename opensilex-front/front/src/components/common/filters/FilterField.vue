<template>
  <div class = "col col-12" v-bind:class="{'col-xl-3 col-sm-6' :baseWidth,'col-xl-12 col-sm-12': fullWidth,'col-xl-6 col-sm-6':halfWidth,'col-xl-3 col-sm-3':quarterWidth}" >
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
import {
  Component,
  Prop
} from "vue-property-decorator";
import Vue, {computed} from "vue";


const props = withDefaults(defineProps<{
 label: string
  fullWidth: boolean
  halWidth: boolean
  quarterWidth: boolean
}>(), {
  label: "component.search.lab",
  fullWidth: false,
  halWidth: false,
  quarterWidth: false
})

  const baseWidth = computed(() =>
  {
    return !props.fullWidth && !props.halWidth && !props.quarterWidth
  })

</script>


<style scoped lang="scss">
</style>

