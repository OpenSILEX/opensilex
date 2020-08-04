<template>
  <b-modal
    ref="modalRef"
    :class="(modalSize == 'full' ? 'full-screen-modal-form' : '')"
    @ok.prevent="validate"
    :size="modalSize"
    :static="true"
    no-close-on-backdrop
    no-close-on-esc
  >
    <template v-slot:modal-ok>{{$t('component.common.ok')}}</template>
    <template v-slot:modal-cancel>{{$t('component.common.cancel')}}</template>

    <template v-slot:modal-title>
      <i>
        <slot name="icon">
          <opensilex-Icon :icon="icon" class="icon-title" />
        </slot>
        <span v-if="editMode">{{ $t(editTitle) }}</span>
        <span v-else>{{ $t(createTitle) }}</span>
      </i>
    </template>

    <ValidationObserver ref="validatorRef">
      <component ref="componentRef" v-bind:is="component" :editMode="editMode" :form.sync="form"></component>
    </ValidationObserver>
  </b-modal>
</template>

<script lang="ts">
import { Component, Prop, Ref, PropSync } from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {
  OpenSilexResponse
} from "opensilex-security/HttpResponse";

@Component
export default class ModalForm extends Vue {
  $opensilex: any;

  @Ref("modalRef") readonly modalRef!: any;
  @Ref("validatorRef") readonly validatorRef!: any;

  editMode = false;

  form = {};

  @Prop()
  component;

  @Prop()
  editTitle;

  @Prop()
  createTitle;

  @Prop()
  icon;

  @Prop({ default: "md" })
  modalSize;

  @Prop({
    type: Function,
    default: function(f) {}
  })
  initForm: Function;

  @Prop()
  createAction: Function;

  @Prop()
  updateAction: Function;

  @Prop({
    type: [String, Function],
    default: "component.common.element"
  })
  successMessage: string | Function;

  validate() {
    this.validatorRef.validate().then(isValid => {
      if (isValid) {
        let submitMethod: any = this.getFormRef()["create"];
        if (this.createAction) {
          submitMethod = this.createAction;
        }
        let successEvent = "onCreate";
        if (this.editMode) {
          if (this.updateAction) {
            submitMethod = this.updateAction;
          } else {
            submitMethod = this.getFormRef()["update"];
          }

          successEvent = "onUpdate";
        }

        let submitResult: any = submitMethod(this.form);
        if (!(submitResult instanceof Promise)) {
          submitResult = Promise.resolve(submitResult);
        }
        submitResult
          .then(result => {
            this.creationOrUpdateMessage();
            this.$nextTick(() => {
              this.$emit(successEvent, submitResult);
              this.hide();
            });
          })
          .catch(console.error);
      }
    });
  }

  creationOrUpdateMessage() {
    let successMessage;
    if (typeof this.successMessage == "function") {
      successMessage = this.successMessage(this.form);
    } else {
      successMessage = this.$i18n.t(this.successMessage);
    }
    if (this.editMode) {
      successMessage =
        successMessage +
        this.$i18n.t("component.common.success.update-success-message");
    } else {
      successMessage =
        successMessage +
        this.$i18n.t("component.common.success.creation-success-message");
    }
    this.$opensilex.showSuccessToast(successMessage);
  }
  getFormRef(): any {
    return this.$refs.componentRef;
  }

  showCreateForm() {
    this.form = this.getFormRef().getEmptyForm();
    let form = this.initForm(this.form);
    if (form) {
      this.form = form;
    }
    this.modalRef.show();
    this.editMode = false;
    this.validatorRef.reset();
    if (this.getFormRef().reset) {
      this.getFormRef().reset();
    }
  }

  showEditForm(form) {
    this.form = form;
    this.modalRef.show();
    this.editMode = true;
    this.validatorRef.reset();
    if (this.getFormRef().reset) {
      this.getFormRef().reset();
    }
  }

  hide() {
    this.modalRef.hide();
  }
}
</script>

<style scoped lang="scss">
.icon-title {
  margin-right: 5px;
}
::v-deep .full-screen-modal-form > .modal-dialog {
  max-width: 95%;
}
</style>;
