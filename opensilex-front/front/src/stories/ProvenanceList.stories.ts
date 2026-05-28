import ProvenanceList from '../components/data/ProvenanceList.vue';

export default {
  title: 'Components/ProvenanceList',
  component: ProvenanceList,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ProvenanceList },
  setup() { return { args }; },
  template: '<ProvenanceList v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
