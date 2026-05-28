import IconForm from '../components/common/forms/IconForm.vue';

export default {
  title: 'Components/IconForm',
  component: IconForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { IconForm },
  setup() { return { args }; },
  template: '<IconForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
