import EventCsvForm from '../components/events/form/csv/EventCsvForm.vue';

export default {
  title: 'Components/EventCsvForm',
  component: EventCsvForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { EventCsvForm },
  setup() { return { args }; },
  template: '<EventCsvForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
