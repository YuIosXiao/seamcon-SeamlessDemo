#!/usr/bin/env bash

adb -d shell "run-as de.uni_marburg.ds.seamlesslogger cat /data/data/de.uni_marburg.ds.seamlesslogger/databases/seamless_logger.db" > seamless_logger.db

echo Done.
