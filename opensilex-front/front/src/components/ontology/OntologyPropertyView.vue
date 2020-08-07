<template>
  <div class="container-fluid">
    <opensilex-PageHeader :icon="icon" :title="$t(title)"></opensilex-PageHeader>

    <opensilex-PageActions>
      <template v-slot>
        <opensilex-CreateButton @click="showCreateForm()" label="OntologyPropertyView.add"></opensilex-CreateButton>
        <opensilex-ModalForm
          ref="propertyForm"
          component="opensilex-OntologyPropertyForm"
          createTitle="OntologyPropertyView.add"
          editTitle="OntologyPropertyView.update"
          :initForm="initForm"
          @onCreate="refresh()"
          @onUpdate="refresh()"
          modalSize="lg"
          :icon="icon"
        ></opensilex-ModalForm>
      </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <div class="row">
          <div class="col-md-6">
            <b-card>
              <opensilex-OntologyPropertyTreeView
                ref="propertiesTree"
                :domain="domain"
                @selectionChange="selected = $event"
                @editProperty="showEditForm($event)"
                @createChildProperty="showCreateForm($event)"
                @deleteProperty="deleteProperty($event)"
              ></opensilex-OntologyPropertyTreeView>
            </b-card>
          </div>
          <div class="col-md-6">
            <opensilex-OntologyPropertyDetail :selected="selected" />
          </div>
        </div>
      </template>
    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import { OntologyService } from "opensilex-core/index";
import OWL from "../../ontologies/OWL";

@Component
export default class OntologyPropertyView extends Vue {
  $opensilex: any;

  @Prop()
  domain;

  @Prop()
  title;

  @Prop()
  icon;

  selected = null;

  @Ref("propertyForm") readonly propertyForm!: any;
  @Ref("propertiesTree") readonly propertiesTree!: any;

  ontologyService: OntologyService;

  created() {
    this.ontologyService = this.$opensilex.getService(
      "opensilex-core.OntologyService"
    );
  }

  initForm(form) {
    form.parent = this.parentURI;
    if (this.parentURI != null) {
      form.type = null;
    }
  }

  parentURI;

  showCreateForm(parentURI?) {
    this.parentURI = parentURI;
    this.propertyForm.showCreateForm();
  }

  showEditForm(data) {
    this.ontologyService.getProperty(data.uri, data.type).then(http => {
      let propertyFormComponent = this.propertyForm.getFormRef();
      propertyFormComponent.setParentPropertiesTree(
        this.propertiesTree.getTree()
      );
      propertyFormComponent.setDomain(this.domain);
      let form = http.response.result;
      if (OWL.hasParent(form.parent)) {
        form.type = null;
      } else if(OWL.isDatatypeProperty(form.type)) {
          form.type = OWL.DATATYPE_PROPERTY_URI;
      }else if(OWL.isObjectTypeProperty(form.type)) {
          form.type = OWL.OBJECT_PROPERTY_URI;
      }
      this.propertyForm.showEditForm(form);
    });
  }

  deleteProperty(data) {
      this.ontologyService.deleteProperty(data.uri, data.type)
        .then(() => {
            this.refresh();
        });
  }

  refresh() {
    this.propertiesTree.refresh();
  }
}
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
  OntologyPropertyView:
    add: Create property
    update: Update property

fr:
  OntologyPropertyView:
    add: Créer une propriété
    update: Mettre à jour la propriété
</i18n>
