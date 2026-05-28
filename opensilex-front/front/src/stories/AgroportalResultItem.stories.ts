import AgroportalResultItem from '../components/common/external-references/agroportal/AgroportalResultItem.vue';

export default {
  title: 'Components/AgroportalResultItem',
  component: AgroportalResultItem,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { AgroportalResultItem },
  setup() { return { args }; },
  template: '<AgroportalResultItem v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
