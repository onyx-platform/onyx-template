# {{app-name}}

An Onyx 0.7.4 application that does distributed things.

## Usage

### Start the Development Mode

Load up `env/dev/user.clj`. Evaluate the `go` function, which takes a single integer argument for how many peers to launch.

### Launch the Sample Job in Development

Create tests in `test/jobs/`. Be sure to have evaluated the `go` function beforehand to start your development environment before running tests.

### Reset the Development Mode

Load up `env/dev/user.clj`. Evaluate the `reset` function.

### Stop the Development Mode

Load up `env/dev/user.clj`. Evaluate the `stop` function.

### Production Mode Peers

Launch the `src/{{app-name}}/launcher/launch_prod_peers.clj` main function, giving it an Onyx ID. Optionally, a Dockerfile has been created at the root of the project to package this up into an uberjar for a Java 8 Ubuntu 14 environment.

## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
