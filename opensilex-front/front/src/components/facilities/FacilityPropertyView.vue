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
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {OrganizationsService} from "opensilex-core/api/organizations.service";

@Component
export default class FacilityPropertyView extends Vue {
  $opensilex: OpenSilexVuePlugin;

  @Prop()
  value;

  @Prop({
    default: "_self"
  })
  target: string;

  label = "";
  organization = null;
  to = null;

  mounted() {
    this.onValueChange();
  }

  @Watch("value")
  onValueChange() {
    if (this.value) {
      this.$opensilex.disableLoader();
      this.$opensilex
        .getService<OrganizationsService>("opensilex.OrganizationsService")
        .getFacility(this.value)
        .then((http) => {
          this.organization = http.response.result;
          this.label = this.organization.name;
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

