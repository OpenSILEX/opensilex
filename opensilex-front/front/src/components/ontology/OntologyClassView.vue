<template>
  <div class="row">
    <div class="col-md-6">
      <b-card>
        <div class="button-zone">
          <opensilex-CreateButton v-if="user.isAdmin()" @click="showCreateForm()" label="OntologyClassView.add"></opensilex-CreateButton>
          <opensilex-ModalForm
            ref="classForm"
            component="opensilex-OntologyClassForm"
            createTitle="OntologyClassView.add"
            editTitle="OntologyClassView.update"
            :initForm="initForm"
            @onCreate="refresh()"
            @onUpdate="refresh()"
            modalSize="lg"
            :icon="icon"
          ></opensilex-ModalForm>
        </div>
        <opensilex-OntologyClassTreeView
          ref="classesTree"
          :rdfType="rdfType"
          @selectionChange="selected = $event"
          @editClass="showEditForm($event)"
          @createChildClass="showCreateForm($event)"
          @deleteRDFType="deleteRDFType($event)"
        ></opensilex-OntologyClassTreeView>
      </b-card>
    </div>
    <div class="col-md-6 ">
      <div class="sticky">
      <opensilex-OntologyClassDetail
        :rdfType="rdfType"
        :selected="selected"
        @onDetailChange="refresh()"
      />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import { OntologyService } from "opensilex-core/index";
import OWL from "../../ontologies/OWL";

@Component
export default class OntologyClassView extends Vue {
  $opensilex: any;
  $store: any;

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

  @Ref("classForm") readonly classForm!: any;
  @Ref("classesTree") readonly classesTree!: any;

  initForm(form) {
    form.parent = this.parentURI;
  }

  parentURI;

  showCreateForm(parentURI?) {
    this.parentURI = parentURI;
    this.classForm.getFormRef().setParentTypes(this.classesTree.getTree());
    this.classForm.showCreateForm();
  }

  showEditForm(data) {
    this.$opensilex
      .getService("opensilex.VueJsOntologyExtensionService")
      .getRDFType(data.uri, this.rdfType)
      .then(http => {
        let form = http.response.result;
        this.classForm.getFormRef().setParentTypes(this.classesTree.getTree());
        this.classForm.showEditForm(form);
      });
  }

  deleteRDFType(data) {
    this.$opensilex
      .getService("opensilex.VueJsOntologyExtensionService")
      .deleteRDFType(data.uri)
      .then(http => {
        this.refresh();
      });
  }

  private langUnwatcher;
  mounted() {
    this.langUnwatcher = this.$store.watch(
      () => this.$store.getters.language,
      lang => {
        this.refresh();
      }
    );
  }

  beforeDestroy() {
    this.langUnwatcher();
  }

  refresh() {
    this.classesTree.refresh(this.selected);
  }
}
</script>

<style scoped lang="scss">
   div.sticky {
        position: -webkit-sticky; /* Safari */
        position: sticky;
        top: 0;
    }
</style>


<i18n>
en:
  OntologyClassView:
    add: Create type
    update: Update type

fr:
  OntologyClassView:
    add: Créer un type
    update: Mettre à jour le type
</i18n>
