<template>
  <div>
    <opensilex-Tutorial
      v-if="!editMode"
      ref="factorTutorial"
      :steps="steps"
      @onSkip="continueFormEditing()"
      @onFinish="continueFormEditing()"
      :editMode="editMode"
    ></opensilex-Tutorial>
    <p v-if="editMode" class="alert-info">
      {{ $t("component.factor.alert-help") }}
    </p>
    <b-form>
      <!-- URI -->
      <opensilex-UriForm
        :uri.sync="form.uri"
        label="component.factor.uri"
        helpMessage="component.common.uri-help-message"
        :editMode="editMode"
        :generated.sync="uriGenerated"
      ></opensilex-UriForm>

      <!-- Name -->
      <div id="v-step-0">
        <opensilex-InputForm
          rules="nameFiltered"
          :value.sync="form.name"
          label="component.factor.name"
          helpMessage="component.factor.name-help"
          type="text"
          :required="true"
          placeholder="component.factor.name-placeholder"
        ></opensilex-InputForm>
      </div>

      <p class="alert-info">
        {{ $t("component.factor.category-help-more") }} : PECO (
        <a
          target="_blank"
          href="http://agroportal.lirmm.fr/ontologies/PECO/?p=classes&conceptid=http%3A%2F%2Fpurl.obolibrary.org%2Fobo%2FPECO_0001001"
        >
          Agroportal
        </a>
        ;
        <a
          target="_blank"
          href="http://agroportal.lirmm.fr/ontologies/PECO/?p=classes&conceptid=http%3A%2F%2Fpurl.obolibrary.org%2Fobo%2FPECO_0007359"
        >
          Ontobee
        </a>
        ) - AGROVOC (
        <a
          target="_blank"
          href="http://agroportal.lirmm.fr/ontologies/AGROVOC/?p=classes&conceptid=http%3A%2F%2Faims.fao.org%2Faos%2Fagrovoc%2Fc_331093"
        >
          Agroportal
        </a>
        ;
        <a
          target="_blank"
          href="http://agroportal.lirmm.fr/ontologies/AGROVOC/?p=classes&conceptid=http%3A%2F%2Faims.fao.org%2Faos%2Fagrovoc%2Fc_331093"
        >
          Agrovoc
        </a>
        )
        {{ $t("component.factor.or") }}
        
      </p>
      <!-- Category-->
      <div id="v-step-1">
        <opensilex-FactorCategorySelector
          ref="factorCategorySelector"
          label="component.factor.category"
          placeholder="component.factor.category-placeholder"
          helpMessage="component.factor.name-help"
          :category.sync="form.category"
        ></opensilex-FactorCategorySelector>
      </div>
      <!-- description -->
      <div id="v-step-2">
        <opensilex-TextAreaForm
          :value.sync="form.description"
          label="component.factor.description"
          helpMessage="component.factor.description-help"
          placeholder="component.factor.description-placeholder"
        ></opensilex-TextAreaForm>
      </div>
      <div id="v-step-3">
        <opensilex-FactorLevelTable
          ref="factorLevelTable"
          :editMode.sync="editMode"
          :factorLevels.sync="form.levels"
        ></opensilex-FactorLevelTable>
      </div>
    </b-form>
  </div>
</template>


<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import HttpResponse, {
  OpenSilexResponse,
} from "opensilex-security/HttpResponse";

@Component
export default class FactorForm extends Vue {
  $opensilex: any;
  $store: any;
  $i18n: any;
  $t: any;

  get user() {
    return this.$store.state.user;
  }
  @Ref("factorTutorial") readonly factorTutorial!: any;
  @Ref("factorCategorySelector") readonly factorCategorySelector!: any;
  @Ref("factorLevelTable") readonly factorLevelTable!: any;

  uriGenerated = true;

  @Prop({ default: false })
  editMode: boolean;

  @Prop({
    default: () => {
      return {
        uri: null,
        name: null,
        category: null,
        description: null,
        experiment: null,
        exact_match: [],
        close_match: [],
        broader_match: [],
        narrower_match: [],
        levels: [
          {
            uri: null,
            name: null,
            description: null,
          },
        ],
      };
    },
  })
  form;

  savedForm: any = {};

  reset() {
    this.uriGenerated = true;
    if (!this.editMode) {
      this.factorTutorial.stop();
    }
  }

  addEmptyRow() {
    console.debug("add row");
    this.form.levels.unshift({
      uri: null,
      name: null,
      description: null,
    });
  }
  saveForm() {
    this.savedForm = JSON.parse(JSON.stringify(this.form));
  }

  getEmptyForm() {
    return {
      uri: null,
      name: null,
      category: null,
      description: null,
      exact_match: [],
      close_match: [],
      broad_match: [],
      narrow_match: [],
      levels: [],
    };
  }

