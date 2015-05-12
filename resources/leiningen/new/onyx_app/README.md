# {{app-name}}

An Onyx 0.6.0 application that does distributed things.

## Usage

### Start the Development Mode

Load up `env/dev/user.clj`. Evaluate the `go` function.

### Launch the Sample Job in Development

Run the `test-job` function in `test/{{app-name}}/sample_job_test.clj`. Be sure to have evaluated the `go` function beforehand to start your development environment.

### Reset the Development Mode

Load up `env/dev/user.clj`. Evaluate the `reset` function.

### Stop the Development Mode

Load up `env/dev/user.clj`. Evaluate the `stop` function.

### Production Mode Peers

Launch the `src/{{app-name}}/launcher/launch_prod_peers.clj` main function, giving it an Onyx ID.

### Launch the Sample Job in Production

Launch the `src/{{app-name}}/launcher/submit_prod_sample_job.clj` main function, giving it an Onyx ID.

## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
