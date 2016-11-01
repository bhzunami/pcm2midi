package ch.fhnw.tvver;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class Plot {
    /** the frame **/
    private JFrame frame;

    /** the scroll pane **/
    private JScrollPane scrollPane;

    /** the image gui component **/
    private JPanel panel;

    /** the image **/
    private BufferedImage image;

    /** the last scaling factor to normalize samples **/
    private float scalingFactor = 1;

    /** wheter the plot was cleared, if true we have to recalculate the scaling factor **/
    private boolean cleared = true;

    /** current marker position and color **/
    private int markerPosition = 0;
    private Color markerColor = Color.white;

    public Plot(final String title, final int width, final int height) {
        image = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);

        frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(width + frame.getInsets().left + frame.getInsets().right,
                frame.getInsets().top + frame.getInsets().bottom + height));
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = (Graphics2D) img.getGraphics();
        g.setColor(Color.black);
        g.fillRect(0, 0, width, height);
        g.dispose();
        image = img;
        panel = createPanel();
        // panel.setPreferredSize( new Dimension( width, height ) );
        scrollPane = new JScrollPane(panel);
        frame.getContentPane().add(scrollPane);
        frame.pack();
        frame.setVisible(true);

    }

    private JPanel createPanel() {
        return new JPanel() {

            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                synchronized (image) {
                    g.drawImage(image, 0, 0, null);
                    g.setColor(markerColor);
                    g.drawLine(markerPosition, 0, markerPosition, image.getHeight());
                }

                Thread.yield();

                frame.repaint();
            }

            @Override
            public void update(Graphics g) {
                paint(g);
            }

            public Dimension getPreferredSize() {
                return new Dimension(image.getWidth(), image.getHeight());
            }
        };
    }
    
    
    public void update(float[] data) {
        System.out.println("Add data to plot");
    }

}
