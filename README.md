# zprobe

Probe and signal handler for servers running inside K8s clusters.

## Features

### Readiness probe

A readiness probe is exposed at endpoint `GET /ready`.

The response status code will be `200` if the forge server reached `ServerStartedEvent`, `500` otherwise.
The response body is always empty.

### Liveness probe

A liveness probe is exposed at endpoint `GET /live`.

The response status code will be `200` if the server completed any ticks in last 5 seconds, `500` otherwise.
The response body is always empty.

### Graceful shutdown

When received `SIGTERM`, zprobe will prevent default shutdown hook from shutting down the server, and will wait `GRACEFUL_PERIOD` seconds before shutting down the server. Notofications of the server going down will also be broadcasted to players connected to the server.

## Configuration

All configurations of zprobe are read from environmental variables.

### Probe listen port

Env: `ZPROBE_LISTEN_PORT`
Default: `35565`

The TCP port where the probe http server will listen on.

### Graceful period

Env: `ZPROBE_GRACEFUL_PERIOD`
Default: `30`

How many seconds to wait before terminating the server after SIGTERM received.
