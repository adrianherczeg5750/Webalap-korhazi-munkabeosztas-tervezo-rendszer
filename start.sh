#!/bin/bash

if ! command -v docker &> /dev/null
then
  exit 1
fi

if ! docker info > /dev/null 2>&1
then
  exit 1
fi
docker compose down
docker compose up --build