# Nostalgia

> _Yes, man I miss when programming was that pure..._
>
> -- _A comment in [r/ProgrammerHumor](https://www.reddit.com/r/ProgrammerHumor/comments/eyczz4/optical_illusion/fgh3fel?utm_source=share&utm_medium=web2x) about assembler code_

Jokes aside, this is a simulator of an imaginary computer system.

## Build Instructions

You need jdk 8 or above.

Run `gradlew build` to build it or `gradlew run` to run after building.

You probably want to supply the path to the kernel also. For example:

```bash
$ ./gradlew run --args ./src/main/resources/moving_dot.nos
```

## Game Instructions

> _Heh! You consider this a game!?_

You code assembler! Maybe some people like this...

![Proof of Concept](./sample.gif)

## Assets Used

* Super authentic [VGA-ROM font](https://github.com/spacerace/romfont)
