Quizmaster
===============

[![Build Status](https://travis-ci.org/nymanjens/quizmaster.svg?branch=master)](https://travis-ci.org/nymanjens/quizmaster)

This is a web-app to run locally while conducting a quiz (in real life) with about 4 groups.

## Screenshot

![screenshot](screenshot.png "Screenshot")

## Installation

- Clone this repository or download the files
- Run following commands to get the app running:

    ```
    # refresh application secret
    sbt playUpdateSecret

    # Build application
    sbt dist

    # Deploy files
    cd /somewhere/you/want/the/files
    unzip .../target/universal/quizmaster-1.0-SNAPSHOT.zip
    mv quizmaster-1.0-SNAPSHOT/* .
    rm -d quizmaster-1.0-SNAPSHOT/

    # Create database tables
    bin/server -DdropAndCreateNewDb
    rm RUNNING_PID

    # Run application
    bin/server
    ```

## Configuration
- `conf/quiz.yml`:<br>
  Add your questions and answers here.
