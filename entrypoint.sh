#!/bin/sh
# entrypoint.sh

set -e

until curl -f http://rabbit:15672; do
  >&2 echo "Rabbit is unavailable - sleeping"
  sleep 1
done

>&2 echo "Rabbit is up - executing command"

exec $@
