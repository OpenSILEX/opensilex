<template>
  <opensilex-Button
    @click="$emit('click')"
    variant="outline-info"
    :small="small"
    :label="label"
    :disabled="disabled"
  ><template v-slot:icon>
      <opensilex-Icon v-if="!deprecated" icon="ik#ik-minus-circle" />
      <opensilex-Icon v-if="deprecated" icon="ik#ik-plus-circle" />
    </template>
  </opensilex-Button>
  
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class DeprecatedButton extends Vue {

  get label(){
    if(this.deprecated == false){
      return "DeprecatedButton.deprecated";
    } else {
      return  "DeprecatedButton.cancelDeprecated";
    }
  }

  @Prop()
  small: boolean;
  
  @Prop()
  disabled: boolean;

  @Prop()
  deprecated: boolean;

}
</script>

<style scoped lang="scss">
</style>

<i18n>

en:
  DeprecatedButton:
    deprecated: Deprecated
    cancelDeprecated: Cancel Deprecated

fr:
  DeprecatedButton:
    deprecated: Obsolète
    cancelDeprecated: Annuler Obsolète
      
</i18n>