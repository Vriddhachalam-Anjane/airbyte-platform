#! /bin/bash

set -ex

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

if ! command -v timeout &> /dev/null
then
    echo "timeout could not be found, installing it"
    brew install coreutils
fi

NEW_HASH="$( git rev-parse HEAD )"

git checkout 0.50.37
git pull --no-rebase

"$SCRIPT_DIR"/../../gradlew -p "$SCRIPT_DIR"/../.. generate-docker

cd "$SCRIPT_DIR"/../..
VERSION=0.50.37 docker compose -f "$SCRIPT_DIR"/../../docker-compose.yaml up &

sleep 75
VERSION=0.50.37 docker compose down

git stash
git checkout $NEW_HASH
"$SCRIPT_DIR"/../../gradlew -p "$SCRIPT_DIR"/../.. generate-docker

VERSION=0.50.37 docker compose -f "$SCRIPT_DIR"/../../docker-compose.yaml up
