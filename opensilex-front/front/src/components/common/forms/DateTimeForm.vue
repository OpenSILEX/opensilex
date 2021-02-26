<template>
  <opensilex-FormField
    :required="required"
    :label="label"
    :helpMessage="helpMessage"
  >
    <template v-slot:field="field">
      <b-input-group>
        <template #append
          ><opensilex-Button
            @click="clearValue()"
            class="clear-btn"
            variant="outline-secondary"
            icon="fa#times"
            :small="true"
            :label="$t('DateTime.clear')"
          ></opensilex-Button>
        </template>
        <datetime
          v-model="datetime"
          :type="type"
          :zone="zone"
          :value-zone="valueZone"
          :flow="flow"
          :format="format"
          :phrases="$t('DateTime.phrases')"
          @input="$emit('input', $event)"
          @close="$emit('close', $event)"
        >
        </datetime>
      </b-input-group>
    </template>
  </opensilex-FormField>
</template>

<script lang="ts">
import { Component, Prop, PropSync } from "vue-property-decorator";
import Vue from "vue";
import { Settings } from "luxon";

@Component
export default class DateTimeForm extends Vue {
  $opensilex: any;
  $t: any;

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      (lang) => {
        Settings.defaultLocale = lang;
      }
    );
    let vdatetimeInputlist = this.$el.getElementsByClassName("vdatetime-input");
    for (var i = 0; i < vdatetimeInputlist.length; ++i) {
      vdatetimeInputlist[i].classList.add("form-control");
    }
  }

  clearValue() {
    this.datetime = "";
    this.$emit("clear");
  }

  @Prop()
  label: string;

  @Prop()
  helpMessage: string;

  @Prop()
  placeholder: string;

  @PropSync("required")
  isRequired: boolean;

  @Prop({ default: false })
  disabled: boolean;

  @PropSync("value")
  datetime: string;

  @Prop({ default: "datetime" })
  type: string;

  @Prop({ default: "local" })
  zone: string;

  @Prop({ default: "local" })
  valueZone: string;

  @Prop({ default: "dd-MMM-yyyy HH:mm ZZ" })
  format: string;

  flow: Array<string> = ["date", "time"];
}
</script>

<style scoped lang="scss">
::v-deep .vdatetime-input {
  padding-bottom: 7px !important;
  padding-top: 7px !important;
}

::v-deep .input-group {
  flex-wrap: nowrap;
}
</style>

<i18n>
en :
    DateTime :
        phrases :
            ok: Ok
            cancel: Cancel
        clear : Effacer
fr :
    DateTime :
        phrases :
            ok: Ok
            cancel: Annuler
        clear : Clear
</i18n>