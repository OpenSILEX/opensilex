import EventForm from '../components/events/form/EventForm.vue';

export default {
  title: 'Components/EventForm',
  component: EventForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { EventForm },
  setup() { return { args }; },
  template: '<EventForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
