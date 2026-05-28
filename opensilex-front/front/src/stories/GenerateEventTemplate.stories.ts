import GenerateEventTemplate from '../components/events/form/csv/GenerateEventTemplate.vue';

export default {
  title: 'Components/GenerateEventTemplate',
  component: GenerateEventTemplate,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { GenerateEventTemplate },
  setup() { return { args }; },
  template: '<GenerateEventTemplate v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
