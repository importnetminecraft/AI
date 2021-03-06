package managers;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import calc.Settings;
import geom.Point;
import geom.Rectangle;
import gui.InfoPanel;
import gui.Terminal;
import sprites.Creature;
import sprites.Food;
import sprites.Sprite;

public class Main extends JFrame {
    private static final long serialVersionUID = 1L;
    EvolvePanel panel = new EvolvePanel();

    public Main() {
        super("AI");
        setSize(2900, 1900);
        setLocation(20, 20);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
        setContentPane(panel);
        // setAlwaysOnTop(true);
        addKeyListener(panel.controller);
    }

    public static void main(String[] arguments) {
        Main clock = new Main();
        clock.setVisible(true);
    }
}

class EvolvePanel extends JPanel implements Runnable {
    private static final long serialVersionUID = 1L;
    // region gen vars
    ControlManager controller = new ControlManager(this);
    Random rnd = new Random();
    final Thread runner, painter, creater;
    AffineTransform tx = new AffineTransform();
    InfoPanel info = new InfoPanel(new Rectangle2D.Double(5.0 / 7, 1.0 / 20, 3.0 / 11, 9.0 / 10));
    final QuadTree tree = new QuadTree();
    QuadTree safeclone = new QuadTree(tree);
    final HashSet<Sprite> queue = new HashSet<>();
    boolean paused = false;
    Terminal terminal = new Terminal(new Rectangle2D.Double(0, 0, getWidth(), getHeight()), tree, controller);

    static BufferedImage backgroundImg = new BufferedImage(2 * Settings.range, 2 * Settings.range,
            BufferedImage.TYPE_INT_RGB);
    static {
        try {
            backgroundImg = ImageIO.read(new File("C:/Users/Public/eclipse-workspace/AI/dean-kamen.jpg"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // endregion
    EvolvePanel() {
        // region add listeners
        addMouseListener(controller);
        addMouseMotionListener(controller);
        addMouseWheelListener(controller);
        // endregion
        setBackground(Color.WHITE);

        (runner = new Thread(this, "That Main Thing")).start();

        (painter = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    terminal.update(new Rectangle2D.Double(0, 0, getWidth(), getHeight()));
                    repaint();
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "That Paint Thing")).start();

        (creater = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    if (safeclone.count(Creature.class) < Settings.creaturecap) {// create new creature
                        Creature cr = new Creature(rnd.nextInt(2 * Settings.range) - Settings.range,
                                rnd.nextInt(2 * Settings.range) - Settings.range, rnd.nextInt(360), Settings.range);
                        cr.net.change(Settings.startingcomplexity);
                        if (cr.net.nodes.get(Settings.inputcount + 6).in.size() > 0
                                && cr.net.nodes.get(Settings.inputcount + 5).in.size()
                                        + cr.net.nodes.get(Settings.inputcount + 4).in.size() > 0)
                            synchronized (queue) {
                                queue.add(cr);
                            }
                    }
                    if (safeclone.count(Food.class) < Settings.foodcap) {// create new food
                        synchronized (queue) {
                            queue.add(new Food(rnd.nextInt(2 * Settings.range) - Settings.range,
                                    rnd.nextInt(2 * Settings.range) - Settings.range, rnd.nextInt(14) + 2, false,
                                    Settings.range));
                        }
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "That Creating Thing")).start();
    }

    public void run() {
        while (true) {
            if (!paused) {
                tree.reorg();
                tree.runSP();
            }
            synchronized (queue) {
                tree.addAll(queue);
                queue.clear();
            }
            safeclone = new QuadTree(tree);
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void paintComponent(Graphics comp) {
        Graphics2D comp2D = (Graphics2D) comp;
        Font type = new Font("Serif", Font.BOLD, 24);
        comp2D.setFont(type);
        comp2D.clearRect(0, 0, getWidth(), getHeight());
        AffineTransform prevTrans = comp2D.getTransform();
        comp2D.transform(controller.tx);
        comp2D.drawImage(backgroundImg, -Settings.range, -Settings.range, 2 * Settings.range, 2 * Settings.range, null);
        comp2D.setColor(Color.black);
        // safeclone.drawtree(comp2D, Settings.rangeRect);
        Point2D topleft = controller.project(new Point2D.Double(0, 0)),
                bottomright = controller.project(new Point2D.Double(getWidth(), getHeight()));
        Rectangle randrect = new Rectangle(new Point(topleft.getX(), -topleft.getY()),
                new Point(bottomright.getX(), -bottomright.getY()));
        safeclone.query(randrect, Settings.rangeRect).forEach(s -> s.draw(comp2D));
        comp2D.setTransform(prevTrans);

        info.update(controller.focus, getSize());
        info.draw(comp2D);

        if (paused) {
            comp2D.setColor(Color.WHITE);
            comp2D.drawString("paused", 0, 20);
        }
        terminal.draw(comp2D);
    }
}