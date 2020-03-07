package org.atoiks.games.nostalgia;

public interface InstrVisitor {

    public void illegalOp(int fullWord);

    // ***** OP0 class *****

    public void movI(int imm6, int rdst);

    public void addR(int rlhs, int rrhs, int rdst);
    public void subR(int rlhs, int rrhs, int rdst);
    public void andR(int rlhs, int rrhs, int rdst);
    public void orR(int rlhs, int rrhs, int rdst);
    public void xorR(int rlhs, int rrhs, int rdst);
    public void nandR(int rlhs, int rrhs, int rdst);
    public void norR(int rlhs, int rrhs, int rdst);
    public void nxorR(int rlhs, int rrhs, int rdst);

    public void addI(int imm6, int rdst);
    public void subI(int imm6, int rdst);
    public void rsubI(int imm6, int rdst);

    public void jabsZ(int imm6, int rflag);
    public void jabsNZ(int imm6, int rflag);
    public void jabsGE(int imm6, int rflag);
    public void jabsGT(int imm6, int rflag);
    public void jabsLE(int imm6, int rflag);
    public void jabsLT(int imm6, int rflag);

    public void jrelZ(int imm6, int rflag);
    public void jrelNZ(int imm6, int rflag);
    public void jrelGE(int imm6, int rflag);
    public void jrelGT(int imm6, int rflag);
    public void jrelLE(int imm6, int rflag);
    public void jrelLT(int imm6, int rflag);

    public void push(int imm6, int rsrc);
    public void pop(int imm6, int rdst);

    public void mtsp(int imm6, int rsrc);
    public void mtbp(int imm6, int rsrc);

    public void mspt(int imm6, int rdst);
    public void mbpt(int imm6, int rdst);

    public void call(int imm9);
    public void ret();
    public void enter(int imm9);
    public void leave();

    public void inner(int rlhs, int rrhs, int rdst);
    public void outer(int rlhs, int rrhs, int rdst);

    public void ldW(int imm3, int radj, int rdst);
    public void stW(int imm3, int radj, int rsrc);
    public void ldB(int imm3, int radj, int rdst);
    public void stB(int imm3, int radj, int rsrc);

    public void shlR(int rlhs, int rrhs, int rdst);
    public void shrR(int rlhs, int rrhs, int rdst);
    public void sarR(int rlhs, int rrhs, int rdst);

    public void shlI(int imm4, int rdst);
    public void shrI(int imm4, int rdst);
    public void sarI(int imm4, int rdst);

    public void push3(int rC, int rB, int rA);
    public void pop3(int rC, int rB, int rA);

    public void cmovI(int imm3, int rflag, int rdst);
    public void cmovR(int rsrc, int rflag, int rdst);

    // ***** OP1 class *****

    public void iex(int imm12); // prefix
    public void rex(int rD, int rC, int rB, int rA); // prefix

    public void mul(int rlhs, int rrhs, int rdlo, int rdhi);
    public void div(int rlhs, int rrhs, int rdrem, int rdquo);
}
