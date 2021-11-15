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
          <opensilex-StringFilter
              :filter.sync="filter"
              @update="updateFilter()"
              placeholder="OntologyClassView.search"
          ></opensilex-StringFilter>

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
      <div>
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
import OntologyClassTreeView from "./OntologyClassTreeView.vue";

@Component
export default class OntologyClassView extends Vue {
  $opensilex: any;
  $store: any;

  private filter: any = "";

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
  @Ref("classesTree") readonly classesTree!: OntologyClassTreeView;

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
        let message = this.$i18n.t("OntologyClassView.the-type") + " " + data.name + this.$i18n.t("component.common.success.delete-success-message");
        this.$opensilex.showSuccessToast(message);

        this.selected = undefined;
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
    this.classesTree.refresh(this.selected,this.filter);
  }


  updateFilter() {
    this.refresh();
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
    the-type: The type
    add: Create type
    update: Update type
    search: Search and select a type
fr:
  OntologyClassView:
    the-type: Le type
    add: Créer un type
    update: Mettre à jour le type
    search: Rechercher et sélectioner un type
</i18n>

