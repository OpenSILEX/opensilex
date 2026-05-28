import AgroportalResults from '../components/common/external-references/agroportal/AgroportalResults.vue';

export default {
  title: 'Components/AgroportalResults',
  component: AgroportalResults,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { AgroportalResults },
  setup() { return { args }; },
  template: '<AgroportalResults v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
