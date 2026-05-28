import ProjectDetails from '../components/projects/ProjectDetails.vue';

export default {
  title: 'Components/ProjectDetails',
  component: ProjectDetails,
  argTypes: {},
} as const;

const Template = (args: any) => ({
  components: { ProjectDetails },
  setup() { return { args }; },
  template: '<ProjectDetails v-bind="args" />',
});

export const Default = Template.bind({});
Default.args = {};
