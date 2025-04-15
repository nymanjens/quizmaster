# Quizmaster

[![CI Status](https://github.com/nymanjens/quizmaster/actions/workflows/ci.yml/badge.svg)](https://github.com/nymanjens/quizmaster/actions)

A web-app for conducting a quiz, including a page for players to enter their answers. Lots of
question types are suported, which are configured in a YAML file.

## Screenshot

![screenshot](screenshot.png "Screenshot")

## Installation

### From prebuilt release in zip file (recommended)

- Install Java 11 (JDK 11) on your server
- Download "Binaries (compiled files) with demo configuration" from the [latest
  release](https://github.com/nymanjens/quizmaster/releases)
- Unpack the archive and open a terminal in the unpacked folder
- Run `bin/server` (UNIX) or `bin/server.bat` (Windows)
- Browse to http://localhost:9000

### Using a prebuilt docker image

- Run the following commands:

```
git clone https://github.com/nymanjens/quizmaster.git
cd quizmaster
docker-compose --file=docker-compose-prebuilt.yml up
```

- Browse to http://localhost:9000

### Building and running your own release with Docker


- Run the following commands:

```
git clone https://github.com/nymanjens/quizmaster.git
cd quizmaster
docker-compose --file=docker-compose-build-locally.yml up
```
- Run this command
- This way, users can force Docker to recreate containers using the --force-recreate option.
```
- sudo docker-compose -f docker-compose-prebuilt.yml up --force-recreate
```


- Browse to http://localhost:9000

## Configuration

- `conf/quiz/quiz-config.yml`:<br>
  Configure your quiz here (questions, choices, answers, images, ...). The existing one in the
  release is a demo config that contains most of the options.

## Play

### How to set up

Follow these steps to host a quiz:

- Make your own quiz by editing `conf/quiz/quiz-config.yml`. You can test your quiz by starting a
  local server with it (see the installation section above)
- Host the server somewhere accessible to all players
- During a quiz, share the link to your server with all players. You
  can go to the same page and unlock the master controls via the padlock icon (if you configured a
  `masterSecret` in `quiz-config.yml`). The important pages during the quiz:
  - The player's answer submission page: This is what players use to input their answers.
  - The quiz page: This is the screen to show to all players. It shows the questions, player
    scores and plays audio and video.
  - The master page: This is a screen only for the quizmaster. It allows you to score player
    answers and generally control the quiz flow.

### Shortcuts

- **Quiz navigation**
  - `left/right`: Go to the previous/next step of the question
  - `alt + left/right`: Go to the previous/next question
  - `alt + shift + left/right`: Go to the previous/next round
- **Tools during question**
  - `spacebar`: Pause and resume the timer
  - `shift + r`: Play the current audio/video file from the beginning
  - `shift + -/o`: Subtract 30 seconds from the current timer
  - `shift + =/+/p`: Add 30 seconds from the current timer
  - `alt + enter`: Toggle enlarged image (if there is a visible image)
  - `a`: toggle the answer to be visible in the master view (http://localhost:9000/app/master)

