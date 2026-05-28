import ExternalReferencesDetails from '../components/common/external-references/ExternalReferencesDetails.vue';

export default {
  title: 'Components/ExternalReferencesDetails',
  component: ExternalReferencesDetails,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ExternalReferencesDetails },
  setup() { return { args }; },
  template: '<ExternalReferencesDetails v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
