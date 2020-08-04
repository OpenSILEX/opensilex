<template>
  <opensilex-Button
    @click="$emit('click')"
    variant="outline-info"
    :small="small"
    :label="label"
    :disabled="disabled"
    icon="fa#question"
    class="helpButton"
  ></opensilex-Button>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class HelpButton extends Vue {
  @Prop()
  label: string;

  @Prop()
  small: boolean;

  @Prop()
  disabled: boolean;
}
</script>

<style scoped lang="scss">
.helpButton {
  padding: 0px 0px;
}
</style>

