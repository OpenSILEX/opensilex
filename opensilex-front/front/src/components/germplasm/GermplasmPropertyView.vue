<template>
  <span class="static-field-line">
    <opensilex-UriLink
      v-if="label"
      :uri="value"
      :value="label"
      :to="{path: '/germplasm/details/'+ encodeURIComponent(value)}"
      :target="target"
    ></opensilex-UriLink>
  </span>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  Watch
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class GermplasmPropertyView extends Vue {
  $opensilex: any;

  @Prop()
  value;

  @Prop({
    default: "_self"
  })
  target: string;

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

