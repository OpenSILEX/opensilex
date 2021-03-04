<template>
  <span class="static-field-line">
    <opensilex-UriLink
      v-if="label"
      :uri="value"
      :value="label"
      :to="{path: '/germplasm/details/'+ encodeURIComponent(value)}"
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
  Watch
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class GermplasmPropertyView extends Vue {
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
      this.$opensilex.disableLoader();
      this.$opensilex
        .getService("opensilex.OntologyService")
        .getURILabel(this.value)
        .then(http => {
          this.label = http.response.result;
        })
        .catch(() => {
          this.label = this.value;
        })
        .finally(() => {
          this.$opensilex.enableLoader();
        });
    }
  }
}
</script>

<style scoped lang="scss">
</style>

