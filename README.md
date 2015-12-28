# Onyx Application Template

A Leiningen template for creating Onyx 0.8.2 apps.

### Usage

To get an application template with a sample job:

```text
lein new onyx-app my-app-name
```
Then read the instructions on the `README.md` in your fresh project.

### Options
The template also supports the following options

``text
lein new onyx-app my-app-name -- +docker
``
For a containerized onyx example that will stream meetup.com data through a
single node kafka cluster, a simple onyx workflow, and write to a MySQL databse.

``text
lein new onyx-app my-app-name -- +metrics
``
For adding the ability to instrument any step in a workflow with latency and
throughput metrics.


## License

Copyright © 2015 Distributed Masonry

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
