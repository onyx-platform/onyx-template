# {{app-name}}

An Onyx application that does distributed things. This project has been
populated with a sample job and some basic Onyx idioms to make development
a bit easier to get started.
{{#docker?}}Build docker image: `lein do clean, uberjar; docker build -t peerimage .` {{/docker?}}

### Running the app

Run the following commands from the project's root directory:

```
lein do clean, ubjerjar

# Start the peers
> java -cp target/peer.jar {{app-name}}.core start-peers 4 -c resources/config.edn
Starting peer-group
Starting env
Starting peers
Attempting to connect to Zookeeper @ 127.0.0.1:2188
Started peers. Blocking forever.

# In a separate terminal, submit a job to the peers
> java -cp target/peer.jar {{app-name}}.core submit-job "basic-job" -c resources/config.edn
...
Successfully submitted job:  #uuid "6a1f7400-f5d3-3d49-ebde-487f6964ab8e"
Blocking on job completion...

```

Please note that this is only an example for submitting the job into the Onyx peergroup. There's no simply way to submit data to the `:in` on the core.async basic-job & it won't complete after submission.

In order to leverage a "real" job, use an [Onyx plugin](https://github.com/onyx-platform/onyx-kafka) to read from external datasource such as [Kafka.](https://kafka.apache.org/)

### Running tests

```
lein test {{app-name}}.jobs.basic-test
Starting Onyx test environment
Stopping Onyx test environment

Ran 1 tests containing 2 assertions.
0 failures, 0 errors.
```
