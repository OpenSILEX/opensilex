import OntologyClassDetail from '../components/ontology/class/OntologyClassDetail.vue';

export default {
  title: 'Components/OntologyClassDetail',
  component: OntologyClassDetail,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { OntologyClassDetail },
  setup() { return { args }; },
  template: '<OntologyClassDetail v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
