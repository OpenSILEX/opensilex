<template>
  <opensilex-Button
    @click="$emit('click')"
    :variant="variant"
    :small="small"
    :label="label"
    :disabled="disabled"
    icon="fa#question"
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

  @Prop({ default: "outline-info" })
  variant: string;
}
</script>

<style scoped lang="scss">

// .btn-outline-info{
//     border-radius: 10px;
//     padding: 10px 5px;
//     height: 10px;
// }

</style>

