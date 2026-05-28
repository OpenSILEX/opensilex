import DeleteButton from '../components/common/buttons/DeleteButton.vue';

export default {
  title: 'Components/DeleteButton',
  component: DeleteButton,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { DeleteButton },
  setup() { return { args }; },
  template: '<DeleteButton v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
