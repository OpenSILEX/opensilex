import OntologyCsvImporter from '../components/ontology/csv/OntologyCsvImporter.vue';

export default {
  title: 'Components/OntologyCsvImporter',
  component: OntologyCsvImporter,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { OntologyCsvImporter },
  setup() { return { args }; },
  template: '<OntologyCsvImporter v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
