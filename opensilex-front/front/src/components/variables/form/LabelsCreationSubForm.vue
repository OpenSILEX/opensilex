<template>
  <div v-if="languages.length>0">

    <ValidationObserver ref="validatorRef">
      <opensilex-Tutorial
          ref="LabelCreationSubFormTutorial"
          :steps="tutorialSteps"

      ></opensilex-Tutorial>

      <div class="sub-form-container ">
        <div class="col-lg-6 text-right" id="#v-step-lang">
          <b-dropdown
              id="langDropdown float-right"
              variant="link">
            <template v-slot:button-content>
              <i class="icon ik ik-globe"></i>
              <span class="hidden-phone">
                {{ getTranslationOfLanguage(i18nLabels.locale) }}
              </span>
              <span class="show-phone">
                {{ getTranslationOfLanguage(i18nLabels.locale).substring(0, 2)}}
              </span>
              <i class="ik ik-chevron-down"></i>
            </template>

            <b-dropdown-item
                v-for="item in languages"
                :key="`language-${item}`"
                href="#"
                @click.prevent="setLanguage(item)">
              {{ getTranslationOfLanguage(item) }}
            </b-dropdown-item>
          </b-dropdown>
        </div>

        <!-- PrefLabel -->
        <div class="row">
          <div class="col-lg-6" id="#v-step-prefLabel">
            <opensilex-InputForm
                v-model="labelDTO.prefLabel"
                :label="getTranslationOf('prefLabel')"
                type="text"
                :required="true"
                class="wide-input"
            ></opensilex-InputForm>
          </div>

          <div class="col-lg-6" id="#v-step-shortLabel">
            <opensilex-InputForm
                v-model="labelDTO.shortLabel"
                :label="getTranslationOf('shortLabel')"
                type="text"
                :required="false"
                class="wide-input"
            ></opensilex-InputForm>
          </div>

          <!-- AltLabels -->
          <div class="col-lg-6" id="#v-step-altLabel">
            <div class="form-group alt-labels d-flex align-items-center"
                 v-for="(altLabel, i) in labelDTO.altLabels"
                 :key="i">
              <opensilex-InputForm
                  v-if="i===0"
                  v-model="labelDTO.altLabels[i]"
                  :label="getTranslationOf('altLabel')"
                  type="text"
                  :required="false"
                  :actionHandler="addAltLabel"
                  class="wide-input "
              ></opensilex-InputForm>

              <opensilex-InputForm
                  v-else
                  v-model="labelDTO.altLabels[i]"
                  :label="getTranslationOf('altLabel')"
                  type="text"
                  :required="false"
                  class="wide-input "
              ></opensilex-InputForm>
            </div>
          </div>

          <!-- Definition -->
          <div class="col-lg-6" id="#v-step-definition">
            <opensilex-TextAreaForm
                :value.sync="labelDTO.definition"
                :label="getTranslationOf('definition')"
                type="text"
                :required="true"
                class="wide-input"
            >
            </opensilex-TextAreaForm>

          </div>

        </div>
      </div>
    </ValidationObserver>
  </div>

</template>


<script lang="ts">

import {Component, Prop, PropSync, Ref, Watch} from "vue-property-decorator";
import EntityCreate from "./EntityCreate.vue";

import {EntityCreationDTO} from "opensilex-core/model/entityCreationDTO";
// import {LabelDTO} from "opensilex-core/model/labelDTO"
import {MultiLabelsDTO} from "opensilex-core/model/multiLabelsDTO";

import Vue from 'vue';


import VueI18n from "vue-i18n";

@Component
export default class LabelsCreationSubForm extends Vue {

  @Ref("validatorRef") readonly validatorRef!: any;

  i18nLabels: VueI18n;

  languages: Array<string>;

  IsValidsubForm: boolean = false;

  @PropSync("form")
  labelDTO = {
    prefLabel: null,
    shortLabel: null,
    altLabels: [''],
    definition: null,
    lang: this.$i18n.locale,
  }


  labelDTOs: MultiLabelsDTO = {
    prefLabels: {},
    shortLabels: {},
    altLabels: {},
    definitions: {},
  };

  dataLoaded: boolean;

  emits: ['labelDTOsSubForm', 'dataLoadedSubForm'];

  created() {

    this.initAttributs();

  }

  validate() {
    return this.validatorRef.validate();
  }


  initAttributs() {


    this.i18nLabels = new VueI18n({
      locale: this.$i18n.locale, // Langue par défaut
      messages: this.$i18n.messages
    });


    this.labelDTOs = {
      prefLabels: {},
      shortLabels: {},
      altLabels: {},
      definitions: {}
    };
    this.dataLoaded = false;

    this.languages = Object.keys(this.i18nLabels.messages);


    // ca va pas
    this.$emit('labelDTOs', this.labelDTOs);
    this.$emit('dataLoaded', this.dataLoaded);


  }

