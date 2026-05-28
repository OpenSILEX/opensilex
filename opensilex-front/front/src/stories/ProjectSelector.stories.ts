import ProjectSelector from '../components/projects/ProjectSelector.vue';

export default {
  title: 'Components/ProjectSelector',
  component: ProjectSelector,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ProjectSelector },
  setup() { return { args }; },
  template: '<ProjectSelector v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
