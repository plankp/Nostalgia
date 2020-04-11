package org.atoiks.games.nostalgia;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class App {

    public static void main(String[] args) throws Exception {
        boolean errored = false;
        boolean dspHelp = false;

        boolean disassemble = false;
        final ArrayList<String> asArgs = new ArrayList<>();
        final ArrayList<String> emuArgs = new ArrayList<>();

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
                        case "-I":
                            asArgs.add(el);
                            asArgs.add(args[++i]);
                            continue;
                        case "--dis":
                            disassemble = true;
                            continue;
                        case "--fast":
                        case "--slow":
                            emuArgs.add(el);
                            continue;
                        default:
                            System.out.println("Error: Unsupported option: " + el);
                            errored = true;
                            continue;
                    }
                }

                asArgs.add(el);
            } catch (IndexOutOfBoundsException ex) {
                System.out.println("Error: Option " + el + " missing value after");
                errored = true;
                continue;
            }
        }

        if (dspHelp) {
            System.out.println(""
                    + "Nostalgia\n"
                    + "\n"
                    + "Usage: nostalgia [options] file...\n"
                    + "\n"
                    + "Options:\n"
                    + "  -h | --help            Displays this help message\n"
                    + "  -I <dir>               Add directory to search path when assembling the kernel\n"
                    + "  --dis                  Disassemble the kernel (that was just assembled)\n"
                    + "  --fast                 Runs at (relatively) fast mode\n"
                    + "  --slow                 Runs at (relatively) slow mode [default]\n"
                    + "\n"
                    + "Note: the file will be loaded at 0x4000");
            return;
        }

        if (errored) {
            return;
        }

        // Create a file temporary
        final Path tempFile = Files.createTempFile(null, null);
        try {
            final String tempFilename = tempFile.toRealPath() + "";

            // Run assembler
            asArgs.add("-o");
            asArgs.add(tempFilename);
            final boolean res = org.atoiks.games.nostalgia.toolchain.LegacyAssembler.entry(asArgs.toArray(new String[0]));
            asArgs.clear();

            if (res) {
                // Run disassembler
                if (disassemble) {
                    org.atoiks.games.nostalgia.toolchain.LegacyDisassembler.main(new String[] { tempFilename });
                }

                // Run emulator
                emuArgs.add(tempFilename);
            }
            org.atoiks.games.nostalgia.toolchain.NostalgiaEmulator.main(emuArgs.toArray(new String[0]));
            emuArgs.clear();
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
