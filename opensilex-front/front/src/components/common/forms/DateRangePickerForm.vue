<template>
  <opensilex-FormField
    :rules="rules"
    :required="required"
    :label="label"
    :helpMessage="helpMessage"
  >
    <template v-slot:field="field">

      <div  class="datepicker-trigger">
        <b-input-group>

          <b-form-input
            :value="stringValue"
            @blur="updateDates()"
            :disabled="disabled"
            :type="type"
            :required="required"
            :placeholder="$t('DateRangePickerForm.placeholder')"
            :autocomplete="autocomplete"
          ></b-form-input>
          <b-input-group-append>
            <b-button :id="field.id" variant="primary"><opensilex-Icon icon="ik#ik-calendar" /></b-button>
            <b-button variant="primary" class="last" @click="clearDates()">x</b-button>
          </b-input-group-append>
          
          <AirbnbStyleDatepicker 
            ref="datepicker"
            :id="field.id" 
            :trigger-element-id="field.id" 
            :mode="'range'" 
            :date-one.sync="dateOne" 
            :date-two.sync="dateTwo" 
            @date-one-selected="val => { dateOne = val }" 
            @date-two-selected="val => { dateTwo = val }" 
            @apply="formatDates()"
            @cancelled="updateDates()"
          />
        </b-input-group>
      </div>
    </template>
  </opensilex-FormField>
</template>

<script lang="ts">
import {
  Component,
  Prop,
  Model,
  Provide,
  PropSync,
  Ref
} from "vue-property-decorator";
import Vue from "vue";
import moment from 'moment';
import DateRangePicker from 'vue-airbnb-style-datepicker';
import VueI18n from 'vue-i18n'

@Component
export default class DateRangePickerForm extends Vue {
  $opensilex: any;

  @Ref("datepicker") readonly datepicker!: any;

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
  required: boolean;

  @Prop()
  rules: string | Function;
  
  dateFormat = 'DD/MM/YYYY';
  dateFormatPicker = 'YYYY-DD-MM';

  dateOne;
  dateTwo;

  created() {
    const datepickerOptions = {
      monthNames: [
        this.$i18n.t('DateRangePickerForm.monthNames.january'),
        this.$i18n.t('DateRangePickerForm.monthNames.february'),
        this.$i18n.t('DateRangePickerForm.monthNames.march'),
        this.$i18n.t('DateRangePickerForm.monthNames.april'),
        this.$i18n.t('DateRangePickerForm.monthNames.may'),
        this.$i18n.t('DateRangePickerForm.monthNames.june'),
        this.$i18n.t('DateRangePickerForm.monthNames.july'),
        this.$i18n.t('DateRangePickerForm.monthNames.august'),
        this.$i18n.t('DateRangePickerForm.monthNames.september'),
        this.$i18n.t('DateRangePickerForm.monthNames.october'),
        this.$i18n.t('DateRangePickerForm.monthNames.november'),
        this.$i18n.t('DateRangePickerForm.monthNames.december')
      ],
      days: [
        this.$i18n.t('DateRangePickerForm.dayNames.monday'),
        this.$i18n.t('DateRangePickerForm.dayNames.tuesday'),
        this.$i18n.t('DateRangePickerForm.dayNames.wednesday'),
        this.$i18n.t('DateRangePickerForm.dayNames.thursday'),
        this.$i18n.t('DateRangePickerForm.dayNames.friday'),
        this.$i18n.t('DateRangePickerForm.dayNames.saturday'),
        this.$i18n.t('DateRangePickerForm.dayNames.sunday')
      ],
      daysShort: [
        this.$i18n.t('DateRangePickerForm.dayShortNames.mon'),
        this.$i18n.t('DateRangePickerForm.dayShortNames.tue'),
        this.$i18n.t('DateRangePickerForm.dayShortNames.wed'),
        this.$i18n.t('DateRangePickerForm.dayShortNames.thu'),
        this.$i18n.t('DateRangePickerForm.dayShortNames.fri'),
        this.$i18n.t('DateRangePickerForm.dayShortNames.sat'),
        this.$i18n.t('DateRangePickerForm.dayShortNames.sun')
      ],
      texts: {
        apply: this.$i18n.t('DateRangePickerForm.texts.apply'),
        cancel: this.$i18n.t('DateRangePickerForm.texts.cancel')
      }
    };

    Vue.use(DateRangePicker, datepickerOptions);

    this.updateDates();
  }

  formatDates() {
    let formattedDates = '';
    if (this.dateOne) {
      formattedDates = moment(this.dateOne).format(this.dateFormat);
    }
    if (this.dateTwo) {
      formattedDates += ' - ' + moment(this.dateOne).format(this.dateFormat);
    }
    this.stringValue = formattedDates;
  }

  clearDates() {
    this.dateOne = undefined;
    this.dateTwo = undefined;
    this.stringValue = undefined;
  }

  updateDates() {
    let dateOne = undefined;
    let dateTwo = undefined;

    if(this.stringValue) {
      let dates = this.stringValue.split(" - ");
      
      if(dates && dates.length == 2) {
        dateOne = moment(dates[0], this.dateFormat);
        dateTwo = moment(dates[1], this.dateFormat);
      }
    }

    this.dateOne = dateOne;
    this.dateTwo = dateTwo;
  }
}

</script>

<style scoped lang="scss">

  ::v-deep button.last {
    border-left: 1px solid #ffffff;
    border-top-right-radius: 5px !important;
    border-bottom-right-radius: 5px !important;
  }

  .field {
    position: relative;
  }

</style>

<i18n>

en:
  DateRangePickerForm:
    dayNames:
      sunday: Sun
      monday: Mon
      tuesday: Tue
      wednesday: Wed
      thursday: Thu
      friday: Fri
      saturday: Sat
    monthNames: 
      january: January
      february: February
      march: March
      april: April
      may: May
      june: Jun,
      july: July
      august: August
      september: September
      october: October
      november: November
      december: December
    dayShortNames:
      sun: Sun
      mon: Mon
      tue: Tue
      wed: Wed
      thu: Thu
      fri: Fri
      sat: Sat
    texts:
      apply: Apply
      cancel: Cancel
    placeholder: DD/MM/YYYY - DD/MM/YYYY

fr:
  DateRangePickerForm:
    dayNames:
        sunday: Dim
        monday: Lun
        tuesday: Mar
        wednesday: Mer
        thursday: Jeu
        friday: Ven
        saturday: Sam
    monthNames:
        january: Janvier
        february: Février
        march: Mars
        april: Avril
        may: Mai
        june: Juin
        july: Juillet
        august: Août
        september: Septembre
        october: Octobre
        november: Novembre
        december: Decembre
    dayShortNames:
        sun: Dim
        mon: Lun
        tue: Mar
        wed: Mer
        thu: Jeu
        fri: Ven
        sat: Sam
    texts:
      apply: Appliquer
      cancel: Annuler
    placeholder: JJ/MM/AAAA - JJ/MM/AAAA

</i18n>


            