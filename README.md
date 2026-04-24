<h1>HeXO Renderer</h1>
HeXO Renderer is a small discord bot written in kotlin for rendering rectilinear <a href="https://hexo.did.science">HeXO</a> notation within discord.

## Invite
Add HeXO Renderer to your server or user account: [Invite HeXO Renderer](https://discord.com/oauth2/authorize?client_id=1496214901713014894).

## Table of Contents
<!-- TOC -->
  * [Invite](#invite)
  * [Table of Contents](#table-of-contents)
  * [Rectilinear Notation](#rectilinear-notation)
  * [Features](#features)
    * [Command `hexo`](#command-hexo)
    * [Message command](#message-command)
    * [Command `game`](#command-game)
  * [Contributing](#contributing)
  * [Build](#build)
<!-- TOC -->

## Rectilinear Notation
Rectilinear notation is a notation for encoding board states used by the community to quickly write down formations in text messages. 
However, it can become hard to reason about for more complex states. To solve this issue, this bot provides a way to render this notation as image directly from within discord.

The general syntax has the following characters:

| Character | Meaning                                               |
|-----------|-------------------------------------------------------|
| x         | Player 1 (Red / Yellow)                               |
| o         | Player 2 (Blue)                                       |
| .         | Empty cell                                            |
| -         | Two empty cells (equivalent to ..)                    |
| /         | New row. A newline character can also be used instead |

It is also possible to use numbers to indicate that number of empty cells, so the following are equivalent: `x...x`, `x-.x`, `x3x`.

The following image is produced by the notation `x-x/o.o//x`:

![example 1](assets/example_1.png)

Or for a more complex example:
```
. . x
 . o o
  . x x x o
   x x . o .
```
![example 2](assets/example_2.png)

It is also possible to highlight cells. For player characters you can use the uppercase letter and for empty cells `!`:

| Default Character | Highlighted Variant |
|-------------------|---------------------|
| x                 | X                   |
| o                 | O                   |
| .                 | !                   |

Additionally, winning rows (6 or more in a row) are highlighted automatically.

`...!/...x/oxxxxxx/.oox/ooox/.o.o`

![example 3](assets/example_3.png)


## Features
### Command `hexo`
Accepts heXO notation as parameter and renders it as image. Example usage:

![example slash command](assets/example_slash_command.png)

### Message command
It is also possible to render notation in existing messages. To do so, right-click the message and select `Apps > HeXO Renderer > Render HeXO notation in message`:

![example message command](assets/example_message_command.png)

### Command `game`
Another feature is reviewing games from https://hexo.did.science in discord. Simply use the `game` slash command and provide a game id (or link) to the game you want to review:

## Contributing
Contributions are very welcome.
If you have a small or medium improvement, feel free to open a PR directly.
For larger changes, please open an issue first so we can align on scope and approach.

## Build
If you want to build (and run) the bot yourself, you can run `./gradlew discord:shadowJar`, which will create `discord/build/libs/discord-1.0.0-all.jar`. 
You can then run this jar with `java -jar path-to.jar`.

> [!NOTE]
> You need a JDK 21 (or higher) installed to build the jar. For running it a JRE is sufficient.

Alternatively you can use the `Dockerfile` to build a docker image that you can run.
In both cases the environment variable `TOKEN` has to be set to the bot token of your discord bot.
