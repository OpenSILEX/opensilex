import OntologyPropertyDetail from '../components/ontology/property/OntologyPropertyDetail.vue';

export default {
  title: 'Components/OntologyPropertyDetail',
  component: OntologyPropertyDetail,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { OntologyPropertyDetail },
  setup() { return { args }; },
  template: '<OntologyPropertyDetail v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
