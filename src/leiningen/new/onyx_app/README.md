# {{app-name}}

An Onyx 0.8.3 application that does distributed things. This project has been populated with a sample job and some basic Onyx idioms to make development easier to use.

## Usage

### Development
During development, you can use the `(with-test-env)` macro to start and stop
Onyx environments on your local machine. You should use the same macro for your
development as well as automated testing.

There are four main parts of the dev/test environment.


1. The `let` binding sets up a dev-mode configuration. This is a plain clojure
map describing [peer-config](http://www.onyxplatform.org/cheat-sheet.html#/peer-config)
and [environment-config](http://www.onyxplatform.org/cheat-sheet.html#/env-config). Any of these
defaults can be overriden. The 0-arity version of load-config loads the default
map suitable for development.

    ```
    (let [id (java.util.UUID/randomUUID)
          config (load-config)
          env-config (assoc (:env-config config) :onyx/id id)
          peer-config (assoc (:peer-config config) :onyx/id id)]
    ```
2. The `(with-test-env)` macro will setup and teardown a full fledged
test environment for running your job (or jobs) locally. It handles the
different failure conditions of Onyx, as well as terminating when it
recieves an interupt (Ctrl-C). The macro is [*anaphoric*](http://letoverlambda.com/index.cl/guest/chap6.html)
,meaning that it creates a development environment and binds it to a
user defined symbol. In this case, that symbol is `test-env`.
[Read More](https://onyx-platform.gitbooks.io/onyx/content/doc/user-guide/testing-onyx-jobs.html#automatic-resource-clean-up)

    ```
    (with-test-env [test-env [5 env-config peer-config]]
    ```

3. `(build-job :dev)` is a convinient idiom we use to build onyx jobs. Most of
the time, you're going to be testing your workflows on a static dataset during
development time, but switch to a SQL DB or a Kafka queue when in production. We
signal this switch with a `mode` keyword. This keyword is then used to build
and modify the job map. In this case, switching from `:dev` to `:prod` switches
out the stock input (reading lines from a file) and output (core.async channel)
in favor of using a Kafka queue and a MySQL db for reading and writing.

4. The rest of the inner body of `with-test-env` is devoted entirely to
submitting the job and collecting the results. We provide the function
`(get-core-async-channels)` to return a map of channels allocated at the start
of the job, in this case it's only one that we care about, `:write-lines`.
That will have an associated channel that we can use to collect our output!

Try to run the test, and watch the output of onyx.log in your project root.

### Production
Running onyx in production just requires building an uberjar and running
the `{{app-name}}.launcher.launch_prod_peers` function with an `onyx_id` and a `npeers`
argument.

`onyx_id` will essentially namespace this particular peer to a cluster.
This allows you to run multiple groups of onyx environments with the same
zookeeper ensemble.

`npeers` will create multiple peers (units to execute tasks in a workflow) on
the same JVM. It is recommended you have 1 peer per core.

You are also going to want to verify that the peer-config in `resources/config.edn`
is correct. Specifically that `:zookeeper/address` contains a ZK server in your
ensemble, and that your `:onyx.messaging/bind-addr` is an address reachable
by any other peer in the cluster.

For our case right now, `:zookeeper/address` is set to use "zk:2181", and since
we only have one physical node running several peers, localhost is reachable by
all the peers in the cluster.

Submitting a job to a production cluster is exactly the same as in the
development example. You generate your job (this time with `:prod` instead of
`:dev`), and call `submit-job`. This time your peer config will come from
'resources/config.edn' instead of the anaphoric macro though.
{{#docker}}
## Docker Compose
With docker-compose, we can demonstrate a real example application. 

### Problem
We want to get data from [Meetup.com](www.meetup.com) into a MySQL database after doing some transformations. Meetup.com provides an event stream at `stream.meetup.com`. You can use `curl` to check it out by running this common

    curl -i https://stream.meetup.com/2/open_events
    
The basic structure is just a nested JSON map. We want to be able to process this as `edn` segments in Onyx, and eventually store our results in MySQl.

### Approach
The architecture is quite straightforward. We will be using 5 containers in a docker-compose network.

1. ZooKeeper
2. MySql
3. Kafka
	- [Kafka](http://kafka.apache.org/) durable queue.
4. Peer
	- A container running (default 6) Onyx peers. You can change the `NPEERS` in the docker-compose.yml file
5. KafkaCat
	- A small utility container to `curl` data from meetup.com into the `meetups` Kafka topic.
	
KafkaCat will forward data to a topic in Kafka (meetups) that will store it indefinitely. We can then submit jobs to the ZooKeeper container, and the Peer's will pick them up and start running them. We can then output our data using the Onyx SQL plugin to MySQL. 

### Execution
**First** steps are to make the scripts in the `script/` directory executable. Run the following two commands 

    `chmod +x script/*`
    `chmod +x script/kafka-meetup-streamer/*`
This allows us to use the convenience build scripts, and allows docker to include the scripts that launch our peers and meetup.com streamer. 

**Next**, we will build the example app. Out of the box the lein template includes all that you would need to stream from meetup.com->Kafka->Onyx->MySQL. Run the build script.
    `./script/build.sh`
    
**Once** That finishes, you can run `docker-compose up` to download, configure and launch the rest of the containers. Once that completes (it will take some time), you will have a fully configured Onyx cluster. 

{{/docker}}
## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
