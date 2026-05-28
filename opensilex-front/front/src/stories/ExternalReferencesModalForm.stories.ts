import ExternalReferencesModalForm from '../components/common/external-references/ExternalReferencesModalForm.vue';

export default {
  title: 'Components/ExternalReferencesModalForm',
  component: ExternalReferencesModalForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ExternalReferencesModalForm },
  setup() { return { args }; },
  template: '<ExternalReferencesModalForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
