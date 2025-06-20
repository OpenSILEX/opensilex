<template>
  <opensilex-ModalForm
      ref="modalForm"
      modalSize="lg"
      :tutorial="false"
      component="opensilex-AnnotationForm"
      createTitle="Annotation.add"
      editTitle="Annotation.edit"
      icon="fa#vials"
      :createAction="create"
      :updateAction="update"
  ></opensilex-ModalForm>

</template>


<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import ModalForm from "../../common/forms/ModalForm.vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";
import {AnnotationsService} from "opensilex-core/api/annotations.service";
import {AnnotationCreationDTO, AnnotationUpdateDTO} from 'opensilex-core/index';
import AnnotationForm from "./AnnotationForm.vue";


@Component
export default class AnnotationModalForm extends Vue {

  $opensilex: OpenSilexVuePlugin;
  $store: any;
  service: AnnotationsService;
  $i18n: any;
  targets : Array<string>;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  @Ref("modalForm") readonly modalForm!: ModalForm<AnnotationForm, AnnotationCreationDTO, AnnotationUpdateDTO>;

  created() {
    this.service = this.$opensilex.getService("opensilex.AnnotationsService");
  }

  showCreateForm(targets : Array<string> ) {
    this.targets = targets;
    this.modalForm.showCreateForm();
  }

  showEditForm(form : AnnotationUpdateDTO) {
    this.modalForm.showEditForm(form);
  }

  @Prop()
  editMode;

  create(annotation : AnnotationCreationDTO) {

    annotation.targets = this.targets;

    return this.service.createAnnotation(annotation).then((http: HttpResponse<OpenSilexResponse<string>>) => {
      let message = this.$i18n.t("Annotation.name") + " " + http.response.result + " " + this.$i18n.t("component.common.success.creation-success-message");
      this.$opensilex.showSuccessToast(message);
      this.$emit("onCreate", http.response.result.toString());
    }).catch((error) => {
      if (error.status == 409) {
        this.$opensilex.errorHandler(error,"Annotation "+annotation.uri+" : "+this.$i18n.t("Annotation.already-exist"));
      } else {
        this.$opensilex.errorHandler(error);
      }
    });
  }

  update(annotation: AnnotationUpdateDTO) {

    return this.service.updateAnnotation(annotation).then(() => {
      let message = this.$i18n.t("Annotation.name") + " " + annotation.uri+ " " + this.$i18n.t("component.common.success.update-success-message");
      this.$opensilex.showSuccessToast(message);
      this.$emit("onUpdate", annotation.uri);
    }).catch((error) => {
      this.$opensilex.errorHandler(error);
    });
  }
}

</script>
