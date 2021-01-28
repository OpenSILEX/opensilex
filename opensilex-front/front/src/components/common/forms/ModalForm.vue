<template>
  <b-modal
    ref="modalRef"
    :class="(modalSize === 'full' ? 'full-screen-modal-form' : '')"
    @ok.prevent="validate"
    @hide="$emit('hide')"
    :size="modalSize"
    :static="true"
    no-close-on-backdrop
    no-close-on-esc
  >
    <template v-slot:modal-ok>{{$t('component.common.ok')}}</template>
    <template v-slot:modal-cancel>{{$t('component.common.cancel')}}</template>

    <template class="mt-1" v-slot:modal-header>
      <b-row class="mt-1" style="width:100%">
        <b-col cols="10" >
          <i>
            <h4> 
              <slot name="icon">
                <opensilex-Icon :icon="icon" class="icon-title" />
              </slot>
              <span v-if="editMode">{{ $t(editTitle) }}</span>
              <span v-else>{{ $t(createTitle) }}</span>
          
            </h4>
          </i>
        </b-col>
        <b-col cols="1">
          <opensilex-HelpButton
            v-if="tutorial && !editMode"
              class="ml-2"
              variant="outline-info"
              label="component.tutorial.label"  
              @click="getFormRef().tutorial()"
          ></opensilex-HelpButton> 
        </b-col>
        <b-col cols="1">
        <!-- Emulate built in modal header close button action -->
        <button type="button" class="close" @click="modalRef.hide()" data-dismiss="modal" aria-label="Close">
          <span aria-hidden="true">&times;</span>
        </button>
        </b-col>
      </b-row> 
    </template>


    <ValidationObserver ref="validatorRef">
        <component ref="componentRef" v-bind:is="component" :editMode="editMode" :form.sync="form">
            <slot name="customFields" v-bind:form="form" v-bind:editMode="editMode"></slot>
        </component>
    </ValidationObserver>
  </b-modal>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class ModalForm extends Vue {
  $opensilex: any;

  @Ref("modalRef") readonly modalRef!: any;
  @Ref("validatorRef") readonly validatorRef!: any;

  editMode = false;

  form = {};

  @Prop({ default: false })
  tutorial :boolean;

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
            if (result !== false && result !== undefined) {
              this.creationOrUpdateMessage();
            }
            this.$nextTick(() => {
              if (result !== false) {
                this.$emit(successEvent, submitResult);
              }
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
    this.editMode = false;

    this.$nextTick(() => {
      this.form = this.getFormRef().getEmptyForm();
      let form = this.initForm(this.form);
      if (form) {
        this.form = form;
      }
      this.modalRef.show();

      this.validatorRef.reset();
      if (this.getFormRef().reset) {
        this.getFormRef().reset();
      }
    });
  }

  showEditForm(form) {
    this.editMode = true;

    this.$nextTick(() => {
      this.form = form;
      this.modalRef.show();
      this.validatorRef.reset();
      if (this.getFormRef().reset) {
        this.getFormRef().reset();
      }
    });
  }

  hide() {
    this.modalRef.hide();
  }
}
</script>

<style scoped lang="scss">
::v-deep .full-screen-modal-form > .modal-dialog {
  max-width: 95%;
}
</style>;