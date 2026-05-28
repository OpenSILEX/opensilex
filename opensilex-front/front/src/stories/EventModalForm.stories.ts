import EventModalForm from '../components/events/form/csv/EventModalForm.vue';

export default {
  title: 'Components/EventModalForm',
  component: EventModalForm,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { EventModalForm },
  setup() { return { args }; },
  template: '<EventModalForm v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
