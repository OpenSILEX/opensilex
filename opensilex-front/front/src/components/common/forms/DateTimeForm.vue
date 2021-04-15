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
          mode="datetime"
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
            @click="date = undefined"
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

@Component
export default class DateTimeForm extends Vue {
  $opensilex: any;
  $store: any;
  $t: any;

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

  @Prop({ default: "local" })
  valueZone: string;

  private modelConfig = {
    type: "string",
    mask: "iso",
    timeAdjust: "now",
  };

  get user() {
    return this.$store.state.user;
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