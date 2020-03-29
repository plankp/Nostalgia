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

### Very important!

If the assembler crashed (due to bugs or because the code provided is incorrect),
the default kernel will display the error message.

-----

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

Some instructions end with a `.*`.
That is a conditional suffix and must be substituted with one of the following:

 Conditional Suffix | Execute Condition
--------------------|-------------
 `.Z`               | If the register being tested = zero.
 `.NZ`              | If the register begin tested ≠ zero.
 `.GT`              | If the register begin tested > zero. (signed)
 `.GE`              | If the register begin tested ≥ zero. (signed)
 `.LT`              | If the register begin tested < zero. (signed)
 `.LE`              | If the register begin tested ≤ zero. (signed)

Some instructions have `<regmask>` as an operand.
That is a register mask.
It is conceptually as a non-empty set of registers sorted in natural order (so `%R1`, `%R2`, ...).
It is also written as a list of registers:

```
LDM.W %R1W, %R2W, %R3W, %R4W, %R5W, %SP
```

Notice all the registers of the mask (`%R1` to `%R5`) have the same [access mode suffix](#Registers).
These restrictions will be noted by each individual instruction using a register mask.
Using a different access mode suffix from the one specified results in an error.
Another restriction is that the zero register `%R0` cannot appear in the mask.

Here is a list of instructions supported by the assembler.
All instructions are case insensitive.
Using one that's not supported would cause the assembler to crash.

 Name       | Example                       | Description
------------|-------------------------------|------------
 `MOV.I`    | `MOV.I %R1W, 10`              | Moves a maximum 16 bit immediate value into a register.
 `MOV.LO`   | `MOV.LO %R1W, 10`             | Moves a maximum 16 bit immediate value into the lower half of the register. The higher half is left untouched, and the immediate value is potentially subject to truncation.
 `MOV.HI`   | `MOV.HI %R1W, 10`             | Moves a maximum 16 bit immediate value into the higher half of the register. The lower half is left untouched, and the immediate value is potentially subject to truncation.
 `ADD.R`    | `ADD.R %R1W, %R2W, %R3W`      | Adds two registers and stores the result into a register.
 `SUB.R`    | `SUB.R %R1W, %R2W, %R3W`      | Takes the difference of two registers and stores the result into a register.
 `AND.R`    | `AND.R %R1W, %R2W, %R3W`      | Performs the bitwise AND of two registers and stores the result into a register.
 `OR.R`     | `OR.R %R1W, %R2W, %R3W`       | Performs the bitwise OR of two registers and stores the result into a register.
 `XOR.R`    | `XOR.R %R1W, %R2W, %R3W`      | Performs the bitwise XOR of two registers and stores the result into a register.
 `NAND.R`   | `NAND.R %R1W, %R2W, %R3W`     | Performs the bitwise AND of two registers and stores the complement of the result into a register.
 `NOR.R`    | `NOR.R %R1W, %R2W, %R3W`      | Performs the bitwise OR of two registers and stores the complement of the result into a register.
 `NXOR.R`   | `NXOR.R %R1W, %R2W, %R3W`     | Performs the bitwise XOR of two registers and stores the complement of the result into a register.
 `ADD.I`    | `ADD.I %R1W, 10`              | Adds the maximum 16 bit immediate value to a register.
 `SUB.I`    | `SUB.I %R1W, 10`              | Subtracts a maximum 16 bit immediate value from a register.
 `RSUB.I`   | `RSUB.I %R1W, 10`             | Subtracts a register from a maximum 16 bit immediate value (and stores it back into the same register).
 `JABS.*`   | `JABS.* %R1W, FUNC, %R0`      | Performs an absolute jump to the address `FUNC+%R0` with register `%R1W` being tested.
 `JREL.*`   | `JREL.* %R0W, -4`             | Performs an relative jump with register
 `PUSH`     | `PUSH %R1W, 0`                | Pushes a 16 bit or 32 bit value onto the stack (depends on register suffix). Uses the stack pointer implicitly.
 `POP`      | `POP %R1W, 0`                 | Pops a 16 bit or 32 bit value from the stack (depends on register suffix). Uses the stack pointer implicitly.
 `CALL.*`   | `CALL.* %R1W, MEMCPY, %R0`    | The return address is pushed onto the stack and core will perform an absolute jump to the address `MEMCPY+%R0`. Uses the stack pointer implicitly. Register `%R1W` is being tested.
 `RET`      | `RET 0`                       | Pops the return address from the stack, jumps to it, then pops a maximum 16 bit amount of bytes off the stack. Uses the stack pointer implicitly.
 `ENTER`    | `ENTER 4`                     | Allocates some space on the stack when entering a function. Uses the stack pointer and base pointer implicitly.
 `LEAVE`    | `LEAVE`                       | Used to free the stack when leaving a function. Uses the stack pointer and base pointer implicitly.
 `LD.D`     | `LD.D %R2D, 0x1000, %R0W`     | Loads a dword from memory from address `0x1000+%R0W`.
 `ST.D`     | `ST.D %R2D, 0x1000, %R0W`     | Stores a dword into memory at address `0x1000+%R0W`.
 `LD.W`     | `LD.W %R2W, 0x1000, %R0W`     | Loads a word from memory from address `0x1000+%R0W`.
 `ST.W`     | `ST.W %R2W, 0x1000, %R0W`     | Stores a word into memory at address `0x1000+%R0W`.
 `LD.B`     | `LD.B %R2L, 0x1000, %R0W`     | Loads a byte from memory from address `0x1000+%R0W`.
 `ST.B`     | `ST.B %R2L, 0x1000, %R0W`     | Stores a byte into memory at address `0x1000+%R0W`.
 `LDM.D`    | `LDM.D <regmask>, %SP`        | Performs `LD.D` then increment address in forwards order of register mask. The `%SP` operand indicates the initial memory address. Access mode suffix must be `D`. 
 `STM.D`    | `STM.D <regmask>, %SP`        | Decrements address then performs `ST.D` in reverse order of register mask. The `%SP` operand indicates the initial memory address. Access mode suffix must be `D`. 
 `LDM.W`    | `LDM.W <regmask>, %SP`        | Performs `LD.W` then increment address in forwards order of register mask. The `%SP` operand indicates the initial memory address. Access mode suffix must be `W`. 
 `STM.W`    | `STM.W <regmask>, %SP`        | Decrements address then performs `ST.W` in reverse order of register mask. The `%SP` operand indicates the initial memory address. Access mode suffix must be `W`. 
 `LDM.L`    | `LDM.L <regmask>, %SP`        | Performs `LD.B` then increment address in forwards order of register mask. The `%SP` operand indicates the initial memory address. Access mode suffix must be `L`. 
 `STM.L`    | `STM.L <regmask>, %SP`        | Decrements address then performs `ST.B` in reverse order of register mask. The `%SP` operand indicates the initial memory address. Access mode suffix must be `L`. 
 `LDM.H`    | `LDM.H <regmask>, %SP`        | Performs `LD.B` then increment address in forwards order of register mask. The `%SP` operand indicates the initial memory address. Access mode suffix must be `H`. 
 `STM.H`    | `STM.H <regmask>, %SP`        | Decrements address then performs `ST.B` in reverse order of register mask. The `%SP` operand indicates the initial memory address. Access mode suffix must be `H`. 
 `LDM.DS`   | `LDM.DS <regmask>, %SP`       | Performs `LD.D` then increment address in forwards order of register mask. The `%SP` operand indicates the initial memory address; it will contain the final memory address after this instruction. Access mode suffix must be `D`. 
 `STM.DS`   | `STM.DS <regmask>, %SP`       | Decrements address then performs `ST.D` in reverse order of register mask. The `%SP` operand indicates the initial memory address; it will contain the final memory address after this instruction. Access mode suffix must be `D`. 
 `LDM.WS`   | `LDM.WS <regmask>, %SP`       | Performs `LD.W` then increment address in forwards order of register mask. The `%SP` operand indicates the initial memory address; it will contain the final memory address after this instruction. Access mode suffix must be `W`. 
 `STM.WS`   | `STM.WS <regmask>, %SP`       | Decrements address then performs `ST.W` in reverse order of register mask. The `%SP` operand indicates the initial memory address; it will contain the final memory address after this instruction. Access mode suffix must be `W`. 
 `LDM.LS`   | `LDM.LS <regmask>, %SP`       | Performs `LD.B` then increment address in forwards order of register mask. The `%SP` operand indicates the initial memory address; it will contain the final memory address after this instruction. Access mode suffix must be `L`. 
 `STM.LS`   | `STM.LS <regmask>, %SP`       | Decrements address then performs `ST.B` in reverse order of register mask. The `%SP` operand indicates the initial memory address; it will contain the final memory address after this instruction. Access mode suffix must be `L`. 
 `LDM.HS`   | `LDM.HS <regmask>, %SP`       | Performs `LD.B` then increment address in forwards order of register mask. The `%SP` operand indicates the initial memory address; it will contain the final memory address after this instruction. Access mode suffix must be `H`. 
 `STM.HS`   | `STM.HS <regmask>, %SP`       | Decrements address then performs `ST.B` in reverse order of register mask. The `%SP` operand indicates the initial memory address; it will contain the final memory address after this instruction. Access mode suffix must be `H`. 
 `SHL.R`    | `SHL.R %R3W, %R2W, %R1W`      | Performs left shift on register (same as `SAL.R`).
 `SAL.R`    | `SAL.R %R3W, %R2W, %R1W`      | Performs left shift on register (same as `SHL.R`).
 `SHR.R`    | `SHR.R %R3W, %R2W, %R1W`      | Performs logical right shift on register.
 `SAR.R`    | `SAR.R %R3W, %R2W, %R1W`      | Performs arithmetic right shift on register.
 `SHL.I`    | `SHL.I %R2W, 5`               | Performs left shift on register (same as `SAL.I`).
 `SAL.I`    | `SAL.I %R2W, 5`               | Performs left shift on register (same as `SHL.I`).
 `SHR.I`    | `SHR.I %R2W, 5`               | Performs logical right shift on register.
 `SAR.I`    | `SAR.I %R2W, 5`               | Performs arithmetic right shift on register.
 `CMOV.I`   | `CMOV.I %R1W, 5, %R0W`        | Stores a maximum 16 bit immediate value into `%R1W` operand if `%R0W` operand is not 0.
 `CMOV.R`   | `CMOV.I %R1W, %R2W, %R0W`     | Moves the value of `%R2W` operand into `%R1W` operand if `%R0W` operand is not 0.
 `PADD.W`   | `PADD.W %R1D, %R2D, %R0D`     | Performs addition the internal 32 bit buffer as two 16 bit values.
 `PSUB.W`   | `PSUB.W %R1D, %R2D, %R0D`     | Performs subtraction on the internal 32 bit buffer as two 16 bit values.
 `PADD.B`   | `PADD.B %R1D, %R2D, %R0D`     | Performs addition the internal 32 bit buffer as four 8 bit values.
 `PSUB.B`   | `PSUB.B %R1D, %R2D, %R0D`     | Performs subtraction on the internal 32 bit buffer as four 8 bit values.
 `IMUL`     | `IMUL %R0W, %R1W, %R2W, %R3W` | Multiplies `%R2W` operand and `%R3W` operand as signed values and stores the result into register pair `%R0W:%R1W` as high:low pair.
 `IDIV`     | `IDIV %R1W, %R0W, %R2W, %R3W` | Divides `%R2W` operand by `%R3W` operand as signed values. Quotient is stored in `%R1W` operand, remainder is stored in `%R0W` operand.
 `MUL`      | `MUL %R0W, %R1W, %R2W, %R3W`  | Multiplies `%R2W` operand and `%R3W` operand as unsigned values and stores the result into register pair `%R0W:%R1W` as high:low pair.
 `DIV`      | `DIV %R1W, %R0W, %R2W, %R3W`  | Divides `%R2W` operand by `%R3W` operand as unsigned values. Quotient is stored in `%R1W` operand, remainder is stored in `%R0W` operand.
 `IMAC`     | `IMAC %R1W, %R2W, %R3W, %R4W` | `%R1W = %R2W + (%R3W * %R4W)`. All values are interpreted as signed.

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