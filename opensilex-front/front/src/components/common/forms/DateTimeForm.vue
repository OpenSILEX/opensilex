<template>
  <opensilex-FormField
    :helpMessage="helpMessage"
    :label="label"
    :required="isRequired"
  >
    <template v-slot:field="field">
      <b-input-group>
        <vc-date-picker
          v-model="date"
          :locale="user.locale"
          :max-date="maxDate"
          :min-date="minDate"
          :model-config="modelConfig"
          class="inline-block h-full"
          is24hr
          :valid-hours="checkIfHourIsValid"
          mode="datetime"
          :timezone="timezone"
          @input="input"
        >
          <template v-slot="{ inputValue, inputEvents }">
            <input
              :id="field.id"
              v-on="inputEvents"
              :disabled="disabled"
              :placeholder="$t(placeholder)"
              :required="isRequired"
              type="text"
              :value="inputValue"
              class="form-control"
            />
          </template>
        </vc-date-picker>
        <template #append>
          <b-btn
            class="clear-btn"
            variant="outline-secondary"
            @click="clear"
          >
            <opensilex-Icon icon="fa#times" />
          </b-btn>
        </template>
      </b-input-group>
    </template>
  </opensilex-FormField>
</template>

<script lang="ts">
import { Component, Prop, PropSync } from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";

@Component
export default class DateTimeForm extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $store: any;
  $t: any;

  validHour : { min: number, max: number};

  @Prop()
  label: string;

  @Prop()
  helpMessage: string;

  @Prop({ default: "DateTimeForm.placeholder" })
  placeholder: string;

  @PropSync("required", { default: false })
  isRequired: boolean;

  @Prop({ default: false })
  disabled: boolean;

  @PropSync("value")
  date: string;

  @Prop()
  maxDate: Date;

  @Prop()
  minDate: Date;

  @Prop({ default: "" })
  timezone: string;

  private modelConfig = {
    type: "string",
    mask: "iso"
  };

  input(value) {
    this.$emit("input", value);
  }
  
  clear() {
    this.date = undefined;
    this.$emit("clear");
  }

  get user() {
    return this.$store.state.user;
  }

  get checkIfHourIsValid(){
    const startDateObj = new Date(this.minDate);
    const startDateDay = startDateObj.getUTCDate()
    const startDateHour = startDateObj.getUTCHours();

    const endDateObj = new Date(this.maxDate);
    const endDateDay = endDateObj.getUTCDate();

    if (startDateDay == endDateDay){
      this.validHour = { min: startDateHour+2 , max: 24 };
    } else {
      this.validHour = { min: 0, max: 24 };
    }
    return this.validHour;      
  }
}
</script>

<style lang="scss" scoped>
::v-deep .input-group {
  flex-wrap: nowrap;
}
</style>

<i18n>
en:
  DateTimeForm:
    placeholder: MM/DD/YYYY hh:mm
fr:
  DateTimeForm:
    placeholder: JJ/MM/AAAA hh:mm
</i18n>
