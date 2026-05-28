import EditButton from '../components/common/buttons/EditButton.vue';

export default {
  title: 'Components/EditButton',
  component: EditButton,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { EditButton },
  setup() { return { args }; },
  template: '<EditButton v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
