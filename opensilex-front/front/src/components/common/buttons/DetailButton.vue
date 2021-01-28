<template>
  <opensilex-Button v-if="!automaticToogle"
    @click="$emit('click', {})"
    variant="outline-success"
    :small="small"
    :label="label"
    :disabled="disabled"
  >
    <template v-slot:icon>
      <opensilex-Icon v-if="!detailVisible" icon="fa#eye" />
      <opensilex-Icon v-if="detailVisible" icon="fa#eye-slash" />
    </template>
  </opensilex-Button>
  <opensilex-Button v-else
    @click="automaticToogleClick()"
    variant="outline-success"
    :small="small"
    :label="label"
    :disabled="disabled"
  >
    <template v-slot:icon>
      <opensilex-Icon v-if="automaticSwitch" icon="fa#eye" />
      <opensilex-Icon v-if="!automaticSwitch" icon="fa#eye-slash" />
    </template>
  </opensilex-Button>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class DetailButton extends Vue {
  @Prop()
  label: string;

  @Prop()
  small: boolean;

  @Prop()
  disabled: boolean;

  @Prop()
  detailVisible: boolean;

  @Prop({default :false})
  automaticToogle: boolean;

  automaticSwitch : boolean = true;
  automaticToogleClick(){
    this.$emit('click', {});
    this.automaticSwitch = !this.automaticSwitch;
  }
}
</script>

<style scoped lang="scss">
</style>

