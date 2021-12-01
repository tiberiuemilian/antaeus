## CI with GitHub Actions & GitHub containers registry

For CI (continuous integration) I configured GitHub Actions.

You can find the configuration in the following folder:
antaeus\.github\workflows\antaeus-github-actions.yml

CI configuration contains building steps for:
* building the app
* running all tests
* pack the solution in a docker container image
* publish the docker image in my personal GitHub container registry

The docker image is built with the help of Jib project initiated by Google as one of the Google Container Tools:
* https://github.com/GoogleContainerTools/jib
* https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin

It's very fast when you build/re-build docker images because it separates your application into multiple layers, splitting dependencies from classes.
You don’t have to wait for Docker to rebuild your entire Java application - just deploy the layers that changed.
It is also optimized for generating small containers.

![Antaeus CI with GitHub Actions](/readme/github-actions.PNG "Antaeus CI with GitHub Actions")

![Antaeus container registry with GitHub Container Registry](/readme/github-registry.PNG "Antaeus container registry with GitHub Container Registry")