  get steps(): any[] {
    return [
      {
        target: "#v-step-0", // We're using document.querySelector() under the hood
        header: {
          title: this.$i18n.t("component.factor.name").toString(),
        },
        content: this.$i18n.t("component.factor.name-help").toString(),
        params: {
          placement: "left", // Any valid Popper.js placement. See https://popper.js.org/popper-documentation.html#Popper.placements
        },
      },
      {
        target: "#v-step-1",
        header: {
          title: this.$i18n.t("component.factor.category").toString(),
        },
        content: this.$i18n.t("component.factor.category-help").toString(),
        params: {
          placement: "left", // Any valid Popper.js placement. See https://popper.js.org/popper-documentation.html#Popper.placements
        },
      },
      {
        target: "#v-step-2",
        header: {
          title: this.$i18n.t("component.factor.description").toString(),
        },
        content: this.$i18n.t("component.factor.description-help").toString(),
        params: {
          placement: "left", // Any valid Popper.js placement. See https://popper.js.org/popper-documentation.html#Popper.placements
        },
      },
      {
        target: "#v-step-3",
        header: {
          title: this.$i18n.t("component.factorLevel.associated").toString(),
        },
        content: this.$i18n
          .t("component.factorLevel.associated-help")
          .toString(),
        params: {
          placement: "left", // Any valid Popper.js placement. See https://popper.js.org/popper-documentation.html#Popper.placements
        },
      },
    ];
  }

  continueFormEditing() {
    console.debug("Reinitialise form");
    this.form.uri = this.savedForm.uri;
    this.form.name = this.savedForm.name;

    this.form.category = this.savedForm.category;

    this.form.description = this.savedForm.description;

    this.form.exact_match = this.savedForm.exact_match;

    this.form.close_match = this.savedForm.close_match;

    this.form.broader = this.savedForm.broader;

    this.form.narrower = this.savedForm.narrower;
    this.form.levels = this.savedForm.levels;
  }

  setUri(uri: string) {
    this.form.uri = uri;
  }

  updatefactor() {
    return new Promise((resolve, reject) => {
      console.debug("update", this.form);
      return this.$emit("onUpdate", this.form, (result) => {
        if (result instanceof Promise) {
          return result
            .then((resolve) => {
              console.debug("resolve", resolve);
            })
            .catch((reject) => {
              console.debug("resolve", reject);
            });
        } else {
          return resolve(result);
        }
      });
    });
  }

  createFactor() {
    return new Promise((resolve, reject) => {
      return this.$emit("onCreate", this.form, (result) => {
        if (result instanceof Promise) {
          return result
            .then((uri) => {
              this.afterCreate(uri);
              return uri;
            })
            .catch(reject);
        } else {
          return resolve(result);
        }
      });
    });
  }

  afterCreate(uri: string) {
    this.setUri(uri);
    this.$emit("onDetails", uri);
  }

  create(form) {
    console.debug("factor", form);
    return this.$opensilex
      .getService("opensilex.FactorsService")
      .createFactor(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Factor created", uri);
        form.uri = uri;
        return uri;
      })
      .catch((error) => {
        if (error.status == 409) {
          console.error("Factor already exists", error);
          this.$opensilex.errorHandler(
            error,
            this.$i18n.t("component.account.errors.user-already-exists")
          );
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
  }

  update(form) {
    return this.$opensilex
      .getService("opensilex.FactorsService")
      .updateFactor(form)
      .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        let uri = http.response.result;
        console.debug("Factor updated", uri);
      })
      .catch(this.$opensilex.errorHandler);
  }

  get languageCode(): string {
    return this.$opensilex.getLocalLangCode();
  }

  tutorial() {
    this.saveForm();
    this.form.name = this.$i18n.t("component.factor.example.name");
    this.form.category = this.$i18n.t("component.factor.example.category");

    this.factorCategorySelector.select({
      id: this.$i18n.t("component.factor.example.category"),
    });

    this.form.description = this.$i18n.t(
      "component.factor.example.description"
    );
    let levels = [];
    for (const [key, value] of Object.entries(
      this.$i18n.t("component.factor.example.factorLevels")
    )) {
      levels.push(value);
    }
    this.form.levels = levels;
    this.factorTutorial.start();
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
  component: 
    factor:
      alert-help: This factor can be linked to existing experiments be careful when update
      example :
        name : Nitrogen
        category : "http://aims.fao.org/aos/agrovoc/c_5b384c25"
        description : Chemical compound added in the soil
        factorLevels :
          - name: "N-10/N+"
            description: "Dose 10 mmolar"
          - name: "N-5/N-"
            description: "Dose 5 mmolar"         
      category-help-more: "More information"
      or: or    
            
fr:
  component: 
    factor:
      alert-help: Ce facteur peut être lié à des expérimentations faite attention lors de son édition
      example :
        name : Azote
        category : "http://aims.fao.org/aos/agrovoc/c_5b384c25"
        description : Composant chimique ajouté dans le sol 
        factorLevels :
          - name: "N-10/N+"
            description: "Dosage 10 mmol"
          - name: "N-5/N-"
            description: "Dosage 5 mmol"
      category-help-more: "Plus d'informations"
      or : ou
      category-help: "Classifie le facteur utilisé pour des recherches"

</i18n>