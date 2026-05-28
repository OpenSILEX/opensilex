import AgroportalUnitForm from '../components/variables/agroportal/AgroportalUnitForm.vue';

export default {
  title: 'Components/AgroportalUnitForm',
  component: AgroportalUnitForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { AgroportalUnitForm },
  setup() { return { args }; },
  template: '<AgroportalUnitForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
