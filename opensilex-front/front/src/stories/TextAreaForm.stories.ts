import TextAreaForm from '../components/common/forms/TextAreaForm.vue';

export default {
  title: 'Components/TextAreaForm',
  component: TextAreaForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { TextAreaForm },
  setup() { return { args }; },
  template: '<TextAreaForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
