# Nostalgia Execution Environment and Toolchain

> I'll do my best to keep this as up to date as possible.
>
> And it seems like I will be doing this in reverse order...

## Disassembler

When you do (or something similar):

```bash
./bin/dis ./a.out
```

the disassembler will try disassemble the supplied list of binary files.

## Assembler

When you do (or something similar): 

```bash
./bin/as ./sample/feed_char.nos -I ../../../
```

the assembler will try to assemble your code into a binary file.

If you supply multiple files, they will be assembled as if they were concatenated into one in the supplied order.

`-I` needs a path after it, and this affects which paths are searched for `.INCLUDE` and `.IMPORT` directives.
By default, it will search in the current working directory __and not the current file being assembled!__

For the list of instructions, please consult the [Nostalgia Instruction Reference](./IREF.md).

Here is a list of directives supported by the assembler.
All directives start with a dot and are case insensitive.
Using one that's not supported would cause the assembler to crash.

 Name       | Example                       | Description
------------|-------------------------------|------------
 `.SET`     | `.SET VIDMEM, 0x1000`         | Defines a value that is expanded lazily. Crash on redefinition.
 `.UNSET`   | `.UNSET VIDMEM`               | Removes a previous `.SET` definition, if any.
 `.ALIGN`   | `.ALIGN 2`                    | Aligns the encoding buffer to the desired number (of bytes) by padding zeros.
 `.ZERO`    | `.ZERO 0x4010`                | Fills from the current virtual address to the desired address with zeros.
 `.EMIT`    | `.EMIT 0xFE`                  | Emits the desired byte in place. Can take multiple arguments.
 `.ORG`     | `.ORG 0x4000`                 | Sets the current virtual address. This does not affect where in memory the assembled code is loaded. It does affect labels.
 `.INCLUDE` | `.INCLUDE ./dat.nos`          | Includes a file (C's `#include`)
 `.IMPORT`  | `.IMPORT ./utils/memcpy.nos`  | Includes a file once even when used more than once.

## Registers

There is 1 zero register, 15 general purpose registers, and an instruction pointer.

The zero register is mapped to `%R0`.
Reading from this register always results in 0.
Writing to this register causes the written value to be discarded.

The 15 general purpose registers are mapped from `%R1` to `%R15` inclusively.
In particular, `%R8D` (aliased to `%SP`) is used as the stack pointer, and `%R9D` (aliased to `%BP`) is used as the base pointer by some instructions (`CALL` and `RET` for example).

These 16 registers are internally 32 bits integers.
Each have four access modes (detonated by suffixes):

 Suffix  | Description
---------|------------
 `L`     | Accesses the low byte (0 to 7).
 `H`     | Accesses the high byte (8 to 15).
 `W`     | Accesses the word (0 to 15). ___This is the default access mode for compatibility reasons!___
 `D`     | Accesses the full dword (0 to 31)

Any non-word accesses require an additional word sized __Register Extension Prefix__ on the instruction.

Note: The access suffixes cannot be used on aliases `%SP` and `%BP`.
They can be used with `%R8` or `%R9` though!

The instruction pointer is 32 bits.
Note: Due to unfortunate Java reasons, it's currently signed... (We'll fix it... eventually...)

## Environment

When you do:

```bash
./bin/nostalgia ./a.out
```

the environment will try to load the binary file as the [kernel](#Kernel).

If you do not provide a kernel, provide too many (only one is allowed), or your kernel has issues, the [dummy kernel](/src/main/resources/dummy_kernel.nos) will do it's best at reporting that.

### [Bootloader](/src/main/resources/bootloader.nos)

You do not control this part (not even if you write self-modifying hacky code).
It is loaded at address 0.
This will initialize the video memory so all cells have no styling and display null byte with white text on black background.
It will also allocate a 4096 byte stack.
This stack will eventually overwrite the bootloader!

Most importantly, it will perform an absolute jump (not a call) to `0x4000`.
This is where your kernel goes...

### Kernel

This will be loaded at `0x4000`.
Use `.ORG 0x4000` to make address calculations less painful!

### Keyboard Memory

This is located at `0x1000` and ends at `0x1007`.

`0x1000` to `0x1003` (the first dword) holds the key code being checked.
Please consult the Java KeyEvent `VK_*` values.
You can read and write to this.

`0x1004` (byte) is special. Reading from it means checking if the key code (in the first dword) is down.
One means down, zero means not down.
The current keyboard state will be checked everytime this byte is read!

Writing to it is sort of complicated.
Bit 0 to 3 will clear the first dword.
Bit 4 will reset the entire key up/down state.

`0x1005` to `0x1006` (the last word) is the character buffer for the key typed.
Reading this region, unlike the key up/down byte, does not reflect the current keyboard state.

To update the value (poll the value from the internal 24-character overwriting ring buffer), you need to write to `0x1005` with bit 1 on.
There are other bits you can write to this address:

Bit 0 indicates if future inputs should be kept (on means drop them, off means keep).
Bit 1 is already mentioned before.
If the internal ring buffer is empty, the last word will be set to the null character (0).
Bit 2 and 3 will clear the last word.
Bit 4 will clear the interal ring buffer.

### Graphics Memory

> Originally I wanted it to be at `0xB8000` like the VGA stuff,
> but then I did not want to do memory segmentation,
> also the registers were 16 bits (not 32)...

This is located at `0x2000` and ends at `0x2FA0`.

The screen is divided into cells (80 by 25) which each cell being word sized.
The memory is linear, row major: `0x2000` is cell (0, 0), `0x2002` is cell (1, 0), ... (0, 0) is the top left corner.

The low byte (even number addresses) hold the [glyph](/src/main/resources/zoomed_seabios8x16.png) to render.
For those who know the Windows codepages by heart, it's Code page 437.

The high byte (odd number addresses) hold the rendering style:

```
ABCCCDDD
```

* `A`: if we should underline the rendered glyph
* `B`: if we should flip the glyph over the y axis
* `C`: background color
* `D`: foreground color

The color scheme is as follows:

 ID |  Color
----|-------
 0  | Black
 1  | Red
 2  | Green
 3  | Yellow
 4  | Blue
 5  | Magenta
 6  | Cyan
 7  | White