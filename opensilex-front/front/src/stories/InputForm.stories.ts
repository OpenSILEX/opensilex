import InputForm from '../components/common/forms/InputForm.vue';

export default {
  title: 'Components/InputForm',
  component: InputForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { InputForm },
  setup() { return { args }; },
  template: '<InputForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
