# Spring Boot Admin with Keycloak Authentication - Docker

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.x-green.svg)
![Keycloak](https://img.shields.io/badge/Keycloak-18.0.0-blue.svg)
![Docker](https://img.shields.io/badge/Docker-20.10.x-2496ED.svg)

This repository contains a Dockerized Spring Boot Admin server with Keycloak authentication integration for monitoring and managing Spring Boot applications.

## Features

- Spring Boot Admin server for monitoring Spring Boot applications
- Integrated Keycloak authentication for secure access
- Ready-to-use Docker configuration
- Health monitoring, metrics, logging, and environment management

## Prerequisites

- Docker 20.10+ and Docker Compose
- Java 17+ (for development)
- Configured docker-compose.yml with spring-boot services to monitor 
- Keycloak server included in docker-compose (Optional)

## Quick Start

1. Clone this repository:
   ```bash
   git clone https://github.com/prokval/spring-boot-admin-docker.git
   cd spring-boot-admin-docker
   ```

2. Build the Docker image:
   ```bash
   docker build -t spring-boot-admin .
   ```
   
3. Add spring-boot-admin to your docker-compose.yml and set up environment (see [Configuration](#configuration) section)

4. Start the service:
   ```bash
   docker-compose up -d spring-boot-admin
   ```

5. Access the applications:
   - Spring Boot Admin: http://localhost:38080


## Configuration

Add the following to services section of your docker-compose.yml file:
```yaml
  spring-boot-admin:
    image: spring-boot-admin:latest
    environment:
      - KEYCLOAK_AUTH_URL=http://localhost:80/auth # URL where Keycloak is accessible from your browser
      - KEYCLOAK_SERVER_URL=http://keycloak:8080/auth # keycloak here is the Keycloak's host in the docker network 
      - KEYCLOAK_REALM=<your-realm>
      - KEYCLOAK_CLIENT_ID=<your-client-id>
      - APP_HOST=<your-spring-boot-service-to-monitor>
    ports:
       - 38080:8080
```

If more than one service to monitor is needed:
```yaml
  spring-boot-admin:
    image: spring-boot-admin:latest
    environment:
      - KEYCLOAK_AUTH_URL=http://localhost:80/auth # URL where Keycloak is accessible from your browser
      - KEYCLOAK_SERVER_URL=http://keycloak:8080/auth # keycloak here is the Keycloak's host in the docker network 
      - KEYCLOAK_REALM=<your-realm>
      - KEYCLOAK_CLIENT_ID=<your-client-id>
    ports:
       - 38080:8080
    volumes:
      - ./configs/spring-boot-admin/:/app/config/
```

Put ./configs/spring-boot-admin/application.yml with the services definitions:
```yaml
spring.cloud.discovery.client.simple.instances:
  service1:
    - uri: http://service1:9080
      metadata:
        management:
          context-path: /actuator
  service2:
    - uri: http://service2:9080
      metadata:
        management:
          context-path: /actuator
```


### Environment Variables

| Variable                 | Description                                                                                                                      | Default                       |
|--------------------------|----------------------------------------------------------------------------------------------------------------------------------|-------------------------------|
| `KEYCLOAK_AUTH_URL`      | URL to redirect in browser for auth                                                                                              | `http://localhost:18080/auth` |
| `KEYCLOAK_SERVER_URL`    | URL to verify Bearer tokens. If the Keycloak is not in Docker, could be same as KEYCLOAK_AUTH_URL                                | `http://localhost:18080/auth` |
| `KEYCLOAK_REALM`         | Realm in Keycloak                                                                                                                | `master`                      |
| `KEYCLOAK_CLIENT_ID`     | Client ID in Keycloak                                                                                                            | `admin-cli`                   |
| `APP_HOST`               | Host of Spring Boot service to monitor. If you need more than one service to monitor, override application.yml file instead      | `localhost`                   |
| `SPRING_PROFILES_ACTIVE` | If you don't need Keycloak authorization for Spring Boot Admin, override default value of this var with 'keycloak' word excluded | `prod,keycloak`               |



## Adding Applications to Monitor

Configure your Spring Boot applications to register with this admin server by adding these properties:


## License

This project is licensed under the [MIT License](LICENSE.txt).

## Acknowledgements

- [Spring Boot Admin](https://github.com/codecentric/spring-boot-admin)
- [Keycloak](https://www.keycloak.org/)
- [Docker](https://www.docker.com/)