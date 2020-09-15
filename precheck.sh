#!/usr/bin/env bash

sbt clean validate scalastyle coverage test it:test coverageReport
