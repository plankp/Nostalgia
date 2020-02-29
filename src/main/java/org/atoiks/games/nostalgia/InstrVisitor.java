package org.atoiks.games.nostalgia;

public interface InstrVisitor {

    public void movI(int imm, int rdst);

    public void addR(int rlhs, int rrhs, int rdst);
    public void subR(int rlhs, int rrhs, int rdst);
    public void andR(int rlhs, int rrhs, int rdst);
    public void orR(int rlhs, int rrhs, int rdst);
    public void xorR(int rlhs, int rrhs, int rdst);
    public void nandR(int rlhs, int rrhs, int rdst);
    public void norR(int rlhs, int rrhs, int rdst);
    public void nxorR(int rlhs, int rrhs, int rdst);

    public void addI(int imm, int rdst);
    public void subI(int imm, int rdst);
    public void rsubI(int imm, int rdst);

    public void jabsZ(int imm, int rflag);
    public void jabsNZ(int imm, int rflag);
    public void jabsGE(int imm, int rflag);
    public void jabsGT(int imm, int rflag);
    public void jabsLE(int imm, int rflag);
    public void jabsLT(int imm, int rflag);

    public void jrelZ(int imm, int rflag);
    public void jrelNZ(int imm, int rflag);
    public void jrelGE(int imm, int rflag);
    public void jrelGT(int imm, int rflag);
    public void jrelLE(int imm, int rflag);
    public void jrelLT(int imm, int rflag);

    public void push(int imm, int rsrc);
    public void pop(int imm, int rdst);

    public void mtsp(int imm, int rsrc);
    public void mtbp(int imm, int rsrc);

    public void mspt(int imm, int rdst);
    public void mbpt(int imm, int rdst);

    public void call(int imm);
    public void ret();
    public void enter(int imm);
    public void leave();

    public void inner(int rlhs, int rrhs, int rdst);
    public void outer(int rlhs, int rrhs, int rdst);

    public void ldW(int imm, int radj, int rdst);
    public void stW(int imm, int radj, int rsrc);
    public void ldB(int imm, int radj, int rdst);
    public void stB(int imm, int radj, int rsrc);
}
