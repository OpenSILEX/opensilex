import ProvenanceSelector from '../components/data/ProvenanceSelector.vue';

export default {
  title: 'Components/ProvenanceSelector',
  component: ProvenanceSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ProvenanceSelector },
  setup() { return { args }; },
  template: '<ProvenanceSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
