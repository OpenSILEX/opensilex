import ProvenanceView from '../components/data/ProvenanceView.vue';

export default {
  title: 'Components/ProvenanceView',
  component: ProvenanceView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ProvenanceView },
  setup() { return { args }; },
  template: '<ProvenanceView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
