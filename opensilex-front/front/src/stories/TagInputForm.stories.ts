import TagInputForm from '../components/common/forms/TagInputForm.vue';

export default {
  title: 'Components/TagInputForm',
  component: TagInputForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { TagInputForm },
  setup() { return { args }; },
  template: '<TagInputForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
