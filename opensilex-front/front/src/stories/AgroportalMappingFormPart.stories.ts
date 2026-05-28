import AgroportalMappingFormPart from '../components/common/external-references/agroportal/wizard/AgroportalMappingFormPart.vue';

export default {
  title: 'Components/AgroportalMappingFormPart',
  component: AgroportalMappingFormPart,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { AgroportalMappingFormPart },
  setup() { return { args }; },
  template: '<AgroportalMappingFormPart v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
