<template>
  <div>
    <opensilex-SelectForm
      :label="label"
      :helpMessage="helpMessage"
      :selected.sync="selectedSeparator"
      :options="delimiterOptions"
      :required="required"
      :rules="rules"
    ></opensilex-SelectForm>
  </div>
</template>

<script lang="ts">
import { Component, Prop, PropSync } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class CSVSelectorInputForm extends Vue {
  $opensilex: any;
  $t: any;

  @PropSync("separator")
  selectedSeparator: string;

  @Prop({ default: "component.common.csv-delimiters.label" })
  label: string;

  @Prop({ default: "component.common.csv-delimiters.placeholder" })
  helpMessage: string;

  @Prop({ default: true })
  required: boolean;

  @Prop({ default: false })
  disabled: boolean;

  @Prop()
  rules: string | Function;

  private langUnwatcher;
  mounted() {
    if (this.$store.getters.language == "fr") {
      this.selectedSeparator = ";";
    } else {
      this.selectedSeparator = ",";
    }
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      () => {
        if (this.$store.getters.language == "fr") {
          this.selectedSeparator = ";";
        } else {
          this.selectedSeparator = ",";
        }
      }
    );
  }

  get delimiterOptions() {
    return [
      {
        id: ",",
        label: this.$t("component.common.csv-delimiters.comma"),
      },
      {
        id: ";",
        label: this.$t("component.common.csv-delimiters.semicolon"),
      },
    ];
  }
}
</script>

<style scoped lang="scss">
</style>

