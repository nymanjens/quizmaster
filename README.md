Quizmaster
===============

This is a web-app to run locally while conducting a quiz (in real life) with about 4 groups.

## Screenshot

![screenshot](screenshot.png "Screenshot")

## Installation

- Clone this repository or download the files
- Run following commands to get the app running:

    ```
    # Build application
    sbt dist

    # Unzip target/universal/quizmaster-1.0-SNAPSHOT.zip to another folder
    cd path/to/unzipped/snapshot/zip

    # Run application
    bin/server
    ```

## Configuration
- `conf/application.conf`:<br>
  Set the language and some server settings here.

- `conf/quiz-config.yml`:<br>
  Add your questions and answers here.
