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
        helpMessage="component.common.uri.help-message"
        :editMode="editMode"
        :generated.sync="uriGenerated"
      ></opensilex-UriForm>

      <!-- Name -->
      <div id="v-step-0">
        <opensilex-InputForm
          :value.sync="form.name"
          label="component.factor.name"
          helpMessage="component.factor.name-help"
          type="text"
          :required="true"
          placeholder="component.factor.name-placeholder"
        ></opensilex-InputForm>
      </div>
      <!-- Category-->
      <div id="v-step-1">
        <opensilex-FactorCategorySelector
          label="component.factor.category"
          placeholder="component.factor.category-placeholder"
          :category.sync="form.category"
        ></opensilex-FactorCategorySelector>
      </div>
      <!-- Comment -->
      <div id="v-step-2">
        <opensilex-TextAreaForm
          :value.sync="form.comment"
          label="component.factor.comment"
          helpMessage="component.factor.comment-help"
          placeholder="component.factor.comment-placeholder"
        ></opensilex-TextAreaForm>
      </div>
      <div id="v-step-3">
        <opensilex-FactorLevelTable
          ref="factorLevelTable"
          :editMode="editMode"
          :factorLevels.sync="form.factorLevels"
        ></opensilex-FactorLevelTable>
      </div>
    </b-form>
  </div>
</template>


<script lang="ts">
import { Component, Prop, Ref, Watch } from "vue-property-decorator";
import Vue from "vue";
import { FactorsService } from "opensilex-core/index";
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

  @Ref("factorLevelTable") readonly factorLevelTable!: any;

  uriGenerated = true;

  @Prop()
  editMode;

  @Prop({
    default: () => {
      return {
        uri: null,
        name: null,
        category: null,
        comment: null,
        exactMatch: [],
        closeMatch: [],
        broader: [],
        narrower: [],
        factorLevels: [
          {
            uri: null,
            name: null,
            comment: null,
          },
        ],
      };
    },
  })
  form;

  savedForm: any = {};

  reset() {
    this.uriGenerated = true;
    if(!this.editMode){
      this.factorTutorial.stop();
    }
  }

  addEmptyRow() {
    console.debug("add row");
    this.form.factorLevels.unshift({
      uri: null,
      name: null,
      comment: null,
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
      comment: null,
      exactMatch: [],
      closeMatch: [],
      broader: [],
      narrower: [],
      factorLevels: [
        {
          uri: null,
          name: null,
          comment: null,
        },
      ],
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
          title: this.$i18n.t("component.factor.comment").toString(),
        },
        content: this.$i18n.t("component.factor.comment-help").toString(),
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
    console.debug("Reinitiliase form");
    this.form.uri = this.savedForm.uri;
    this.form.name = this.savedForm.name;

    this.form.category = this.savedForm.category;

    this.form.comment = this.savedForm.comment;

    this.form.exactMatch = this.savedForm.exactMatch;

    this.form.closeMatch = this.savedForm.closeMatch;

    this.form.broader = this.savedForm.broader;

    this.form.narrower = this.savedForm.narrower;
    this.form.factorLevels = this.savedForm.factorLevels;
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
            this.$i18n.t("component.user.errors.user-already-exists")
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
    this.form.comment = this.$i18n.t("component.factor.example.comment");
    let factorLevels = [];
    for (const [key, value] of Object.entries(
      this.$i18n.t("component.factor.example.factorLevels")
    )) {
      factorLevels.push(value);
    }
    this.form.factorLevels = factorLevels;
    this.factorTutorial.start();
  }
}
</script>

<style scoped lang="scss">
</style>
<i18n>

en:
  component: 
    factor:
      alert-help: This factor can be linked to existing experiments be careful when update
      example :
        name : Nitrogen
        category : nutrient
        comment : Chemical compound added in the soil
        factorLevels :
          - name: "N-10/N+"
            comment: "Dose 10 mmolar"
          - name: "N-5/N-"
            comment: "Dose 5 mmolar"         
    
            
fr:
  component: 
    factor:
      alert-help: Ce facteur peut être lié à des expérimentations faite attention lors de son édition
      example :
        name : Azote
        category : nutrient
        comment : Composant chimique ajouté dans le sol 
        factorLevels :
          - name: "N-10/N+"
            comment: "Dosage 10 mmol"
          - name: "N-5/N-"
            comment: "Dosage 5 mmol"

</i18n>