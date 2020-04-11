# Nostalgia

> _Yes, man I miss when programming was that pure..._
>
> -- _A comment in [r/ProgrammerHumor](https://www.reddit.com/r/ProgrammerHumor/comments/eyczz4/optical_illusion/fgh3fel?utm_source=share&utm_medium=web2x) about assembler code_

Jokes aside, this is a simulator of an imaginary computer system.

## Build Instructions

You need jdk 8 or above.

Run `gradlew build` to build it.

## Game Instructions

> _Heh! You consider this a game!?_

You code programs for the imaginary Nostalgia Execution Environment!
Maybe some people like this...

For example, to get [moving_dot.nos](/src/dist/sample/moving_dot.nos) working, you would do:

```bash
$ ./bin/as -o kernel.bin ./sample/moving_dot.nos
$ ./bin/nosemu kernel.bin
```

or `./bin/nostalgia ./sample/moving_dot.nos`, but this way you don't get the assembled code.

For more information, see the README.md that is bundled with the build.
Alternatively, click [here](/src/dist/README.md); it's the same document.

_The following is a modified version of [moving_dot.nos](/src/dist/sample/moving_dot.nos)_

![Proof of Concept](./sample.gif)

## Assets Used

* Super authentic [VGA-ROM font](https://github.com/spacerace/romfont)
