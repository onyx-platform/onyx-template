#!/usr/bin/env bash
sleep 5
curl -i http://stream.meetup.com/2/open_events | kafkacat -P -b "$BROKER_LIST" -t meetup
