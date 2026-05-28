import OntologyTypesView from '../components/ontology/OntologyTypesView.vue';

export default {
  title: 'Components/OntologyTypesView',
  component: OntologyTypesView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { OntologyTypesView },
  setup() { return { args }; },
  template: '<OntologyTypesView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
