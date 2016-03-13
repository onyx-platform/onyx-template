# {{app-name}}

An Onyx application that does distributed things. This project has been populated with a sample job and some basic Onyx idioms to make development easier to use.

## Usage

### Development
During development, you can use the `(with-test-env)` macro to start and stop
Onyx environments on your local machine. You should use the same macro for your
development as well as automated testing.

There are four main parts of the dev/test environment. You can see these in the namespace {{app-name}}.jobs.meetup-job-test.

1. The `let` binding sets up a dev-mode configuration. This is a plain clojure
map describing [peer-config](http://www.onyxplatform.org/docs/cheat-sheet/#/peer-config)
and [environment-config](http://www.onyxplatform.org/docs/cheat-sheet/#/env-config). Any of these
defaults can be overridden. The 0-arity version of load-config loads the default
map suitable for development.

    ```
    (let [id (java.util.UUID/randomUUID)
          config (load-config)
          env-config (assoc (:env-config config) :onyx/id id)
          peer-config (assoc (:peer-config config) :onyx/id id)]
    ```
2. The `(with-test-env)` macro will setup and tear-down a full fledged
test environment for running your job (or jobs) locally. It handles the
different failure conditions of Onyx, as well as terminating when it
receives an interrupt (Ctrl-C). The macro is [*anaphoric*](http://letoverlambda.com/index.cl/guest/chap6.html),
meaning that it creates a development environment and binds it to a user
defined symbol. In this case, that symbol is `test-env`.
[Read More](https://onyx-platform.gitbooks.io/onyx/content/doc/user-guide/testing-onyx-jobs.html#automatic-resource-clean-up)

    ```
    (with-test-env [test-env [5 env-config peer-config]]
    ```

3. `(build-job :dev)` is a convenient idiom we use to build onyx jobs. Most of
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

#### Reloading Code in Development

`(clojure.tools.namespace.repl/refresh)` can be used with this setup to refresh your project.
Many Clojure editors also have convenient keybindings for this!

Then try to run the test via your editor, or in the repl e.g. `(clojure.test/run-tests '{{app-name}}.jobs.meetup-job-test)`.
Ensure you `tail -F` the output of onyx.log in your project root, to watch out for any issues that might pop up.

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

{{#docker?}}
## Docker Compose
With docker-compose, we can demonstrate a real example application.

### Problem
We want to get data from [Meetup.com](www.meetup.com) into a MySQL database after doing some transformations. Meetup.com provides an event stream at `stream.meetup.com`. You can use `curl` to check it out by running:

```
    curl -i https://stream.meetup.com/2/open_events
```

The basic structure is just a nested JSON map. We want to be able to process this as `edn` segments in Onyx, and eventually store our results in MySQL.

### Approach
The architecture is quite straightforward. We will be using 5 containers in a docker-compose network.

1. ZooKeeper
2. MySQL
3. Kafka
	- [Kafka](http://kafka.apache.org/) durable queue.
4. Peer
	- A container running (default 6) Onyx peers. You can change the `NPEERS` in the docker-compose.yml file
5. KafkaCat
	- A small utility container to `curl` data from meetup.com into the `meetups` Kafka topic.

KafkaCat will forward data to a topic in Kafka (meetups) that will store it indefinitely. We can then submit jobs to the ZooKeeper container, and the Peer's will pick them up and start running them. We can then output our data using the Onyx SQL plugin to MySQL.

### Prerequisites

1. [Docker ToolBox](https://www.docker.com/products/docker-toolbox/)
2. On Windows or Mac OS X, setup a local VM with VirtualBox, see [get started](https://docs.docker.com/machine/get-started/)
3. Java 8

### Execution

##### Make sure to set the Docker Machine env

If you are using docker-machine, make sure to setup your environment:
`eval "$(docker-machine env default)"`

where default is the name of your docker-machine box.

##### Building the Cluster

First we will build the example app. Out of the box the lein template includes all that you would need to stream from meetup.com->Kafka->Onyx->MySQL. Run the build script (with Java 8).

`./script/build.sh`

**Once** That finishes, you can run `docker-compose up` to download, configure and launch the rest of the containers. Once that completes (it will take some time), you will have a fully configured Onyx cluster. This cluster (of one physical node, and default 6 peers) is fully able to receive jobs. Let's try to submit one.

##### Setup MySQL database

In order to persist this to the `:sql/table` specified in the `:write-lines` catalog entry (:recentMeetups), we need to first create the table and load a schema in MySQL.

Connect to the MySQL instance with: :

`mysql -h $(echo $DOCKER_HOST|cut -d ':' -f 2|sed "s/\/\///g") -P3306 -uroot`.

Then, use the following SQL to setup the table and schema.

```
    use meetup;
    create table recentMeetups (id int PRIMARY KEY AUTO_INCREMENT,
                                groupId VARCHAR(32),
                                groupCity VARCHAR(32),
                                category VARCHAR(32));
```

Now with everything configured, we can finally submit to the cluster.

##### Submitting the Job

**Submitting** a job is done using the `onyx.api/submit-job` function. It takes a [peer config](http://www.onyxplatform.org/cheat-sheet.html#/peer-config) and a job description map (such as the one generated by `{{app-name}}.jobs.meetup-job/build-job`), and submits the job to the specified Onyx cluster.

`({{app-name}}.jobs.meetup-job/-main)` will build and submit a job to the cluster for you. What this job does is use the `:extract-meetup-info` catalog entry to walk through the value from meetup.com, and transform it to a segment of the shape

    {"groupId" ... "groupCity" ... "category" ...}

Note, you will need to start your repl with the ZOOKEEPER environment variable set to your docker host, or edit `resources/config.edn` to configure the zookeeper string.

You can also submit the job via the command-line as follows:
`ZOOKEEPER=$(echo $DOCKER_HOST|cut -d ':' -f 2|sed "s/\/\///g") lein run -m {{app-name}}.jobs.meetup-job`

### Results
You should now see results streaming into MySQL when you select from the table in MySQL:

```
mysql> use meetup;
mysql> select * from recentMeetups;

|   10 |  18430202 | Paris                 | outdoors/adventure       |
|   11 |   1437441 | Raleigh               | outdoors/adventure       |
|   12 |  10587252 | Boston                | parents/family           |
|   13 |  18571863 | Biel                  | socializing              |
|   14 |   1573627 | San Francisco         | music                    |
|   15 |   1527539 | New York              | singles                  |
|   16 |   1339645 | New York              | socializing              |
|   17 |  19321878 | London                | <null>                   |
|   18 |   1381446 | Sydney                | language/ethnic identity |
|   19 |   1442441 | New York              | singles                  |
|   20 |   1753892 | Barcelona             | career/business          |
|   21 |   1389390 | Denver                | new age/spirituality     |
```


### Tips
1. Whenever you make a code change, you need to re-run the `script/build.sh` to remake your docker container with the new jar.
2. Use `docker-compose rm` to delete the MySQL/Kafka datastores and start fresh
3. You can make your own kafkacat containers to pull your own data into Kafka
4. When you submit your job with `submit-job`, a UUID will be returned. You can use this UUID with `kill-job` if you're at the repl.
5. If you're having issues with the kafkacat container connecting to meetup.com, there seems to be a docker-machine bug that won't pass on the correct DNS information on container creation. This is resolved by restarting your docker host, usually `docker-machine restart <name>`

{{/docker?}}
## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
