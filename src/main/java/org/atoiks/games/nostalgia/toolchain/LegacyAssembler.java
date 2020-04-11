package org.atoiks.games.nostalgia.toolchain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

// For now, we just use the old assembler...
import org.atoiks.games.nostalgia.Assembler;

public final class LegacyAssembler {

    public static void main(String[] args) {
        entry(args);
    }

    public static boolean entry(String[] args) {
        boolean errored = false;
        boolean dspHelp = false;

        String output = "a.out";
        ArrayList<String> inpList = new ArrayList<>();
        ArrayList<String> incDirs = new ArrayList<>();

        final int limit = args.length;
        for (int i = 0; i < limit; ++i) {
            final String el = args[i];

            if (el.isEmpty()) {
                continue;
            }

            try {
                if (el.charAt(0) == '-') {
                    switch (el) {
                        case "-h":
                        case "--help":
                            dspHelp = true;
                            continue;
                        case "-o":
                            output = args[++i];
                            continue;
                        case "-I":
                            incDirs.add(args[++i]);
                            continue;
                        default:
                            System.out.println("Error: Unsupported option: " + el);
                            errored = true;
                            continue;
                    }
                }

                inpList.add(el);
            } catch (IndexOutOfBoundsException ex) {
                System.out.println("Error: Option " + el + " missing value after");
                errored = true;
                continue;
            }
        }

        if (dspHelp) {
            System.out.println(""
                    + "Nostalgia Assembler\n"
                    + "\n"
                    + "Usage: as [options] file...\n"
                    + "\n"
                    + "Options:\n"
                    + "  -h | --help            Displays this help message\n"
                    + "  -o <file>              Write output to <file>\n"
                    + "  -I <dir>               Add directory to search path");
            return false;
        }

        if (errored) {
            return false;
        }

        boolean ret = true;
        final Assembler assembler = new Assembler();
        try {
            for (final String inc : incDirs) {
                assembler.addSearchDir(inc);
            }
            incDirs = null;

            for (final String inp : inpList) {
                assembler.loadSource(inp);
            }
            ret = !inpList.isEmpty();
            inpList = null;
        } catch (IOException | RuntimeException ex) {
            System.out.println(ex.getMessage());
            return false;
        }

        final byte[] bytes = assembler.assembleAll();
        try {
            Files.write(Paths.get(output), bytes);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
        return ret;
    }
}