import ProvenanceDescription from '../components/data/ProvenanceDescription.vue';

export default {
  title: 'Components/ProvenanceDescription',
  component: ProvenanceDescription,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ProvenanceDescription },
  setup() { return { args }; },
  template: '<ProvenanceDescription v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
