import ImageLightBox from '../components/visualization/ImageLightBox.vue';

export default {
  title: 'Components/ImageLightBox',
  component: ImageLightBox,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ImageLightBox },
  setup() { return { args }; },
  template: '<ImageLightBox v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
