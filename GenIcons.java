import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Генерирует survival_icons.png (16x8) для ресурспака ToughSurvival.
 * Иконка 0 (x=0): термометр | Иконка 1 (x=8): капля воды
 * Пиксели белые — плагин окрашивает их через § коды.
 */
public class GenIcons {
    public static void main(String[] args) throws Exception {
        String outDir = args.length > 0 ? args[0]
            : "resourcepack-src/assets/minecraft/textures/font";

        new File(outDir).mkdirs();

        BufferedImage img = new BufferedImage(16, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, 16, 8);
        g.dispose();

        // ── Термометр (иконка 0, x offset = 0) ──────────────────────────────
        String[] thermo = {
            "...X....",
            "..X.X...",
            "..X.X...",
            "..X.X...",
            ".XXXXX..",
            ".XXXXX..",
            ".XXXXX..",
            "..XXX..."
        };
        paintIcon(img, 0, thermo);

        // ── Капля воды (иконка 1, x offset = 8) ──────────────────────────────
        String[] drop = {
            "...X....",
            "..XXX...",
            ".XXXXX..",
            "XXXXXXX.",
            "XXXXXXX.",
            "XXXXXXX.",
            ".XXXXX..",
            "..XXX..."
        };
        paintIcon(img, 8, drop);

        File out = new File(outDir, "survival_icons.png");
        ImageIO.write(img, "PNG", out);
        System.out.println("OK: " + out.getAbsolutePath());
    }

    static void paintIcon(BufferedImage img, int xOff, String[] rows) {
        int white = new Color(255, 255, 255, 255).getRGB();
        for (int y = 0; y < 8; y++) {
            String row = y < rows.length ? rows[y] : "";
            for (int x = 0; x < 8; x++) {
                if (x < row.length() && row.charAt(x) == 'X') {
                    img.setRGB(xOff + x, y, white);
                }
            }
        }
    }
}
