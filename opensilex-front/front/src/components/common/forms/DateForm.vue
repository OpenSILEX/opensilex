<template>
  <opensilex-FormField
    :rules="rules"
    :required="required"
    :label="label"
    :helpMessage="helpMessage"
  >
    <template v-slot:field="field">
      <datePicker
        :id="field.id"
        v-model="stringValue"
        input-class="form-control"
        placeholder="$t(placeholder)"
        :clear-button="true"
        @input="onDateSelected"
        @cleared="onDateCleared"
      ></datePicker>
    </template>
  </opensilex-FormField>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  Model,
  Provide,
  PropSync
} from "vue-property-decorator";
import Vue from "vue";

@Component
export default class DateForm extends Vue {
  $opensilex: any;

  @PropSync("value")
  stringValue: string;

  @Prop({
    default: "text"
  })
  type: string;

  @Prop()
  label: string;

  @Prop()
  helpMessage: string;

  @Prop()
  placeholder: string;

  @Prop()
  required: boolean;

  @Prop()
  rules: string | Function;

  @Prop()
  autocomplete: string;


  onDateSelected() {
    if (this.stringValue !== null) {
      this.stringValue = this.format(this.stringValue);
    }
  }
  onDateCleared() {
    this.stringValue = "";
  }

  format(date) {
    var d = new Date(date),
      month = "" + (d.getMonth() + 1),
      day = "" + d.getDate(),
      year = d.getFullYear();

    if (month.length < 2) month = "0" + month;
    if (day.length < 2) day = "0" + day;

    return [year, month, day].join("-");
  }
}
</script>

<style scoped lang="scss">
</style>

