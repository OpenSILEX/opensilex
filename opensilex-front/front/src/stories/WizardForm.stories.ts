import WizardForm from '../components/common/forms/WizardForm.vue';

export default {
  title: 'Components/WizardForm',
  component: WizardForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { WizardForm },
  setup() { return { args }; },
  template: '<WizardForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
