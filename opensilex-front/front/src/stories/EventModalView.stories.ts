import EventModalView from '../components/events/view/EventModalView.vue';

export default {
  title: 'Components/EventModalView',
  component: EventModalView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { EventModalView },
  setup() { return { args }; },
  template: '<EventModalView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
