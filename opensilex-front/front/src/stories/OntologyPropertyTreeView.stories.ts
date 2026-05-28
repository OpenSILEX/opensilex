import OntologyPropertyTreeView from '../components/ontology/property/OntologyPropertyTreeView.vue';

export default {
  title: 'Components/OntologyPropertyTreeView',
  component: OntologyPropertyTreeView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { OntologyPropertyTreeView },
  setup() { return { args }; },
  template: '<OntologyPropertyTreeView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
