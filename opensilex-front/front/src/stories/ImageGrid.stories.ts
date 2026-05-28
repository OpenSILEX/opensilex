import ImageGrid from '../components/visualization/ImageGrid.vue';

export default {
  title: 'Components/ImageGrid',
  component: ImageGrid,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ImageGrid },
  setup() { return { args }; },
  template: '<ImageGrid v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
