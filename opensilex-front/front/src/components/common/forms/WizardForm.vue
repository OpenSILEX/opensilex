<template>
  <b-modal ref="modalRef" :size="modalSize" :static="static" no-close-on-backdrop no-close-on-esc >
    <template v-slot:modal-title>
      <i>
        <slot name="icon">
          <opensilex-Icon :icon="icon" class="icon-title" />
        </slot>
        <span v-if="editMode">{{ $t(editTitle) }}</span>
        <span v-else>{{ $t(createTitle) }}</span>
      </i>
    </template>
    <b-form ref="formRef" v-if="form">
      <form-wizard
        title
        subtitle
        ref="wizardRef"
        shape="square"
        :class="{'single-wizard' : steps.length == 1}"
        color="#00a38d"
      >
        <tab-content v-for="(step, index) in steps" :key="index" v-bind:title="$t(step.title)">
          <component
            :ref="'step' + index"
            v-bind:is="step.component"
            :editMode="editMode"
            :form.sync="form"
          ></component>
        </tab-content>

        <template slot="footer" slot-scope="props">
          <footer class="modal-footer modal-footer-replacement">
            <div class="wizard-footer-left">
              <b-button
                variant="secondary"
                @click="hide"
              >{{$t('component.common.form-wizard.cancel')}}</b-button>
            </div>

            <div class="wizard-footer-right">
              <b-button-group>
                <b-button
                  variant="success"
                  v-if="props.activeTabIndex > 0"
                  @click="props.prevTab()"
                >{{$t('component.common.form-wizard.previous')}}</b-button>
                <b-button
                  v-if="!props.isLastStep"
                  class="greenThemeColor"
                  @click="nextStepHandler(props)"
                >{{$t('component.common.form-wizard.next')}}</b-button>
                <b-button
                  v-if="props.isLastStep"
                  @click="validate(props)"
                  class="greenThemeColor"
                >{{$t('component.common.form-wizard.done')}}</b-button>
              </b-button-group>
            </div>
          </footer>
        </template>
      </form-wizard>
    </b-form>
  </b-modal>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";

@Component
export default class WizardForm extends Vue {
  $opensilex: any;

  @Ref("modalRef") readonly modalRef!: any;
  @Ref("wizardRef") readonly wizardRef!: any;

  form = null;

  editMode = false;

  @Prop({ default: true })
  static;

  @Prop()
  steps;

  @Prop()
  editTitle;

  @Prop()
  createTitle;

  @Prop()
  icon;

  @Prop({ default: "md" })
  modalSize;

  @Prop()
  initForm: Function;

  @Prop()
  createAction: Function;

  @Prop()
  updateAction: Function;

  @Prop()
  nextStepAction: Function;

  showCreateForm() {
    this.form = this.initForm();
    this.editMode = false;
    this.modalRef.show();
    for (let i = 0; i < this.steps.length; i++) {
      if (this.$refs["step" + i]) {
        let step: any = this.$refs["step" + i][0];
        if (step.reset) {
          step.reset();
        }
      }
    }
    this.wizardRef?.reset();
  }

  showEditForm(form) {
    this.form = form;
    this.modalRef.show();
    this.editMode = true;
    for (let i = 0; i < this.steps.length; i++) {
      if (this.$refs["step" + i]) {
        let step: any = this.$refs["step" + i][0];
        if(step != undefined){
          if (step.reset) {
            step.reset();
          }
        }
      }
    }
    this.wizardRef?.tabs.forEach(tab => {
      tab.checked = true;
    });
    this.wizardRef?.navigateToTab(0);
  }

  hide() {
    this.modalRef.hide();
  }

  validateStep(props) {
    let step: any = this.$refs["step" + props.activeTabIndex][0];
    if (step.validate) {
      return step.validate().then(isValid => {
        return isValid;
      });
    } else {
      return Promise.resolve(true);
    }
  }

  nextStepHandler(props) {
    return this.validateStep(props)
      .then(isValid => {
        if (isValid && this.nextStepAction) {
          let currentStepComponent: any = this.$refs[
            "step" + props.activeTabIndex
          ][0];
          let nextStepComponent: any = this.$refs[
            "step" + (props.activeTabIndex + 1)
          ][0];
          let nextStepResult = this.nextStepAction(
            props.activeTabIndex,
            this.form,
            nextStepComponent,
            currentStepComponent
          );
          if (!(nextStepResult instanceof Promise)) {
            nextStepResult = Promise.resolve(nextStepResult);
          }

          return nextStepResult
            .then(shouldContinue => {
              if (isValid && shouldContinue !== false) {
                props.nextTab();
              }

              return isValid;
            })
            .catch(error => {
              return false;
            });
        } else {
          if (isValid) {
            props.nextTab();
          }
          return isValid;
        }
      })
      .catch(error => {
        console.error(error);
        return false;
      });
  }

  validate(props) {
    this.validateStep(props).then(isValid => {
      if (isValid) {
        let submitMethod: any = null;
        if (this.createAction) {
          submitMethod = this.createAction;
        }

        let successEvent = "onCreate";
        if (this.editMode) {
          if (this.updateAction) {
            submitMethod = this.updateAction;
          } else {
            submitMethod = null;
          }

          successEvent = "onUpdate";
        }

        let submitResult: any = null;
        if (submitMethod != null) {
          submitResult = submitMethod(this.form);
        }
        if (!(submitResult instanceof Promise)) {
          submitResult = Promise.resolve(submitResult);
        }
        submitResult
          .then(result => {
            this.$nextTick(() => {
              this.$emit(successEvent, submitResult);
              this.hide();
            });
          })
          .catch(console.error);
      }
    });
  }
}
</script>

<style scoped lang="scss">
::v-deep .modal-body {
  padding-top: 0;
  padding-bottom: 0;
}

::v-deep .modal-footer {
  display: none;
}

::v-deep .wizard-header {
  display: none;
}

::v-deep .wizard-tab-content {
  padding-top: 5px;
}

.modal-footer-replacement {
  position: absolute;
  display: block;
  width: 100%;
  left: 0;
  bottom: 0;
}

::v-deep .vue-form-wizard {
  padding-bottom: 50px;
}

.icon-title {
  margin-right: 5px;
}

::v-deep .single-wizard .wizard-progress-with-circle,
::v-deep .single-wizard .wizard-nav {
  display: none;
}
</style>;
