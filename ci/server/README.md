# Budget Server Docker Container

# NOTE THIS DIRECTORY IS CURRENTLY UNEEDED AND UNUSED

You'll need to have pulled the jdk image from some container registry:

```shell
docker pull docker.io/eclipse-temurin:21
```

## CI Docker Image

See [Dockerfile](Dockerfile).

Everything you need to know should be in
the [GitHub action that builds and publishes the image](../../.github/workflows/publish-server-container.yml)
(builds and publishes the image) and
the [GitHub action that runs tests](../../.github/workflows/test-server.yml) (runs the container).

To test manually, create the image:

```shell
docker build -t budget-server .
```

Test it with this

```shell
docker run -d --rm --name budget-server -p 127.0.0.1:8085:8080 budget-server:latest # not working ATM
```

You can look at logs with

```shell
docker logs budget-server
```

The image is published by the [publish-server-container.yml](../../.github/workflows/publish-server-container.yml)
action.
