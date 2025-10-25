import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Simple Icon Generator
 * Create a basic icon using Java standard library
 */
public class GenerateIcon {

    public static void main(String[] args) {
        try {
            // Create image
            BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics g = image.createGraphics();
            
            // Set background
            g.setColor(new Color(52, 152, 219)); // Blue background
            g.fillRect(0, 0, 64, 64);
            
            // Draw PLC related graphic elements
            g.setColor(Color.WHITE);
            // Three rectangles representing data
            g.fillRect(16, 16, 12, 32);
            g.fillRect(28, 16, 12, 20);
            g.fillRect(40, 16, 12, 28);
            
            // Center dot
            g.setColor(new Color(231, 76, 60)); // Red
            g.fillOval(28, 28, 8, 8);
            
            g.dispose();
            
            // Save as PNG
            File outputDir = new File("src/main/resources");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            File output = new File(outputDir, "app-icon.png");
            ImageIO.write(image, "png", output);
            
            System.out.println("Icon generated successfully: " + output.getAbsolutePath());
            System.out.println("Note: Generated as PNG format");
            
        } catch (IOException e) {
            System.out.println("Failed to generate icon: " + e.getMessage());
            e.printStackTrace();
        }
    }
}