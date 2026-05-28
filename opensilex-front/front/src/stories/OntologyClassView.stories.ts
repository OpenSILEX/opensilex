import OntologyClassView from '../components/ontology/class/OntologyClassView.vue';

export default {
  title: 'Components/OntologyClassView',
  component: OntologyClassView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { OntologyClassView },
  setup() { return { args }; },
  template: '<OntologyClassView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
