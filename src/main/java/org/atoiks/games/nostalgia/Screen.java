package org.atoiks.games.nostalgia;

import java.io.IOException;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentAdapter;
import java.awt.image.BufferedImage;

import javax.swing.*;
import javax.imageio.ImageIO;

public final class Screen extends JFrame implements MemoryHandler {

    public static final int VIRT_WIDTH      = 80;
    public static final int VIRT_HEIGHT     = 25;

    public static final int UNSCL_WIDTH     = 800;
    public static final int UNSCL_HEIGHT    = 500;

    private static final int CELL_WIDTH     = UNSCL_WIDTH / VIRT_WIDTH;
    private static final int CELL_HEIGHT    = UNSCL_HEIGHT / VIRT_HEIGHT;

    private static final Color[] COLOR_MAP = {
        Color.BLACK,
        Color.RED,
        Color.GREEN,
        Color.YELLOW,
        Color.BLUE,
        Color.MAGENTA,
        Color.CYAN,
        Color.WHITE
    };

    private static final int VGA_FONT_COLS  = 16;
    private static final int VGA_FONT_ROWS  = 17;
    private static final BufferedImage[] VGA_FONT = new BufferedImage[256];

    static {
        if (COLOR_MAP.length != 8) {
            throw new AssertionError("Illegal color map size: " + COLOR_MAP.length);
        }

        try {
            // Load the awesome VGA ROM font and partition it into each character
            final BufferedImage bitmap = ImageIO.read(Screen.class.getResourceAsStream("/zoomed_seabios8x16.png"));
            final int w = bitmap.getWidth() / VGA_FONT_COLS;
            final int h = bitmap.getHeight() / VGA_FONT_ROWS;

            // -1 because last row is blank, we don't care about it
            for (int i = 0; i < VGA_FONT_ROWS - 1; ++i) {
                for (int j = 0; j < VGA_FONT_COLS; ++j) {
                    final int x = j * w;
                    final int y = i * h;
                    VGA_FONT[i * VGA_FONT_COLS + j] = bitmap.getSubimage(x, y, w, h);
                }
            }
        } catch (IOException ex) {
            throw new AssertionError("Cannot read supposed-to-be-existent VGA ROM font file!");
        }
    }

    private float scaleFactor;
    private float transX;
    private float transY;

    // Each short is decomposed into 5 fields:
    //   1_1_111_111_11111111 <== 16 bits (which is a short)
    // (15)         |      (0)
    //
    // [0, 7]   => the character (not quite ASCII? more like DOS codepage)
    // [8, 10]  => foreground color
    // [11, 13] => background color
    // 14       => mirrored over the y axis
    // 15       => underline
    private final short[] memory = new short[VIRT_WIDTH * VIRT_HEIGHT];

    private final JPanel canvas = new JPanel() {

        @Override
        protected void paintComponent(Graphics gr) {
            final Graphics2D g = (Graphics2D) gr;

            g.translate(transX, transY);
            g.scale(scaleFactor, scaleFactor);

            g.setColor(Color.BLACK);
            g.fillRect(0, 0, UNSCL_WIDTH, UNSCL_HEIGHT);

            for (int i = 0; i < VIRT_HEIGHT; ++i) {
                for (int j = 0; j < VIRT_WIDTH; ++j) {
                    this.renderCell(g, i, j);
                }
            }
        }

        private void renderCell(Graphics g, final int row, final int col) {
            final int cell = Short.toUnsignedInt(Screen.this.memory[row * VIRT_WIDTH + col]);

            // Calculate it's on-screen coordinate
            final int x = col * CELL_WIDTH;
            final int y = row * CELL_HEIGHT;

            // Fetch the glyph
            final BufferedImage original = VGA_FONT[cell & 0xFF];

            // Query the glyph styles

            final int bg = Screen.COLOR_MAP[(cell >> 11) & 0x7].getRGB() & 0xFFFFFF;
            final int fg = Screen.COLOR_MAP[(cell >>  8) & 0x7].getRGB() & 0xFFFFFF;
            final boolean inv = ((cell >> 14) & 1) != 0;
            final boolean udl = ((cell >> 15) & 1) != 0;

            final BufferedImage glyph;
            if (!inv && bg == 0 && fg == 0xFFFFFF) {
                // the original glyph is already non-inverted, fg=white, bg=black
                glyph = original;
            } else {
                // Apply transformations to it
                final int w = original.getWidth();
                final int h = original.getHeight();
                glyph = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

                // Foreground originally is white, Background originally is black
                for (int ky = 0; ky < h; ++ky) {
                    for (int kx = 0; kx < w; ++kx) {
                        // RGB is only 24 bits: need explicit mask.
                        final int old = original.getRGB(kx, ky) & 0xFFFFFF;
                        glyph.setRGB(inv ? w - kx - 1 : kx, ky, old == 0 ? bg : fg);
                    }
                }
            }

            g.drawImage(glyph, x, y, CELL_WIDTH, CELL_HEIGHT, null);

            if (udl) {
                g.setColor(Screen.COLOR_MAP[(cell >>  8) & 0x7]);

                final int baseline = y + CELL_HEIGHT - 2;
                g.drawLine(x, baseline, x + CELL_WIDTH, baseline);
            }
        }
    };

    public Screen() {
        super("Atoiks Games - Nostalgia...");
        super.setSize(UNSCL_WIDTH, UNSCL_HEIGHT);
        super.setLocationRelativeTo(null);
        super.setDefaultCloseOperation(3);

        super.getContentPane().add(this.canvas);

        super.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                final float x = Screen.this.getWidth();
                final float y = Screen.this.getHeight();

                final float scaleRatioX = x / UNSCL_WIDTH;
                final float scaleRatioY = y / UNSCL_HEIGHT;

                // Make sure aspect ratio is maintained
                final float sf = Math.min(scaleRatioX, scaleRatioY);
                Screen.this.scaleFactor = sf;

                // Add appropriate padding
                Screen.this.transX = (x - UNSCL_WIDTH * sf) / 2;
                Screen.this.transY = (y - UNSCL_HEIGHT * sf) / 2;
            }
        });
    }

    @Override
    public int getCapacity() {
        return VIRT_WIDTH * VIRT_HEIGHT * 2;
    }

    @Override
    public byte readOffset(final int offset) {
        final short cell = this.memory[offset / 2];
        return (byte) (Short.toUnsignedInt(cell) >> (4 * (offset % 2)));
    }

    @Override
    public void writeOffset(final int offset, byte b) {
        this.internalWrite(offset, b);
        this.repaint();
    }

    private void internalWrite(final int offset, byte b) {
        final int flag = 1 - offset % 2;
        final int mask = 0xFF << (8 * flag);
        final int updt = Byte.toUnsignedInt(b) << 8 * (1 - flag);
        this.memory[offset / 2] = (short) ((this.memory[offset / 2] & mask) | updt);
    }

    public static byte makeCellAttr(int fg, int bg) {
        return makeCellAttr(fg, bg, false, false);
    }

    public static byte makeCellAttr(int fg, int bg, boolean inv, boolean blink) {
        final int value
                = (blink ? 1 << 7 : 0)
                | (inv ? 1 << 6 : 0)
                | ((bg & 0x7) << 3)
                | (fg & 0x7);
        return (byte) value;
    }

    // Convenience method
    public void setEntry(int x, int y, byte ch, byte attr) {
        final int unscaled = y * VIRT_WIDTH + x;
        this.internalWrite(2 * unscaled + 1, attr);
        this.internalWrite(2 * unscaled, ch);
    }

    public void flush() {
        this.repaint();
    }
}