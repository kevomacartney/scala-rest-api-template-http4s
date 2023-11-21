## {{ cookiecutter.project_name }}

{{ cookiecutter.project_short_description }}

## Getting Started
This project is built using [sbt](https://www.scala-sbt.org/). You can use any IDE that supports sbt projects however intelliJ is highly recommended.


### Prerequisites
Make sure you have the following installed on your machine:
- Java 17
- Docker

### Preparing your environment
- Clone the repo
- Run `sbt compile` to compile the project
- Run `sbt test` to run the tests
- Run `sbt run` to run the project
- Run `sbt assembly` to create a fat jar

### Running the app in docker
- Run `docker build -t {{ cookiecutter.project_slug }}:local .` to build the docker image
- Run `docker run -p 8080:8080 -e APP_NAME={{ cookiecutter.project_slug }} -e ENVIRONMENT=dev` to run the app in docker
  - Run `docker run -p 8080:8080 -e APP_NAME={{ cookiecutter.project_slug }} -e ENVIRONMENT=dev -e LOG_LEVEL=DEBUG` to run the app in docker with debug logs.

### Running the app in docker-compose (TODO - Add docker-compose)
- Run `docker-compose up` to run the app in docker-compose
  - Run `docker-compose up -d` to run the app in docker-compose in detached mode
  - Run `docker-compose up --build` to run the app in docker-compose and rebuild the image
  - Run `docker-compose up --build -d` to run the app in docker-compose in detached mode and rebuild the image

## Admin endpoints
- Healthcheck: `http://localhost:5002/alive.txt`
- Metrics: `http://localhost:5002/metrics`
- Prometheus: `http://localhost:5002/prometheus` (TODO: Add prometheus metrics)

## Who do I talk to?
team: Aleph
slack: #core-aleph