import MethodView from '../components/variables/views/MethodView.vue';

export default {
  title: 'Components/MethodView',
  component: MethodView,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { MethodView },
  setup() { return { args }; },
  template: '<MethodView v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
