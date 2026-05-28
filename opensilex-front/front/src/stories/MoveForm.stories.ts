import MoveForm from '../components/events/form/MoveForm.vue';

export default {
  title: 'Components/MoveForm',
  component: MoveForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { MoveForm },
  setup() { return { args }; },
  template: '<MoveForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
