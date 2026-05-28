import AgroportalSearchFormPart from '../components/common/external-references/agroportal/wizard/AgroportalSearchFormPart.vue';

export default {
  title: 'Components/AgroportalSearchFormPart',
  component: AgroportalSearchFormPart,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { AgroportalSearchFormPart },
  setup() { return { args }; },
  template: '<AgroportalSearchFormPart v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
