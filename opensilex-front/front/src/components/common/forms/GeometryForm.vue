<template>
  <opensilex-FormField
    :rules="isMove ? 'containsPoint|wkt' : 'wkt'"
    :label="label"
    :helpMessage="helpMessage"
    :isMove="isMove"
    :vid="vid"
  >
    <template v-slot:field="field">
      <b-form-input
        :id="field.id"
        :value="stringValue"
        @update="updateValue($event)"
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

@Component
export default class GeometryForm extends Vue {
  $opensilex: any;

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
    if (value != null) {
      try {
        this.stringValue = stringify(value);
      } catch (error) {
        this.stringValue = "";
      }
    }
  }

  updateValue(newValue) {
    this.stringValue = newValue;
    let geoJson = parse(this.stringValue);
    this.geoJson = geoJson;
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

