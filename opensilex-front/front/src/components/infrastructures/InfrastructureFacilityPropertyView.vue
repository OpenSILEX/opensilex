<template>
  <span class="static-field-line">
    <opensilex-UriLink v-if="label" :uri="value" :value="label"></opensilex-UriLink>
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
export default class InfrastructureFacilityPropertyView extends Vue {
  $opensilex: any;

  @Prop()
  value;

  label = "";
  infrastructure = null;
  to = null;

  mounted() {
    this.onValueChange();
  }

  @Watch("value")
  onValueChange() {
    if (this.value) {
       this.$opensilex.disableLoader();
      this.$opensilex
        .getService("opensilex.OrganisationsService")
        .getInfrastructureFacility(this.value)
        .then(http => {
          this.infrastructure = http.response.result;
          this.label = this.infrastructure.name ;
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

