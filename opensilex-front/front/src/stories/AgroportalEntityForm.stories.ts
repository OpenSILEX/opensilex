import AgroportalEntityForm from '../components/variables/agroportal/AgroportalEntityForm.vue';

export default {
  title: 'Components/AgroportalEntityForm',
  component: AgroportalEntityForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { AgroportalEntityForm },
  setup() { return { args }; },
  template: '<AgroportalEntityForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
