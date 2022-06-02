<template>
  <opensilex-SelectForm
      :label="label"
      :selected.sync="resourcesURI"
      :multiple="multiple"
      :required="required"
      :itemLoadingMethod="loadResources"
      :conversionMethod="resourcesToSelectNode"
      :placeholder="placeholder"
      @clear="$emit('clear')"
      @select="select"
      @deselect="deselect"
  ></opensilex-SelectForm>
</template>

<script>
import {Prop, PropSync} from "vue-property-decorator";

export default class ResourcesSelector extends Vue {
  $opensilex: any;

  @PropSync("resources")
  resourcesURI;

  @Prop()
  label;

  @Prop({default: false})
  multiple;

  @Prop({default: false})
  required;

}
</script>

<style scoped>

</style>