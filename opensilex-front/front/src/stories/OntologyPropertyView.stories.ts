import OntologyPropertyView from '../components/ontology/property/OntologyPropertyView.vue';

export default {
  title: 'Components/OntologyPropertyView',
  component: OntologyPropertyView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { OntologyPropertyView },
  setup() { return { args }; },
  template: '<OntologyPropertyView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
