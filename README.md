Quizmaster
===============

This is a web-app to run locally while conducting a quiz (in real life) with about 4 groups.

## Screenshot

![screenshot](screenshot.png "Screenshot")

## Installation

- Download the [latest release](https://github.com/nymanjens/quizmaster/releases)
- Unpack the archive and open a terminal in the unpacked folder
- Run `bin/server` (UNIX) or `bin/server.bat` (Windows)
- Browse to http://localhost:9000

## Configuration

- `conf/application.conf`:
    - `play.i18n.langs`: The client language, `"en"` and `"nl"` are supported
    - `app.development.loadDummyData`: If this is true: Start with 4 teams already configured (Team A, Team B, Team C, Team D)

- `conf/quiz/quiz-config.yml`:<br>
  Add your questions and answers here.

## Play

### Game modes

This quiz can be played in different ways, which will inform the quiz settings (http://localhost:9000/app/quizsettings).

- With every team using a phone/tablet/laptop connected to `http://<your-ip-address>:9000/app/teamcontroller`
    - To choose from multiple-choice questions. Note that these are automatically scored.
    - To fill in textual answers. Note that these are automatically scored.
- With up to 4 physical game controllers
    - To choose from multiple-choice questions (in the quiz settings, choose "Answer bullet type" = Arrows). Note that these are automatically scored.
    - To stop the timer and give an answer
    - To indicate that a team has written down the answer on a paper so the quizmaster can continue to the answer when every team has done so
- Via an external form, e.g. Google Forms (in the quiz settings, choose "Answer bullet type" = Characters)
    - Go trough the round(s) first with "Show answers" = No (in the quiz settings)
    - When everyone has submitted their answer, go back and repeate with "Show answers" = Yes

### Shortcuts

- **Quiz navigation**
  - `left/right`: Go to the previous/next step of the question
  - `alt + left/right`: Go to the previous/next question
  - `alt + shift + left/right`: Go to the previous/next round
- **Tools during question**
  - `spacebar`: Pause and resume the timer
  - `shift + r`: Play the current audio file from the beginning
  - `shift + =/-`: Add/subtract 30 seconds from the current timer
  - `alt + enter`: Toggle enlarged image (if there is a visible image)
  - `a`: toggle the answer to be visible in the master view (http://localhost:9000/app/master)
- **Scoring**
  - `1/2/3/.../0`: Increase the score of team 1, 2, ..., 10 by one point
  - `shift + 1/2/3/.../0`: Decrease the score of team 1, 2, ..., 10 by one point
