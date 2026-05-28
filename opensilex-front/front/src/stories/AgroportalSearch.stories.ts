import AgroportalSearch from '../components/common/external-references/agroportal/AgroportalSearch.vue';

export default {
  title: 'Components/AgroportalSearch',
  component: AgroportalSearch,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { AgroportalSearch },
  setup() { return { args }; },
  template: '<AgroportalSearch v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
