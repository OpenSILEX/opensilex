import OntologyCsvTemplateGenerator from '../components/ontology/csv/OntologyCsvTemplateGenerator.vue';

export default {
  title: 'Components/OntologyCsvTemplateGenerator',
  component: OntologyCsvTemplateGenerator,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { OntologyCsvTemplateGenerator },
  setup() { return { args }; },
  template: '<OntologyCsvTemplateGenerator v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
