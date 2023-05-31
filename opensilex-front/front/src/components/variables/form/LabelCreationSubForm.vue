<template>
  <div v-if="languages.length>0">

    <ValidationObserver ref="validatorRef">
      <div class="row">
        <div class="col">
          <div class="sub-form-container ">

            <b-dropdown
                id="langDropdown"
                class="languagesDropdown"
                variant="link"
                right
            >
              <template v-slot:button-content>
                <i class="icon ik ik-globe"></i>
                <span class="hidden-phone">{{ getTranslationOfLanguage(currentLanguage) }}</span>
                <span class="show-phone">{{ getTranslationOfLanguage(currentLanguage).substring(0, 2) }}</span>
                <i class="ik ik-chevron-down"></i>
              </template>
              <b-dropdown-item
                  v-for="item in languages"
                  :key="`language-${item}`"
                  href="#"
                  @click.prevent="setLanguage(item)"
              >
                {{ getTranslationOfLanguage(item) }}
              </b-dropdown-item>
            </b-dropdown>

            <!-- PrefLabel -->
            <!-- :value.sync="form.labelsDTO.prefLabel"-->

            <opensilex-InputForm
                v-model="labelDTO.prefLabel"
                :label="getTranslationOf('prefLabel')"
                type="text"
                :required="true"
            ></opensilex-InputForm>
            <!-- placeholder="EntityForm.prefLabel-placeholder"-->
            <!-- AltLabels -->

            <div class="alt-labels" v-for="(altLabel, i) in this.altLabels" :key="i">
              <opensilex-InputForm
                  v-model="altLabels[i]"
                  :label="getTranslationOf('altLabel')"
                  type="text"
                  :required="false"
                  class="mr-2"
              ></opensilex-InputForm>

              <opensilex-Button
                  v-if="i===0"
                  ref="entityForm"
                  @click="addAltLabel"
                  class="greenThemeColor"
                  icon="ik#ik-plus"
              ></opensilex-Button>
            </div>

            <!-- Definition -->
            <opensilex-TextAreaForm
                :value.sync="labelDTO.definition"
                :label="getTranslationOf('definition')"
                type="text"
                :required="true"
            >
            </opensilex-TextAreaForm>

            <opensilex-CreateButton
                @click="SaveAndRefillInAnotherLanguage"
                :label="getTranslationOf('saveAndRefillInAnotherLanguage')"
                class="createButton"
            ></opensilex-CreateButton>
          </div>
        </div>
      </div>
    </ValidationObserver>
  </div>

</template>


<script lang="ts">
import {Component, Prop, PropSync, Ref, Watch} from "vue-property-decorator";
import EntityCreate from "./EntityCreate.vue";
// @ts-ignore
import {EntityCreationDTO} from "opensilex-core/index";
import {LabelDTO} from "opensilex-core/model/labelDTO";

import Vue from 'vue';

export const EventBus = new Vue();


import VueI18n from "vue-i18n";

@Component
export default class LabelCreationSubForm extends Vue {

  @Ref("validatorRef") readonly validatorRef!: any;

  i18nLabels: VueI18n;

  languages: Array<string> = Object.keys(this.$i18n.messages);

  currentLanguage: string = '';

  subFormValid: boolean = false;


  labelDTO: LabelDTO = {
    prefLabel: null,
    altLabels: [''],
    definition: null,
    lang: null,
  }

  altLabels: Array<string> = [''];


  labelDTOs: Array<LabelDTO> = [];

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
      messages: {
        en: {
          language: {
            en: 'English',
            fr: 'French',
          },

          prefLabel: 'Preferred label',
          altLabel: 'Alternative label',
          altLabels: 'Alternative labels',
          definition: 'Definition',
          lang: 'language',
          saveAndRefillInAnotherLanguage: 'Save and refill',

        },
        fr: {
          language: {
            en: 'Anglais',
            fr: 'Français',
          },
          prefLabel: 'Label préféré',
          altLabel: 'Label alternatif',
          altLabels: 'Les labels alternatives',
          definition: 'définition',
          lang: 'language',
          saveAndRefillInAnotherLanguage: 'Enregistrer et re-remplir',
        },
      },
    });

    this.currentLanguage = this.$i18n.locale;


    this.labelDTOs = [];
    this.dataLoaded = false;


    // ca va pas
    this.$emit('labelDTOs', this.labelDTOs);
    this.$emit('dataLoaded', this.dataLoaded);

  }

  getTranslationOf(element: string) {

    return this.i18nLabels.t(element);

  }

  getTranslationOfLanguage(lang: string) {

    return this.i18nLabels.messages[lang].language[lang];

  }

  IsValidSubForm(){
    let result = this.validatorRef.validate();
    if (result instanceof Promise) {
      return result
          .then((resolve) => {

            if(resolve){
              this.subFormValid = true;
              this.$emit('subFormValid', this.subFormValid);

            }
          })
          .catch((reject) => {
          });
    } else {
      if(result){

        this.subFormValid = true;

      }
    }
  }


  SaveAndRefillInAnotherLanguage() {

    this.IsValidSubForm();

    if (this.subFormValid) {

      this.labelDTO.lang = this.getTranslationOfLanguage(this.currentLanguage);
      this.labelDTO.altLabels = this.altLabels;
      this.labelDTOs.push(this.labelDTO);
      this.$emit('onSubmitSubForm', this.labelDTO);

      this.dataLoaded = true;
      this.$emit('dataLoadedSubForm', this.dataLoaded);


      const lowerCaseLang = (this.labelDTO.lang.toLowerCase()).substring(0, 2);
      const index = this.languages.indexOf(lowerCaseLang);

      if (index !== -1) {

        this.languages.splice(index, 1);
        this.setLanguage(this.languages[0]);

      }

      this.labelDTO = {
        prefLabel: null,
        altLabels: [''],
        definition: null,
        lang: null,
      }

      this.altLabels = [''];

    }


  }

  addAltLabel() {

    this.altLabels.splice(this.altLabels.length);
    Vue.set(this.altLabels, this.altLabels.length, '');
  }


  setLanguage(lang: string) {
    this.i18nLabels.locale = lang;
    this.currentLanguage = this.i18nLabels.locale;
  }

  language() {
    return this.i18nLabels;
  }


}
</script>

<style scoped lang="scss">
a {
  color: #007bff;
}


</style>

<i18n>
en:
  LabelCreationSubForm:
    language:
      fr: French
      en: English
    prefLabel: Preferred label
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
    altLabel: Label alternatif
    altLabels: Les labels alternatives
    definition: définition
    lang: langue
    saveAndRefillInAnotherLanguage: Enregistrer et re-remplir,

</i18n>