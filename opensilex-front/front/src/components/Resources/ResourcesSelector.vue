<template>
  <opensilex-SelectForm
      ref="selectForm"
      :label="label"
      :selected.sync="resourcesURI"
      :multiple="multiple"
      :required="required"
      :optionsLoadingMethod="loadResources"
      :conversionMethod="resourcesToSelectNode"
      :placeholder="placeholder"
      @clear="$emit('clear')"
      @select="select"
      @deselect="deselect"
  ></opensilex-SelectForm>
</template>

<script>
export default {
  name: "ResourcesSelector"
}
</script>

<style scoped>

</style>