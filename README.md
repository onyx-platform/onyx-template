# Onyx Application Template

A Leiningen template for creating Onyx apps.

### Usage

To get an application template with a sample job:

```text
lein new onyx-app my-app-name
```

### Options
The template also supports the following options

```
lein new onyx-app my-app-name -- +docker
```

`+docker` adds a `Dockerfile` and `docker-compose.yaml` that will demonstrate
launching an Onyx cluster with a Zookeeper image.

## License

Copyright © 2016 Distributed Masonry

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
