import ToDoComponent from '../components/layout/ToDoComponent.vue';

export default {
  title: 'Components/ToDoComponent',
  component: ToDoComponent,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ToDoComponent },
  setup() { return { args }; },
  template: '<ToDoComponent v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
