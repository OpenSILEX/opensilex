import DatafileProvenanceSelector from '../components/data/DatafileProvenanceSelector.vue';

export default {
  title: 'Components/DatafileProvenanceSelector',
  component: DatafileProvenanceSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DatafileProvenanceSelector },
  setup() { return { args }; },
  template: '<DatafileProvenanceSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
