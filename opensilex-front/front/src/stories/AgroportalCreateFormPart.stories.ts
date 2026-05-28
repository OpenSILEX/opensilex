import AgroportalCreateFormPart from '../components/common/external-references/agroportal/wizard/AgroportalCreateFormPart.vue';

export default {
  title: 'Components/AgroportalCreateFormPart',
  component: AgroportalCreateFormPart,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { AgroportalCreateFormPart },
  setup() { return { args }; },
  template: '<AgroportalCreateFormPart v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
