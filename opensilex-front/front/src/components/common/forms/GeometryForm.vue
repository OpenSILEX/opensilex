<template>
  <opensilex-FormField
    :rules="isMove ? 'containsPoint|wkt' : 'wkt'"
    :label="label"
    :helpMessage="helpMessage"
    :vid="vid"
    :required="isRequired"
  >
    <template v-slot:field="field">
      <b-form-input
        :id="field.id"
        :value="stringValue"
        @input="stringValue = $event"
        @blur="updateValue()"
        :disabled="disabled"
        type="text"
        :required="isRequired"
        :placeholder="$t(placeholder)"
      ></b-form-input>
    </template>
  </opensilex-FormField>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Watch } from "vue-property-decorator";
import Vue from "vue";
import { parse, stringify } from "wkt";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";

@Component
export default class GeometryForm extends Vue {
  $opensilex: OpenSilexVuePlugin;

  @PropSync("value")
  geoJson: any;

  stringValue: string = "";

  mounted() {
    if (this.geoJson) {
      this.onGeoJsonChange(this.geoJson);
    }
  }

  @Watch("geoJson")
  onGeoJsonChange(value) {
    if (!value) {
      this.stringValue = "";
      return;
    }

    try {
      this.stringValue = stringify(value);
    } catch (e) {
      console.warn("Invalid geojson to stringify", e);
    }
  }

  updateValue() {
    const geometryValue = (this.stringValue || "").trim();
    if (!geometryValue) {
      this.geoJson = undefined;
      this.$emit("onUpdate");
      return;
    }

    try {
      const parsed = parse(geometryValue);
      this.geoJson = parsed; // sync que si c'est ok
      this.$emit("onUpdate");
    } catch (e) {
      console.warn(e);
    }
  }


  @Prop()
  label: string;

  @Prop()
  helpMessage: string;

  @Prop()
  placeholder: string;

  @PropSync("required")
  isRequired: boolean;

  @Prop()
  disabled: boolean;

  @Prop({ default: false })
  isMove: boolean

  @Prop()
  vid;
}
</script>

<style scoped lang="scss">
</style>

