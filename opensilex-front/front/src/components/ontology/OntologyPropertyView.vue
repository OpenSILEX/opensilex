<template>
  <div class="row">
    <div class="col-md-6">
      <b-card>
        <div class="button-zone">
          <opensilex-CreateButton  v-if="user.isAdmin()" @click="showCreateForm()" label="OntologyPropertyView.add"></opensilex-CreateButton>
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
        </div>
        <opensilex-OntologyPropertyTreeView
          ref="propertiesTree"
          :domain="rdfType"
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

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import { OntologyService } from "opensilex-core/index";
import OWL from "../../ontologies/OWL";

@Component
export default class OntologyPropertyView extends Vue {
  $opensilex: any;

   /**
   * Return the current connected user
   */
  get user() {
    return this.$store.state.user;
  }

  @Prop()
  rdfType;

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
    if (OWL.hasParent(form.parent)) {
      form.rdf_type = null;
    } else if (OWL.isDatatypeProperty(form.rdf_type)) {
      form.rdf_type = OWL.DATATYPE_PROPERTY_URI;
    } else if (OWL.isObjectTypeProperty(form.rdf_type)) {
      form.rdf_type = OWL.OBJECT_PROPERTY_URI;
    }
    form.domain_rdf_type = this.rdfType;
  }

  parentURI;

  showCreateForm(parentURI?) {
    this.parentURI = parentURI;
    let propertyFormComponent = this.propertyForm.getFormRef();
    propertyFormComponent.setParentPropertiesTree(
      this.propertiesTree.getTree()
    );
    propertyFormComponent.setDomain(this.rdfType);
    this.propertyForm.showCreateForm();
  }

  showEditForm(data) {
    this.ontologyService.getProperty(data.uri, data.rdf_type, this.rdfType).then(http => {
      let propertyFormComponent = this.propertyForm.getFormRef();
      propertyFormComponent.setParentPropertiesTree(
        this.propertiesTree.getTree()
      );
      propertyFormComponent.setDomain(this.rdfType);
      let form = http.response.result;
      if (OWL.hasParent(form.parent)) {
        form.rdf_type = null;
      } else if (OWL.isDatatypeProperty(form.rdf_type)) {
        form.rdf_type = OWL.DATATYPE_PROPERTY_URI;
        form.range = this.$opensilex.getDatatype(form.range).uri;
      } else if (OWL.isObjectTypeProperty(form.rdf_type)) {
        form.rdf_type = OWL.OBJECT_PROPERTY_URI;
        form.range = this.$opensilex.getObjectType(form.range).uri;
      }
      this.propertyForm.showEditForm(form);
    });
  }

  deleteProperty(data) {
    this.ontologyService.deleteProperty(data.uri, data.rdf_type).then(() => {
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
