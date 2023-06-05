<template>
  <div v-if="languages.length>0">

    <ValidationObserver ref="validatorRef">
      <div class="row">
          <div class="sub-form-container ">
            <div class="languagesDropdown float-right">

              <b-dropdown
                  id="langDropdown"
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
            </div>

            <!-- PrefLabel -->
            <!-- :value.sync="form.labelsDTO.prefLabel"-->


            <opensilex-InputForm
                v-model="labelDTO.prefLabel"
                :label="getTranslationOf('prefLabel')"
                type="text"
                :required="true"
                class="wide-input"
            ></opensilex-InputForm>
            <!-- placeholder="EntityForm.prefLabel-placeholder"-->
            <!-- AltLabels -->

            <div class="form-group alt-labels d-flex align-items-center"  v-for="(altLabel, i) in altLabels" :key="i">              <opensilex-InputForm
                  v-model="altLabels[i]"
                  :label="getTranslationOf('altLabel')"
                  type="text"
                  :required="false"
                  class="wide-input "

              ></opensilex-InputForm>

              <opensilex-Button
                  v-if="i===0"
                  ref="entityForm"
                  @click="addAltLabel"
                  class="greenThemeColor ml-0 align-self-center"
                  icon="ik#ik-plus"
              ></opensilex-Button>
            </div>

            <!-- Definition -->
            <opensilex-TextAreaForm
                :value.sync="labelDTO.definition"
                :label="getTranslationOf('definition')"
                type="text"
                :required="true"
                class="wide-input"

            >
            </opensilex-TextAreaForm>
            <hr/>

              <opensilex-CreateButton
                  @click="SaveAndRefillInAnotherLanguage"
                  :label="getTranslationOf('saveAndRefillInAnotherLanguage')"
                  class="createButton float-right"
              ></opensilex-CreateButton>

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

  languages: Array<string>;

  currentLanguage: string = '';

  IsValidsubForm: boolean = false;


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
            es: 'Spanish', // Nouvelle traduction pour l'espagnol
            it: 'Italian', // Nouvelle traduction pour l'italien
          },
          prefLabel: 'Preferred label',
          altLabel: 'Alternative label',
          altLabels: 'Alternative labels',
          definition: 'Definition',
          lang: 'Language',
          saveAndRefillInAnotherLanguage: 'Save and refill',
        },
        fr: {
          language: {
            en: 'Anglais',
            fr: 'Français',
            es: 'Espagnol', // Nouvelle traduction pour l'espagnol
            it: 'Italien', // Nouvelle traduction pour l'italien
          },
          prefLabel: 'Label préféré',
          altLabel: 'Label alternatif',
          altLabels: 'Les labels alternatives',
          definition: 'Définition',
          lang: 'Langue',
          saveAndRefillInAnotherLanguage: 'Enregistrer et re-remplir',
        },
        es: {
          language: {
            en: 'Inglés',
            fr: 'Francés',
            es: 'Español',
            it: 'Italiano',
          },
          prefLabel: 'Etiqueta preferida',
          altLabel: 'Etiqueta alternativa',
          altLabels: 'Etiquetas alternativas',
          definition: 'Definición',
          lang: 'Idioma',
          saveAndRefillInAnotherLanguage: 'Guardar y rellenar',
        },
        it: {
          language: {
            en: 'Inglese',
            fr: 'Francese',
            es: 'Spagnolo',
            it: 'Italiano',
          },
          prefLabel: 'Etichetta preferita',
          altLabel: 'Etichetta alternativa',
          altLabels: 'Etichette alternative',
          definition: 'Definizione',
          lang: 'Lingua',
          saveAndRefillInAnotherLanguage: 'Salva e ricarica',
        },
      },
    });


    this.currentLanguage = this.$i18n.locale;


    this.labelDTOs = [];
    this.dataLoaded = false;

    this.languages = Object.keys(this.i18nLabels.messages);


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
    });


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

  emitIsValidSubForm() {
    this.$emit('update:isValidSubForm', this.IsValidsubForm);
  }


  resetSubForm() {
    this.$refs.validatorRef.reset();
    this.labelDTO = {
      prefLabel: null,
      altLabels: [''],
      definition: null,
      lang: null,
    };
    this.altLabels = [''];
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