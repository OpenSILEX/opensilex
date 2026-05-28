import GeometryCopy from '../components/common/views/GeometryCopy.vue';

export default {
  title: 'Components/GeometryCopy',
  component: GeometryCopy,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { GeometryCopy },
  setup() { return { args }; },
  template: '<GeometryCopy v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