  getTranslationOf(element: string) {
    const lang = this.i18nLabels.locale;
    const translations = this.i18nLabels.messages[lang]["LabelCreationSubForm"];
    return translations[element];
  }


  getTranslationOfLanguage(lang: string) {
    const translations = this.i18nLabels.messages[lang]["LabelCreationSubForm"];
    return translations["language"][lang];
  }


  getLabelDTO() {
    return this.labelDTO;
  }

  CheckIfIsValidSubForm() {
    let result = this.validatorRef.validate();
    console.log("result", result);
    if (result instanceof Promise) {
      return result
          .then((resolve) => {

            if (resolve) {
              this.IsValidsubForm = true;

              console.log("IsValidsubForm dans check ", this.IsValidsubForm);

              this.$emit('subFormValid', this.IsValidsubForm);

            }
          })
          .catch((reject) => {
          });
    } else {

      if (result) {

        console.log("IsValidsubForm dans check else ", this.IsValidsubForm);

        this.IsValidsubForm = true;

      }
    }
  }


  SaveAndRefillInAnotherLanguage() {

    this.CheckIfIsValidSubForm().then(() => {
      console.log("IsValidsubForm from SaveAndRefillInAnotherLanguage", this.IsValidsubForm);

      if (this.IsValidsubForm) {

        const lang = this.labelDTO.lang;

        this.labelDTOs.prefLabels[lang] = this.labelDTO.prefLabel;
        this.labelDTOs.shortLabels[lang] = this.labelDTO.shortLabel;
        this.labelDTOs.altLabels[lang] = this.labelDTO.altLabels;
        this.labelDTOs.definitions[lang] = this.labelDTO.definition;


        this.$emit('onSubmitSubForm', this.labelDTO);

        this.dataLoaded = true;
        this.$emit('dataLoadedSubForm', this.dataLoaded);

        const lowerCaseLang = (this.labelDTO.lang.toLowerCase()).substring(0, 2);
        const index = this.languages.indexOf(lowerCaseLang);

        if (index !== -1) {
          this.languages.splice(index, 1);
          this.i18nLabels.locale = this.languages[0];
        }

        this.labelDTO = {
          prefLabel: null,
          shortLabel: null,
          altLabels: [''],
          definition: null,
          lang: this.i18nLabels.locale,
        }
      }
    });
  }

  addAltLabel() {

    this.labelDTO.altLabels.splice(this.labelDTO.altLabels.length);
    Vue.set(this.labelDTO.altLabels, this.labelDTO.altLabels.length, '');

  }


  setLanguage(lang: string) {

    // verifie pour altLabels
    if (this.labelDTO.shortLabel || this.labelDTO.altLabels || this.labelDTO.definition) {
      this.SaveAndRefillInAnotherLanguage();
    }

    this.i18nLabels.locale = lang;


  }

  language() {
    return this.i18nLabels;
  }

  emitIsValidSubForm() {
    this.$emit('update:isValidSubForm', this.IsValidsubForm);
  }

  get tutorialSteps(): any[] {
    return [
      {
        target: "#v-step-lang",
        params: {placement: "right"},
      },
      {
        target: "#v-step-prefLabel",
        params: {placement: "left"},
      },
      {
        target: "#v-step-altLabel",
        params: {placement: "left"},
      },
      {
        target: "#v-step-shortLabel",
        params: {placement: "right"},
      },
      {
        target: "#v-step-definition",
        params: {placement: "right"},
      },
    ];
  }


  resetSubForm() {
    this.validatorRef.reset();
    this.labelDTO = {
      prefLabel: null,
      shortLabel: null,
      altLabels: [''],
      definition: null,
      lang: null,
    };

  }


}
</script>

<style scoped lang="scss">
a {
  color: #007bff;
}

.wide-input {
  width: 100%; /* ou une valeur spécifique en pixels ou pourcentage */
}

#langDropdown .dropdown-menu {
  right: 0;
  left: auto;
}


</style>

<i18n>
en:
  LabelCreationSubForm:
    language:
      fr: French
      en: English
    prefLabel: Preferred label
    shortLabel: Short label
    altLabel: Alternative label
    altLabels: Alternative labels
    definition: Definition
    lang: language
    saveAndRefillInAnotherLanguage: Save and refill

fr:
  LabelCreationSubForm:
    language:
      fr: Français
      en: Anglais
    prefLabel: Label préféré
    shortLabel: Label court
    altLabel: Label alternatif
    altLabels: Les labels alternatives
    definition: définition
    lang: langue
    saveAndRefillInAnotherLanguage: Enregistrer et re-remplir,

</i18n>