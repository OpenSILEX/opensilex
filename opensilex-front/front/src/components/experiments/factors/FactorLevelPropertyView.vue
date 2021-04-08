<template>
  <span class="static-field-line">
    <opensilex-UriLink
      v-if="label"
      :uri="value"
      :value="label"
      :to="to"
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
export default class FactorLevelPropertyView extends Vue {
  $opensilex: any;

  @Prop()
  value;

  @Prop({
    default: null,
  })
  experiment;

  label = "";
  factorLevel = null;
  to = null;

  mounted() {
    this.onValueChange();
  }

  @Watch("value")
  onValueChange() {
    if (this.value) {
      this.$opensilex.disableLoader();
      this.$opensilex
        .getService("opensilex.FactorsService")
        .getFactorLevelDetail(this.value)
        .then((http) => {
          this.factorLevel = http.response.result;
          this.label =
            this.factorLevel.name + " (" + this.factorLevel.factor_name + ")";
          if (this.experiment != null) {
            this.to = {
              path:
                "/" +
                encodeURIComponent(this.experiment) +
                "/factor/details/" +
                encodeURIComponent(this.factorLevel.factor),
            };
          }
        })
        .catch(() => {
          this.label = this.value;
          this.to = null;
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

