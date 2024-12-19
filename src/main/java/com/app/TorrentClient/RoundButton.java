package com.app.TorrentClient;

import javax.swing.*;
import java.awt.*;

public class RoundButton extends JButton {

    Color bg_color = Color.WHITE;
    Color color = Color.BLACK;
    String text;
    public RoundButton(String text){
        this.text = text;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        super.paintComponent(g);
        Dimension arcs = new Dimension(15, 15); // Border corners arcs {width, height}, change as needed
        int width = getWidth();
        int height = getHeight();
        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(bg_color);
        graphics.fillRoundRect(0, 0, width - 1, height - 1, arcs.width, arcs.height);
        FontMetrics fm = graphics.getFontMetrics();
        Rectangle textBounds = fm.getStringBounds(text, graphics).getBounds();
        int textX = (width - textBounds.width) / 2;  // Center the text horizontally
        int textY = (height - textBounds.height) / 2 + fm.getAscent();  // Center the text vertically
        graphics.setColor(getForeground()); // Set the text color
        graphics.drawString(text, textX, textY); // Draw the text
    }

    @Override
    protected void paintBorder(Graphics g) {
        super.paintBorder(g);
        super.paintBorder(g);
        Dimension arcs = new Dimension(15, 15); // Border corners arcs {width, height}, change as needed
        int width = getWidth();
        int height = getHeight();
        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(color);
        graphics.setStroke(new BasicStroke(1));
        graphics.drawRoundRect(0, 0, width - 1, height - 1, arcs.width, arcs.height);
    }

    public void set_new_background(Color color){
        this.bg_color = color;
        repaint();
    }

    public void set_new_border(Color color){
        this.color = color;
        repaint();
    }
}
