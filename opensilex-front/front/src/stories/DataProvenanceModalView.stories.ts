import DataProvenanceModalView from '../components/data/DataProvenanceModalView.vue';

export default {
  title: 'Components/DataProvenanceModalView',
  component: DataProvenanceModalView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DataProvenanceModalView },
  setup() { return { args }; },
  template: '<DataProvenanceModalView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
