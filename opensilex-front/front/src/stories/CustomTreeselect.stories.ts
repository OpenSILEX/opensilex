import CustomTreeselect from '../components/common/forms/CustomTreeselect.vue';

export default {
  title: 'Components/CustomTreeselect',
  component: CustomTreeselect,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { CustomTreeselect },
  setup() { return { args }; },
  template: '<CustomTreeselect v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
