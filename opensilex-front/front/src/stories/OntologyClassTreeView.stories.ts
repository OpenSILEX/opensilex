import OntologyClassTreeView from '../components/ontology/class/OntologyClassTreeView.vue';

export default {
  title: 'Components/OntologyClassTreeView',
  component: OntologyClassTreeView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { OntologyClassTreeView },
  setup() { return { args }; },
  template: '<OntologyClassTreeView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
