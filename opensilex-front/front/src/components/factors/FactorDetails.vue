<template>
  <div class="container-fluid">
    <b-row>
      <b-col sm="5">
        <opensilex-Card label="component.factor.details.description">
          <template v-slot:rightHeader>
            <div class="ml-3">
              <opensilex-EditButton
                @click="factorForm.showEditForm(factor)"
                variant="outline-primary"
                label="component.factor.update-button"
              ></opensilex-EditButton>
              <opensilex-InteroperabilityButton
                v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_FACTOR_MODIFICATION_ID
                  )
                "
                label="component.skos.update"
                @click="skosReferences.show()"
              ></opensilex-InteroperabilityButton>
              <opensilex-DeleteButton
                v-if="
                  user.hasCredential(credentials.CREDENTIAL_FACTOR_DELETE_ID)
                "
                :small="true"
                label="component.common.list.buttons.delete"
                @click="$emit('onDelete')"
              ></opensilex-DeleteButton>
            </div>
          </template>
          <template v-slot:body>
            <opensilex-StringView
              label="component.factor.uri"
              :value="factor.uri"
            ></opensilex-StringView>
            <opensilex-StringView
              label="component.factor.name"
              :value="factor.name"
            ></opensilex-StringView>
            <opensilex-StringView
              label="component.factor.category"
              :value="$i18n.t(factorCategoriesMap[factor.category])"
            ></opensilex-StringView>
            <opensilex-StringView
              label="component.factor.description"
              :value="factor.description"
            ></opensilex-StringView>
          </template>
        </opensilex-Card>

        <opensilex-Card
          label="component.skos.ontologies-references-label"
          icon="fa#globe-americas"
        >
          <template v-slot:body>
            <opensilex-ExternalReferencesDetails
              :skosReferences="factor"
            ></opensilex-ExternalReferencesDetails>
          </template>
        </opensilex-Card>
      </b-col>

      <b-col>
        <opensilex-Card label="component.factor.details.factorLevels" icon>
          <template v-slot:body>
            <opensilex-TableView
              v-if="factor.levels.length != 0"
              :items="factor.levels"
              :fields="factorLevelFields"
              :globalFilterField="true"
            >
              <template v-slot:cell(name)="{ data }">
                <opensilex-UriLink
                  :uri="data.item.uri"
                  :value="data.item.name"
                ></opensilex-UriLink>
              </template>
            </opensilex-TableView>

            <p v-else>
              <strong>{{
                $t("component.factor.details.no-factorLevels-provided")
              }}</strong>
            </p>
          </template>
        </opensilex-Card>
      </b-col>
    </b-row>

    <opensilex-ExternalReferencesModalForm
      ref="skosReferences"
      :references.sync="factor"
      @onUpdate="updateReferences"
    ></opensilex-ExternalReferencesModalForm>
    <opensilex-ModalForm
      v-if="user.hasCredential(credentials.CREDENTIAL_FACTOR_MODIFICATION_ID)"
      ref="factorForm"
      modalSize="lg"
      :successMessage="successMessage"
      component="opensilex-FactorForm"
      createTitle="component.factor.add"
      editTitle="component.factor.update"
      icon="fa#sun"
      @onUpdate="update"
    ></opensilex-ModalForm>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref, PropSync } from "vue-property-decorator";
import Vue from "vue";
import { FactorCategory } from "./FactorCategory";

@Component
export default class FactorDetails extends Vue {
  $opensilex: any;
  $store: any;
  $route: any;
  $t: any;
  $i18n: any;
  service: any;

  @Ref("skosReferences") readonly skosReferences!: any;

  @Ref("factorForm") readonly factorForm!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }
  created() {
    this.service = this.$opensilex.getService("opensilex.FactorsService");
  }

  factorCategoriesMap: Map<
    string,
    string
  > = FactorCategory.getFactorCategories();

  @Prop({
    default: () => {
      return {
        uri: null,
        name: null,
        category: null,
        description: null,
        exactMatch: [],
        closeMatch: [],
        broader: [],
        narrower: [],
        levels: [],
      };
    },
  })
  factor: any;

  factorLevelFields: any[] = [
    {
      key: "name",
      label: "component.factorLevel.name",
      sortable: true,
    },
    {
      key: "description",
      label: "component.factorLevel.description",
      sortable: false,
    },
  ];

  update() {
    this.$emit("onUpdate");
  }

  updateReferences() {
    this.$emit("onUpdateReferences", this.factor);
  }

  successMessage(factor) {
    return this.$i18n.t("component.factor.label") + " " + factor.name;
  }
}
</script>

<style scoped lang="scss">
.details-actions-row {
  margin-top: -35px;
  margin-left: -15px;
  margin-right: 15px;
}
</style>
<i18n>
en:
  component:
    factor :
      details:
        label: Details
        description : Description
        factorLevels: Levels
        no-factorLevels-provided: No factor levels provided
fr:
  component:
    factor:
      details:
        label: Détails
        description : Description
        factorLevels: Niveaux de facteurs associés
        no-factorLevels-provided: Aucun niveau de facteur associé

</i18n>