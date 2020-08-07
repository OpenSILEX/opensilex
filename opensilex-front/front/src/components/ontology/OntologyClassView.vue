<template>
  <div class="container-fluid">
    <opensilex-PageHeader :icon="icon" :title="$t(title)"></opensilex-PageHeader>

    <opensilex-PageActions>
      <template v-slot>
        <opensilex-CreateButton @click="showCreateForm()" label="OntologyClassView.add"></opensilex-CreateButton>
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
      </template>
    </opensilex-PageActions>

    <opensilex-PageContent>
      <template v-slot>
        <div class="row">
          <div class="col-md-6">
            <b-card>
              <opensilex-OntologyClassTreeView
                ref="classesTree"
                :rdfClass="rdfClass"
                @selectionChange="selected = $event"
                @editClass="showEditForm($event)"
                @createChildClass="showCreateForm($event)"
                @deleteClass="deleteClass($event)"
              ></opensilex-OntologyClassTreeView>
            </b-card>
          </div>
          <div class="col-md-6">
            <opensilex-OntologyClassDetail :selected="selected" />
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
export default class OntologyClassView extends Vue {
  $opensilex: any;

  @Prop()
  rdfClass;

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
      .getClass(data.uri)
      .then(http => {
        let form = http.response.result;
        this.classForm.getFormRef().setParentTypes(this.classesTree.getTree());
        this.classForm.showEditForm(form);
      });
  }

  deleteClass(data) {
    this.$opensilex
      .getService("opensilex.VueJsOntologyExtensionService")
      .deleteClass(data.uri)
      .then(http => {
        this.refresh();
      });
  }

  refresh() {
    this.classesTree.refresh();
  }
}
</script>

<style scoped lang="scss">
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
