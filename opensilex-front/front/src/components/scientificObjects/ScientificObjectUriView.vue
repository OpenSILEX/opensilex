<template>
  <span class="static-field-line">
    <opensilex-UriLink
      v-if="label"
      :to="{
        path:
          '/scientific-objects/details/' + encodeURIComponent(value),
      }"
      :uri="value"
      :value="label"
    ></opensilex-UriLink>
  </span>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  Model,
  Provide,
  PropSync,
  Watch,
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ScientificObjectUriView extends Vue {
  $opensilex: any;

  @Prop()
  value;

  label = "";

  mounted() {
    this.onValueChange();
  }

  @Watch("value")
  onValueChange() {
    if (this.value) {
      this.$opensilex
        .getService("opensilex.OntologyService")
        .getURILabel(this.value)
        .then((http) => {
          this.label = http.response.result;
        })
        .catch(() => {
          this.label = this.value;
        });
    }
  }
}
</script>

<style scoped lang="scss">
</style>

