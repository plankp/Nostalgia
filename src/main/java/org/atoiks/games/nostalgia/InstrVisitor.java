package org.atoiks.games.nostalgia;

public interface InstrVisitor {

    public void illegalOp(int fullWord);

    // ***** OP0 class *****

    public void movI(int imm6, int rdst);
    public void movLO(int imm6, int rdst);
    public void movHI(int imm6, int rdst);

    public void addR(int rlhs, int rrhs, int rdst);
    public void subR(int rlhs, int rrhs, int rdst);
    public void andR(int rlhs, int rrhs, int rdst);
    public void orR(int rlhs, int rrhs, int rdst);
    public void xorR(int rlhs, int rrhs, int rdst);
    public void andnR(int rlhs, int rrhs, int rdst);
    public void ornR(int rlhs, int rrhs, int rdst);

    public void fpext(int imm3, int rsrc, int rdst);

    public void addI(int imm6, int rdst);
    public void subI(int imm6, int rdst);
    public void rsubI(int imm6, int rdst);

    public void jabsZ(int imm3, int radj, int rflag);
    public void jabsNZ(int imm3, int radj, int rflag);
    public void jabsGE(int imm3, int radj, int rflag);
    public void jabsGT(int imm3, int radj, int rflag);
    public void jabsLE(int imm3, int radj, int rflag);
    public void jabsLT(int imm3, int radj, int rflag);

    public void jrelZ(int imm6, int rflag);
    public void jrelNZ(int imm6, int rflag);
    public void jrelGE(int imm6, int rflag);
    public void jrelGT(int imm6, int rflag);
    public void jrelLE(int imm6, int rflag);
    public void jrelLT(int imm6, int rflag);

    public void push(int imm9);
    public void pop(int imm9);

    public void ret(int imm9);
    public void enter(int imm9);
    public void leave();

    public void ldD(int imm3, int radj, int rdst);
    public void stD(int imm3, int radj, int rsrc);
    public void ldW(int imm3, int radj, int rdst);
    public void stW(int imm3, int radj, int rsrc);
    public void ldB(int imm3, int radj, int rdst);
    public void stB(int imm3, int radj, int rsrc);

    public void ldmD(int imm6, int rbase);
    public void stmD(int imm6, int rbase);
    public void ldmW(int imm6, int rbase);
    public void stmW(int imm6, int rbase);
    public void ldmLB(int imm6, int rbase);
    public void stmLB(int imm6, int rbase);
    public void ldmHB(int imm6, int rbase);
    public void stmHB(int imm6, int rbase);

    public void shlR(int rlhs, int rrhs, int rdst);
    public void shrR(int rlhs, int rrhs, int rdst);
    public void sarR(int rlhs, int rrhs, int rdst);

    public void shlI(int imm6, int rdst);
    public void shrI(int imm6, int rdst);
    public void sarI(int imm6, int rdst);

    public void cmovI(int imm3, int rflag, int rdst);
    public void cmovR(int rsrc, int rflag, int rdst);

    public void paddW(int rlhs, int rrhs, int rdst);
    public void paddB(int rlhs, int rrhs, int rdst);
    public void psubW(int rlhs, int rrhs, int rdst);
    public void psubB(int rlhs, int rrhs, int rdst);

    public void callZ(int imm3, int radj, int rflag);
    public void callNZ(int imm3, int radj, int rflag);
    public void callGE(int imm3, int radj, int rflag);
    public void callGT(int imm3, int radj, int rflag);
    public void callLE(int imm3, int radj, int rflag);
    public void callLT(int imm3, int radj, int rflag);

    // ***** OP1 class *****

    public void iex(int imm12); // prefix
    public void rex(int rD, int rC, int rB, int rA); // prefix

    public void imul(int rlhs, int rrhs, int rdlo, int rdhi);
    public void idiv(int rlhs, int rrhs, int rdrem, int rdquo);

    public void mul(int rlhs, int rrhs, int rdlo, int rdhi);
    public void div(int rlhs, int rrhs, int rdrem, int rdquo);

    public void imac(int rlhs, int rrhs, int racc, int rdst);
}
