package org.atoiks.games.nostalgia.toolchain;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

// For now, we just use the old disassembler...
import org.atoiks.games.nostalgia.Disassembler;

public final class LegacyDisassembler {

   public static void main(String[] args) {
       boolean errored = false;
       boolean dspHelp = false;

       ArrayList<String> inpList = new ArrayList<>();

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
                    + "Nostalgia Disassembler\n"
                    + "\n"
                    + "Usage: dis [options] file...\n"
                    + "\n"
                    + "Options:\n"
                    + "  -h | --help            Displays this help message");
            return;
        }

        if (errored) {
            return;
        }

        try {
            for (final String inp : inpList) {
                System.out.println("File " + inp + ":");
                final byte[] bytes = Files.readAllBytes(Paths.get(inp));
                new Disassembler(System.out, ByteBuffer.wrap(bytes)).disassembleAll();
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            return;
        }
    }
}