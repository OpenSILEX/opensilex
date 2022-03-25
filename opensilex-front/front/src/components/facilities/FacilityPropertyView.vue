<template>
  <span class="static-field-line">
    <opensilex-UriLink
      v-if="label"
      :target="target"
      :to="{
        path: '/facility/details/' + encodeURIComponent(value),
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
  Watch,
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class FacilityPropertyView extends Vue {
  $opensilex: any;

  @Prop()
  value;

  @Prop({
    default: "_self"
  })
  target: string;

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
        .getService("opensilex.OrganizationsService")
        .getInfrastructureFacility(this.value)
        .then((http) => {
          this.infrastructure = http.response.result;
          this.label = this.infrastructure.name;
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

