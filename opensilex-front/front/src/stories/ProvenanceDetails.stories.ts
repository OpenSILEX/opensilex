import ProvenanceDetails from '../components/data/ProvenanceDetails.vue';

export default {
  title: 'Components/ProvenanceDetails',
  component: ProvenanceDetails,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ProvenanceDetails },
  setup() { return { args }; },
  template: '<ProvenanceDetails v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
