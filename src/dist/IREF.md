# Nostalgia Instruction Reference

## Core Instructions

 Mnemonic                                                       | Summary
----------------------------------------------------------------|---------
 [`ADD`](#ADD---Add)                                            | Add
 [`AND`](#AND---Logical-AND)                                    | Logical AND
 [`ANDN`](#ANDN---Logical-AND-NOT)                              | Logical AND NOT
 [`CALL`](#CALL---Call-Function)                                | Call Function
 [`CMOV`](#CMOV---Conditional-Move)                             | Conditional Move
 [`CVT`](#CVT---Convert)                                        | Convert
 [`DIV`](#DIV---Unsigned-Divide)                                | Unsigned Divide
 [`ENTER`](#ENTER---Make-Stack-Frame-for-Function-Parameters)   | Make Stack Frame for Function Parameters
 [`FADD`](#FADD---Float-point-Add)                              | Float-point Add
 [`FDIV`](#FDIV---Float-point-Divide)                           | Float-point Divide
 [`FMOD`](#FMOD---Float-point-Modulo)                           | Float-point Modulo
 [`FMUL`](#FMUL---Float-point-Multiply)                         | Float-point Multiply
 [`FREM`](#FREM---Float-point-Remainder)                        | Float-point Remainder
 [`FSUB`](#FSUB---Float-point-Sub)                              | Float-point Subtract
 [`IDIV`](#IDIV---Signed-Divide)                                | Signed Divide
 [`IMAC`](#IMAC---Signed-Multiply-then-Add)                     | Signed Multiply then Add
 [`IMUL`](#IMUL---Signed-Multiply)                              | Signed Multiply
 [`JABS`](#JABS---Absolute-Jump)                                | Absolute Jump
 [`JREL`](#JREL---Relative-Jump)                                | Relative Jump
 [`LD`](#LD---Load)                                             | Load
 [`LDM`](#LDM---Load-Multiple)                                  | Load Multiple
 [`LEAVE`](#LEAVE---Function-Exit)                              | Function Exit
 [`MOV`](#MOV---Move)                                           | Move
 [`MUL`](#MUL---Unsigned-Multiply)                              | Unsigned Multiply
 [`OR`](#OR---Logical-OR)                                       | Logical OR
 [`ORN`](#ORN---Logical-OR-NOT)                                 | Logical OR NOT
 [`PADD`](#PADD---Add-Packed-Integers)                          | Add Packed Integers
 [`POP`](#POP---Pop-from-the-Stack)                             | Pop from the Stack
 [`PSUB`](#PSUB---Subtract-Packed-Integers)                     | Subtract Packed Integers
 [`PUSH`](#PUSH---Push-Onto-the-Stack)                          | Push Onto the Stack
 [`RET`](#RET---Return-to-Function)                             | Return from Function
 [`RSUB`](#RSUB---Reverse-Subtract)                             | Reverse Subtract
 [`SAL`](#SAL---Arithmetic-Left-Shift)                          | Arithmetic Left Shift
 [`SAR`](#SAR---Arithmetic-Right-Shift)                         | Arithmetic Right Shift
 [`SHL`](#SHL---Logical-Left-Shift)                             | Logical Left Shift
 [`SHR`](#SHR---Logical-Right-Shift)                            | Logical Right Shift
 [`ST`](#ST---Store)                                            | Store
 [`STM`](#STM---Store-Multiple)                                 | Store Multiple
 [`SUB`](#SUB---Subtract)                                       | Subtract
 [`XOR`](#XOR---Logical-XOR)                                    | Logical XOR

## Instruction Prefixes

 Mnemonic                           | Summary
------------------------------------|---------
 [`IEX`](#IEX---Integer-Extension)  | Integer Extension
 [`REX`](#REX---Register-Extension) | Register Extension

## Instruction Encoding

### Class I9

```
0xxx xxxi iiii iiii
```

*   `x` = Opcode
*   `i` = 9 bit immediate

### Class IR

```
0xxx xxxi iiii iaaa
```

*   `x` = Opcode
*   `i` = 6 bit immediate
*   `a` = 3 bit register selector

### Class IRR

```
0xxx xxxi iibb baaa
```

*   `x` = Opcode
*   `i` = 3 bit immediate
*   `a` = 3 bit register selector
*   `b` = 3 bit register selector

### Class 3R

```
0xxx xxxc ccbb baaa
```

*   `x` = Opcode
*   `a` = 3 bit register selector
*   `b` = 3 bit register selector
*   `c` = 3 bit register selector

### Class I12

```
1xxx iiii iiii iiii
```

*   `x` = Opcode
*   `i` = 12 bit immediate

### Class 4R

```
1xxx dddc ccbb baaa
```

*   `x` = Opcode
*   `a` = 3 bit register selector
*   `b` = 3 bit register selector
*   `c` = 3 bit register selector
*   `d` = 3 bit register selector

## IEX - Integer Extension

 Opcode | Instruction   | Encoding          | Description
--------|---------------|-------------------|----------------------------------
 0x06   | IEX.0 _imm12_ | [I12](#Class-I12) | Extends the immediate to 16 bits
 0x07   | IEX.1 _imm12_ | [I12](#Class-I12) | Extends the immediate to 16 bits

Note: This instruction is not exposed by the assembler!

### Description

Extends the immediate of the next instruction to 16 bits.
This only applies to instructions that use the immediate field as an immediate value.

The last integer extension is used if they appear in a sequence.

All immediate values are zero-extended without this extension prefix.

### Effect

```
EXTENSION =
    IF IEX.0 THEN imm12
    IF IEX.1 THEN imm12 OR 0x1000
```

Encoding Class I9:

```
imm = (EXTENSION SHL 9) OR imm9
```

Encoding Class IR:

```
imm = (EXTENSION SHL 6) OR imm6
```

Encoding Class IRR:

```
imm = (EXTENSION SHL 3) OR imm3
```

## REX - Register Extension

 Opcode | Instruction                   | Encoding          | Description
--------|-------------------------------|-------------------|----------------------------------
 0x00   | REX _RA_, _RB_, _RD_, _RC_    | [4R](#Class-4R)   | Extends the register accesses

Note: This instruction is not exposed by the assembler!

### Description

Extends the register accesses of the next instruction.
This only applies to instructions that use actual registers as operands _RA_, _RB_, _RC_ or _RD_.
The _regmask_ operand does not count as using actual registers, and therefore,
this extension is not applied over the register mask.

The last register extension is used if they appear in a sequence.

The registers being accessed depend on the instruction:
without this prefix, general-purpose registers access are 16-bits wide within `%R0` to `%R7` (inclusive)
whereas float-point register accesses are 32-bits wide within `%FP0` to `%FP7` (inclusive).

### Effect

#### If the register is treated as a general-purpose register

The low bit of the register extension allows access to registers `%R8` to `%R15` (inclusive).

The high two bits determine the width of the data access.

 Bit Pattern | Assembler Width Suffix | Description
-------------|------------------------|-------------
 0b00        | `W` or omitted         | Accesses the low byte (0 to 7)
 0b01        | `L`                    | Accesses the high byte (8 to 15)
 0b10        | `H`                    | Accesses the word (0 to 15)
 0b11        | `D`                    | Accesses the full dword

#### If the register is treated as a float-point register

The lower two bits of the register extension allows access to registers `%FP8` to `%FP31` (inclusive).

The highest bit determines the width of the data access.

 Bit Pattern | Assembler Width Suffix | Description
-------------|------------------------|-------------
 0b0         | `D` or omitted         | Access the low dword
 0b1         | `Q`                    | Access the full qword or the high dword in the case of [`MOV.F`](#MOV---Move) and [`MOV.R`](#MOV---Move)

## ADD - Add

 Opcode | Instruction                       | Encoding                      | Description
--------|-----------------------------------|-------------------------------|----------------------
 0x03   | ADD.R gp:_RA_, gp:_RC_, gp:_RB_   | [3R](#Class-3R)               | _RA_ = _RC_ + _RB_
 0x0B   | ADD.I gp:_RA_, _imm6_             | [IR](#Class-IR)               | _RA_ = _RA_ + _imm6_

## AND - Logical AND

 Opcode | Instruction               | Encoding          | Description
--------|---------------------------|-------------------|----------------------
 0x05   | AND.R _RA_, _RC_, _RB_    | [3R](#Class-3R)   | _RA_ = _RC_ AND _RB_

## ANDN - Logical AND NOT

 Opcode | Instruction               | Encoding          | Description
--------|---------------------------|-------------------|----------------------
 0x08   | ANDN.R _RA_, _RC_, _RB_   | [3R](#Class-3R)   | _RA_ = _RC_ AND (BITWISE-NOT _RB_)

## CALL - Call Function

 Opcode | Instruction                   | Encoding          | Description
--------|-------------------------------|-------------------|----------------------
 0x31   | CALL.Z _RA_, _imm3_, _RB_     | [IRR](#Class-IRR) | Call function located at _RB_ + _imm3_ if _RA_ = 0
 0x32   | CALL.NZ _RA_, _imm3_, _RB_    | [IRR](#Class-IRR) | Call function located at _RB_ + _imm3_ if _RA_ ≠ 0
 0x33   | CALL.GE _RA_, _imm3_, _RB_    | [IRR](#Class-IRR) | Call function located at _RB_ + _imm3_ if _RA_ ≥ 0
 0x34   | CALL.GT _RA_, _imm3_, _RB_    | [IRR](#Class-IRR) | Call function located at _RB_ + _imm3_ if _RA_ > 0
 0x35   | CALL.LE _RA_, _imm3_, _RB_    | [IRR](#Class-IRR) | Call function located at _RB_ + _imm3_ if _RA_ ≤ 0
 0x36   | CALL.LT _RA_, _imm3_, _RB_    | [IRR](#Class-IRR) | Call function located at _RB_ + _imm3_ if _RA_ < 0

### Description

Call a function located at the memory location specified by _RB_ + _imm3_.

This is implemented by pushing the return address onto the stack before performing an absolute jump (see [`JABS`](#JABS---Absolute-Jump)).

## CMOV - Conditional Move

 Opcode | Instruction               | Encoding          | Description
--------|---------------------------|-------------------|----------------------
 0x2B   | CMOV.I _RA_, _imm3_, _RB_ | [IRR](#Class-IRR) | _RA_ = _imm3_ if _RB_ ≠ 0
 0x2C   | CMOV.R _RA_, _RC_, _RB_   | [3R](#Class-3R)   | _RA_ = _RC_ if _RB_ ≠ 0

## CVT - Convert

 Opcode | Instruction                       | Encoding                      | Description
--------|-----------------------------------|-------------------------------|----------------------
 0x0A   | CVT.F fp:_RA_, gp:_RB_            | [IRR](#Class-IRR)<sup>1</sup> | _RA_ = _RB_
 0x0A   | CVT.R gp:_RA_, fp:_RB_            | [IRR](#Class-IRR)<sup>2</sup> | _RA_ = _RB_

Notes:
*   <sup>1</sup> - Immediate field is fixed to 2
*   <sup>2</sup> - Immediate field is fixed to 3

## DIV - Unsigned Divide

 Opcode | Instruction                   | Encoding          | Description
--------|-------------------------------|-------------------|----------------------------------
 0x04   | DIV _RA_, _RB_, _RD_, _RC_    | [4R](#Class-4R)   | _RA_ = _RD_ / _RC_, _RB_ = _RD_ % _RC_

### Description

Divides _RD_ by _RC_ as unsigned integers.
The quotient is stored in _RA_; the remainder is stored in _RB_.

## ENTER - Make Stack Frame for Function Parameters

 Opcode | Instruction               | Encoding          | Description
--------|---------------------------|-------------------|----------------------
 0x1D   | ENTER _imm9_              | [I9](#Class-I9)   | Create a stack frame for a function

## FADD - Float-point Add

 Opcode | Instruction           | Encoding                      | Description
--------|-----------------------|-------------------------------|----------------------
 0x0A   | FADD fp:_RA_, fp:_RB_ | [IRR](#Class-IRR)<sup>1</sup> | _RA_ = _RA_ + _RB_

Notes:
*   <sup>1</sup> - Immediate field is fixed to 4

## FDIV - Float-point Divide

 Opcode | Instruction           | Encoding                      | Description
--------|-----------------------|-------------------------------|----------------------
 0x0A   | FDIV fp:_RA_, fp:_RB_ | [IRR](#Class-IRR)<sup>1</sup> | _RA_ = _RA_ / _RB_

Notes:
*   <sup>1</sup> - Immediate field is fixed to 7

## FMOD - Float-point Modulo

 Opcode | Instruction           | Encoding                      | Description
--------|-----------------------|-------------------------------|----------------------
 0x0A   | FMOD fp:_RA_, fp:_RB_ | [IRR](#Class-IRR)<sup>1</sup> | _RA_ = _RA_ % _RB_

Notes:
*   <sup>1</sup> - Immediate field is fixed to 8

## FMUL - Float-point Multiply

 Opcode | Instruction           | Encoding                      | Description
--------|-----------------------|-------------------------------|----------------------
 0x0A   | FMUL fp:_RA_, fp:_RB_ | [IRR](#Class-IRR)<sup>1</sup> | _RA_ = _RA_ * _RB_

Notes:
*   <sup>1</sup> - Immediate field is fixed to 6

## FREM - Float-point Remainder

 Opcode | Instruction           | Encoding                      | Description
--------|-----------------------|-------------------------------|----------------------
 0x0A   | FREM fp:_RA_, fp:_RB_ | [IRR](#Class-IRR)<sup>1</sup> | _RA_ = _RA_ REMAINDER _RB_

Notes:
*   <sup>1</sup> - Immediate field is fixed to 9

## FSUB - Float-point Subtract

 Opcode | Instruction           | Encoding                      | Description
--------|-----------------------|-------------------------------|----------------------
 0x0A   | FSUB fp:_RA_, fp:_RB_ | [IRR](#Class-IRR)<sup>1</sup> | _RA_ = _RA_ - _RB_

Notes:
*   <sup>1</sup> - Immediate field is fixed to 4

## IDIV - Signed Divide

 Opcode | Instruction                   | Encoding          | Description
--------|-------------------------------|-------------------|----------------------------------
 0x02   | IDIV _RA_, _RB_, _RD_, _RC_   | [4R](#Class-4R)   | _RA_ = _RD_ / _RC_, _RB_ = _RD_ % _RC_

### Description

Divides _RD_ by _RC_ as signed integers.
The quotient is stored in _RA_; the remainder is stored in _RB_.

## IMAC - Signed Multiply then Add

 Opcode | Instruction                   | Encoding          | Description
--------|-------------------------------|-------------------|----------------------------------
 0x05   | IMAC _RA_, _RB_, _RD_, _RC_   | [4R](#Class-4R)   | _RA_ =_RB_ + _RD_ * _RC_

### Description

Multiples _RD_ and _RC_ as signed integers, then adds _RB_ to the result.

Unlike [`IMUL`](#IMUL---Signed-Multiply), the upper 32-bits for potential overflow is always discarded.

## IMUL - Signed Multiply

 Opcode | Instruction                   | Encoding          | Description
--------|-------------------------------|-------------------|----------------------------------
 0x01   | IMUL _RA_, _RB_, _RD_, _RC_   | [4R](#Class-4R)   | _RA_:_RB_ = _RD_ * _RC_

### Description

Multiples _RD_ and _RC_ as signed integers.
The 64-bit product into split into two parts, with the higher part stored into _RA_, and the lower part stored into _RB_.

```
64 -------------------------------- 32 ------------------ 16 -------- 8 -------- 0
 |            dword RA               |                 dword RB                  |
 +-----------------------------------+---------------------+----------+----------+
                                     |       word RA       |       word RB       |
                                     +---------------------+----------+----------+
                                                           |  byte RA |  byte RB |
                                                           +----------+----------+
```

## JABS - Absolute Jump

 Opcode | Instruction                   | Encoding          | Description
--------|-------------------------------|-------------------|----------------------
 0x0E   | JABS.Z _RA_, _imm3_, _RB_     | [IRR](#Class-IRR) | _IP_ = _RB_ + _imm3_ if _RA_ = 0
 0x0F   | JABS.NZ _RA_, _imm3_, _RB_    | [IRR](#Class-IRR) | _IP_ = _RB_ + _imm3_ if _RA_ ≠ 0
 0x10   | JABS.GE _RA_, _imm3_, _RB_    | [IRR](#Class-IRR) | _IP_ = _RB_ + _imm3_ if _RA_ ≥ 0
 0x11   | JABS.GT _RA_, _imm3_, _RB_    | [IRR](#Class-IRR) | _IP_ = _RB_ + _imm3_ if _RA_ > 0
 0x12   | JABS.LE _RA_, _imm3_, _RB_    | [IRR](#Class-IRR) | _IP_ = _RB_ + _imm3_ if _RA_ ≤ 0
 0x13   | JABS.LT _RA_, _imm3_, _RB_    | [IRR](#Class-IRR) | _IP_ = _RB_ + _imm3_ if _RA_ < 0

### Description

Jump to the memory location specified by _RB_ + _imm3_.

## JREL - Relative Jump

 Opcode | Instruction               | Encoding          | Description
--------|---------------------------|-------------------|----------------------
 0x14   | JREL.Z _RA_, _imm6_       | [IR](#Class-IR)   | _IP_ += _imm6_ if _RA_ = 0
 0x15   | JREL.NZ _RA_, _imm6_      | [IR](#Class-IR)   | _IP_ += _imm6_ if _RA_ ≠ 0
 0x16   | JREL.GE _RA_, _imm6_      | [IR](#Class-IR)   | _IP_ += _imm6_ if _RA_ ≥ 0
 0x17   | JREL.GT _RA_, _imm6_      | [IR](#Class-IR)   | _IP_ += _imm6_ if _RA_ > 0
 0x18   | JREL.LE _RA_, _imm6_      | [IR](#Class-IR)   | _IP_ += _imm6_ if _RA_ ≤ 0
 0x19   | JREL.LT _RA_, _imm6_      | [IR](#Class-IR)   | _IP_ += _imm6_ if _RA_ < 0

### Description

Jump relative to the end address of the current instruction.

### Example

Infinite loop:

```
+0000 JREL.Z %R0, -4            ; two bytes from JREL.Z, two bytes from hidden IEX.1
+0004 ...                       ; <- the offset is calculated from here, not at +0000
```

## LD - Load

 Opcode | Instruction               | Encoding          | Description
--------|---------------------------|-------------------|----------------------
 0x1F   | LD.D _RA_, _imm3_, _RB_   | [IRR](#Class-IRR) | _RA_ = dword ptr [_RB_ + _imm3_]
 0x21   | LD.W _RA_, _imm3_, _RB_   | [IRR](#Class-IRR) | _RA_ = word ptr [_RB_ + _imm3_]
 0x23   | LD.B _RA_, _imm3_, _RB_   | [IRR](#Class-IRR) | _RA_ = byte ptr [_RB_ + _imm3_]

## LDM - Load Multiple

 Opcode | Instruction               | Encoding          | Description
--------|---------------------------|-------------------|----------------------
 0x37   | LDM.D _regmask_, _RA_     | [IR](#Class-IR)   | Load dword then increment
 0x39   | LDM.W _regmask_, _RA_     | [IR](#Class-IR)   | Load word then increment
 0x3B   | LDM.H _regmask_, _RA_     | [IR](#Class-IR)   | Load high byte then increment
 0x3D   | LDM.L _regmask_, _RA_     | [IR](#Class-IR)   | Load low byte then increment
 0x37   | LDM.DS _regmask_, _RA_    | [IR](#Class-IR)   | Load dword then increment, save final address
 0x39   | LDM.WS _regmask_, _RA_    | [IR](#Class-IR)   | Load word then increment, save final address
 0x3B   | LDM.HS _regmask_, _RA_    | [IR](#Class-IR)   | Load high byte then increment, save final address
 0x3D   | LDM.LS _regmask_, _RA_    | [IR](#Class-IR)   | Load low byte then increment, save final address

Note: bit 0 of the register mask does not encode `%R0` but the `S` variant instead.

### Effect

```
WIDTH = .D OR .W OR .H OR .L

ADDRESS = ra
FOR EACH REGISTER IN regmask
    REGISTER.WIDTH = LOAD WIDTH PTR ADDRESS
    ADDRESS += WIDTH

IF BIT 0 THEN ra = ADDRESS
```

## LEAVE - Function Exit

 Opcode | Instruction               | Encoding          | Description
--------|---------------------------|-------------------|----------------------
 0x1E   | LEAVE                     | [I9](#Class-I9)   | Sets SP to BP then pop BP

## MOV - Move

 Opcode | Instruction               | Encoding                      | Description
--------|---------------------------|-------------------------------|----------------------
 0x00   | MOV.I gp:_RA_, _imm6_     | [IR](#Class-IR)               | _RA_ = _imm6_
 0x01   | MOV.LO gp:_RA_, _imm6_    | [IR](#Class-IR)               | lower half of _RA_ = _imm6_
 0x02   | MOV.HI gp:_RA_, _imm6_    | [IR](#Class-IR)               | higher half of _RA_ = _imm6_
 0x0A   | MOV.F fp:_RA_, gp:_RB_    | [IRR](#Class-IRR)<sup>1</sup> | _RA_ = BITCAST _RB_
 0x0A   | MOV.R gp:_RA_, fp:_RB_    | [IRR](#Class-IRR)<sup>2</sup> | _RA_ = BITCAST _RB_

Notes:
*   <sup>1</sup> - Immediate field is fixed to 0
*   <sup>2</sup> - Immediate field is fixed to 1

### Description

Moves the operand on the right side into the register operand on the left side.

## MUL - Unsigned Multiply

 Opcode | Instruction                   | Encoding          | Description
--------|-------------------------------|-------------------|----------------------------------
 0x03   | MUL _RA_, _RB_, _RD_, _RC_    | [4R](#Class-4R)   | _RA_:_RB_ = _RD_ * _RC_

### Description

Multiples _RD_ and _RC_ as unsigned integers.
The 64-bit product into split into two parts, with the higher part stored into _RA_, and the lower part stored into _RB_.

```
64 -------------------------------- 32 ------------------ 16 -------- 8 -------- 0
 |            dword RA               |                 dword RB                  |
 +-----------------------------------+---------------------+----------+----------+
                                     |       word RA       |       word RB       |
                                     +---------------------+----------+----------+
                                                           |  byte RA |  byte RB |
                                                           +----------+----------+
```

## OR - Logical OR

 Opcode | Instruction               | Encoding          | Description
--------|---------------------------|-------------------|----------------------
 0x06   | OR.R _RA_, _RC_, _RB_     | [3R](#Class-3R)   | _RA_ = _RC_ OR _RB_

## ORN - Logical OR NOT

 Opcode | Instruction               | Encoding          | Description
--------|---------------------------|-------------------|----------------------
 0x09   | ORN.R _RA_, _RC_, _RB_    | [3R](#Class-3R)   | _RA_ = _RC_ OR (BITWISE-NOT _RB_)

## PADD - Add Packed Integers

 Opcode | Instruction               | Encoding          | Description
--------|---------------------------|-------------------|----------------------
 0x2D   | PADD.W _RA_, _RC_, _RB_   | [3R](#Class-3R)   | _RA_ = _RC_ + _RB_ as packed words
 0x2E   | PADD.B _RA_, _RC_, _RB_   | [3R](#Class-3R)   | _RA_ = _RC_ + _RB_ as packed bytes

## POP - Pop from the Stack

 Opcode | Instruction               | Encoding          | Description
--------|---------------------------|-------------------|----------------------
 0x1A   | POP.D _regmask_           | [I9](#Class-I9)   | Pops a sequence of registers from the stack
 0x1A   | POP.W _regmask_           | [I9](#Class-I9)   | Pops a sequence of registers from the stack

Note: bit 0 of the register mask does not encode `%R0` but the `D` variant.

### Effect

```
WIDTH = IF BIT 0 THEN .D ELSE .W

ADDRESS = sp
FOR EACH REGISTER IN regmask
    REGISTER.WIDTH = LOAD WIDTH PTR ADDRESS
    ADDRESS += WIDTH

sp = ADDRESS
```

## PSUB - Subtract Packed Integers

 Opcode | Instruction               | Encoding          | Description
--------|---------------------------|-------------------|----------------------
 0x2F   | PSUB.W _RA_, _RC_, _RB_   | [3R](#Class-3R)   | _RA_ = _RC_ - _RB_ as packed words
 0x30   | PSUB.B _RA_, _RC_, _RB_   | [3R](#Class-3R)   | _RA_ = _RC_ - _RB_ as packed bytes

## PUSH - Push Onto the Stack

 Opcode | Instruction               | Encoding          | Description
--------|---------------------------|-------------------|----------------------
 0x1A   | PUSH.D _regmask_          | [I9](#Class-I9)   | Pushes a sequence of registers onto the stack
 0x1A   | PUSH.W _regmask_          | [I9](#Class-I9)   | Pushes a sequence of registers onto the stack

Note: bit 0 of the register mask does not encode `%R0` but the `D` variant.

### Effect

```
WIDTH = IF BIT 0 THEN .D ELSE .W

ADDRESS = sp
FOR EACH REGISTER IN regmask REVERSED
    ADDRESS -= WIDTH
    STORE WIDTH PTR ADDRESS = REGISTER.WIDTH

sp = ADDRESS
```

## RET - Return to Function

 Opcode | Instruction               | Encoding          | Description
--------|---------------------------|-------------------|----------------------
 0x1C   | RET _imm9_                | [I9](#Class-I9)   | Return to caller then pop _imm9_ bytes

## RSUB - Reverse Subtract

 Opcode | Instruction               | Encoding          | Description
--------|---------------------------|-------------------|----------------------
 0x0D   | RSUB.I _RA_, _imm6_       | [IR](#Class-IR)   | _RA_ = _imm6_ - _RA_

## SAL - Arithmetic Left Shift

 Opcode | Instruction               | Encoding          | Description
--------|---------------------------|-------------------|----------------------
 0x25   | SAL.R _RA_, _RC_, _RB_    | [3R](#Class-3R)   | _RA_ = _RC_ << _RB_
 0x28   | SAL.I _RA_, _imm6_        | [IR](#Class-IR)   | _RA_ = _RA_ << _imm6_

Note: This is the same as [`SHL`](#SHL---Logical-Left-Shift)!

## SAR - Arithmetic Right Shift

 Opcode | Instruction               | Encoding          | Description
--------|---------------------------|-------------------|----------------------
 0x27   | SAR.R _RA_, _RC_, _RB_    | [3R](#Class-3R)   | _RA_ = _RC_ >> _RB_
 0x2A   | SAR.I _RA_, _imm6_        | [IR](#Class-IR)   | _RA_ = _RA_ >> _imm6_

## SHL - Logical Left Shift

 Opcode | Instruction               | Encoding          | Description
--------|---------------------------|-------------------|----------------------
 0x25   | SHL.R _RA_, _RC_, _RB_    | [3R](#Class-3R)   | _RA_ = _RC_ << _RB_
 0x28   | SHL.I _RA_, _imm6_        | [IR](#Class-IR)   | _RA_ = _RA_ << _imm6_

Note: This is the same as [`SAL`](#SAL---Arithmetic-Left-Shift)!

## SHR - Logical Right Shift

 Opcode | Instruction               | Encoding          | Description
--------|---------------------------|-------------------|----------------------
 0x26   | SHR.R _RA_, _RC_, _RB_    | [3R](#Class-3R)   | _RA_ = _RC_ >> _RB_
 0x29   | SHR.I _RA_, _imm6_        | [IR](#Class-IR)   | _RA_ = _RA_ >> _imm6_

## ST - Store

 Opcode | Instruction               | Encoding          | Description
--------|---------------------------|-------------------|----------------------
 0x20   | ST.D _RA_, _imm3_, _RB_   | [IRR](#Class-IRR) | dword ptr [_RB_ + _imm3_] = _RA_
 0x22   | ST.W _RA_, _imm3_, _RB_   | [IRR](#Class-IRR) | word ptr [_RB_ + _imm3_] = _RA_
 0x24   | ST.B _RA_, _imm3_, _RB_   | [IRR](#Class-IRR) | byte ptr [_RB_ + _imm3_] = _RA_

## STM - Store Multiple

 Opcode | Instruction               | Encoding          | Description
--------|---------------------------|-------------------|----------------------
 0x38   | STM.D _regmask_, _RA_     | [IR](#Class-IR)   | Decrement then store dword
 0x3A   | STM.W _regmask_, _RA_     | [IR](#Class-IR)   | Decrement then store word
 0x3C   | STM.H _regmask_, _RA_     | [IR](#Class-IR)   | Decrement then store high byte
 0x3E   | STM.L _regmask_, _RA_     | [IR](#Class-IR)   | Decrement then store low byte
 0x38   | STM.DS _regmask_, _RA_    | [IR](#Class-IR)   | Decrement then store dword, save final address
 0x3A   | STM.WS _regmask_, _RA_    | [IR](#Class-IR)   | Decrement then store word, save final address
 0x3C   | STM.HS _regmask_, _RA_    | [IR](#Class-IR)   | Decrement then store high byte, save final address
 0x3E   | STM.LS _regmask_, _RA_    | [IR](#Class-IR)   | Decrement then store low byte, save final address

Note: bit 0 of the register mask does not encode `%R0` but the `S` variant instead.

### Effect

```
WIDTH = .D OR .W OR .H OR .L

ADDRESS = ra
FOR EACH REGISTER IN regmask REVERSED
    ADDRESS -= WIDTH
    STORE WIDTH PTR ADDRESS = REGISTER.WIDTH

IF BIT 0 THEN ra = ADDRESS
```

## SUB - Subtract

 Opcode | Instruction                       | Encoding                      | Description
--------|-----------------------------------|-------------------------------|----------------------
 0x04   | SUB.R gp:_RA_, gp:_RC_, gp:_RB_   | [3R](#Class-3R)               | _RA_ = _RC_ - _RB_
 0x0C   | SUB.I gp:_RA_, _imm6_             | [IR](#Class-IR)               | _RA_ = _RA_ - _imm6_

## XOR - Logical XOR

 Opcode | Instruction               | Encoding          | Description
--------|---------------------------|-------------------|----------------------
 0x07   | XOR.R _RA_, _RC_, _RB_    | [3R](#Class-3R)   | _RA_ = _RC_ XOR _RB_
