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
                :small="false"
              ></opensilex-EditButton>
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
              label="component.factor.comment"
              :value="factor.comment"
            ></opensilex-StringView>
          </template>
        </opensilex-Card>

        <opensilex-Card
          label="component.skos.ontologies-references-label"
          icon="fa#globe-americas"
        >
          <template v-slot:rightHeader>
            <div class="ml-4">
              <opensilex-InteroperabilityButton
                v-if="
                  user.hasCredential(
                    credentials.CREDENTIAL_FACTOR_MODIFICATION_ID
                  )
                "
                :small="false"
                label="component.skos.update"
                @click="skosReferences.show()"
              ></opensilex-InteroperabilityButton>
            </div>
          </template>
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
            <b-table
              v-if="factor.factorLevels.length != 0"
              striped
              hover
              small
              responsive
              sort-icon-left
              :items="factor.factorLevels"
              :fields="factorLevelFields"
            >
              <template v-slot:head(uri)="data">{{ $t(data.label) }}</template>
              <template v-slot:head(name)="data">{{ $t(data.label) }}</template>
              <template v-slot:head(category)="data">{{
                $t(data.label)
              }}</template>
              <template v-slot:head(comment)="data">{{
                $t(data.label)
              }}</template>
            </b-table>
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
      @onUpdate="update"
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

  @Ref("skosReferences") readonly skosReferences!: any;

  @Ref("factorForm") readonly factorForm!: any;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
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
        comment: null,
        exactMatch: [],
        closeMatch: [],
        broader: [],
        narrower: [],
        factorLevels: [],
      };
    },
  })
  factor: any;

  factorLevelFields: any[] = [
    {
      key: "uri",
      label: "component.factorLevel.uri",
      sortable: false,
    },
    {
      key: "name",
      label: "component.factorLevel.name",
      sortable: true,
    },
    {
      key: "comment",
      label: "component.factorLevel.comment",
      sortable: false,
    },
  ];

  update(factor) {
    this.$emit("onUpdate", factor);
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