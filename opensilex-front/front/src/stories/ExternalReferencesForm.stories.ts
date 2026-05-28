import ExternalReferencesForm from '../components/common/external-references/ExternalReferencesForm.vue';

export default {
  title: 'Components/ExternalReferencesForm',
  component: ExternalReferencesForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ExternalReferencesForm },
  setup() { return { args }; },
  template: '<ExternalReferencesForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
