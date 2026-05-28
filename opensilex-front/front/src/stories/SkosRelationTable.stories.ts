import SkosRelationTable from '../components/common/external-references/skos/SkosRelationTable.vue';

export default {
  title: 'Components/SkosRelationTable',
  component: SkosRelationTable,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { SkosRelationTable },
  setup() { return { args }; },
  template: '<SkosRelationTable v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
