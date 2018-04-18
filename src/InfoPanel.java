import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

class InfoPanel {
    Rectangle2D bounds;
    Creature c;
    Button openet;
    boolean draweditor;
    Dimension superwindow;
    int moveid = -1;
    double clickx, clicky;
    Point start;

    public InfoPanel(Rectangle2D r) {
        bounds = r;
        openet = new Button("Edit", new Color(0, 255, 0, 127), new Rectangle2D.Double(0.3, 0.4, 0.4, 0.05));
    }

    public void update(Creature c, Dimension d) {
        this.c = c;
        superwindow = d;
        openet = new Button("Edit", new Color(0, 255, 0, 127), new Rectangle2D.Double(0.3, 0.4, 0.4, 0.05));
    }

    synchronized void draw(Graphics2D g) {
        if (c == null)
            return;
        Drawer d = new Drawer(scale(), g);
        g.setColor(new Color(127, 127, 127, 127));
        d.drawRect(new Rectangle(0, 0, 1, 1));
        g.setColor(Color.BLACK);
        d.drawString(c.getX() + ", " + c.getY(), 0.5, 0.05);
        d.drawString((c.female ? "fe" : "") + "male", 0.5, 0.5);
        d.drawString("Age: "+c.age, 0.5, 0.55);
        d.drawString("Stamina: "+c.stamina, 0.5, 0.6);
        d.drawString("Range: "+c.range, 0.5, 0.65);
        d.drawString("Inner loc: "+c.innerloc.getX()+", "+c.innerloc.getY(), 0.5, 0.7);
        d.drawString("Name: "+c.name, 0.5, 0.75);
        c.net.draw(new Drawer(Arith.shift(Arith.scale(scale(), 0.7, 0.3), 0, -scale().getHeight() / 4), g));
        openet.draw(d);
        if (draweditor)
            drawEditor(g);
    }

    void drawEditor(Graphics2D g) {
        Drawer d = new Drawer(scaleEdit(), g);
        d.setColor(new Color(0, 0, 0, 127));
        d.drawRect(new Rectangle2D.Double(0, 0, 1, 1));
        c.net.draw(new Drawer(Arith.scale(scaleEdit(), 0.9), g));
    }

    void click(MouseEvent p) {
        if (scaleEdit().contains(p.getPoint())) {
            Rectangle2D rect = Arith.scale(scaleEdit(), 0.9);
            Point2D click = new Point2D.Double((p.getX() - rect.getX()) / rect.getWidth(),
                    (p.getY() - rect.getY()) / rect.getHeight());
            if (p.getButton() == MouseEvent.BUTTON1) {
                for (int i = 0; i < c.net.nodes.size(); i++) {
                    NOR node = c.net.nodes.get(i);
                    if (node != null) {
                        if (new Rectangle2D.Double(node.x - 0.005, node.y - 0.009, 0.01, 0.018).contains(click)) {
                            moveid = i;
                            clickx = click.getX();
                            clicky = click.getY();
                            break;
                        }
                    }
                }
            } else if (p.getButton() == MouseEvent.BUTTON3) {
                start = p.getPoint();
            }
        } else {
            Point2D click = new Point2D.Double((p.getX() - scale().getX()) / scale().getWidth(),
                    (p.getY() - scale().getY()) / scale().getHeight());
            if (openet.bounds.contains(click)) {
                draweditor = !draweditor;
            }
        }
    }

    void release(MouseEvent p) {
        if (scaleEdit().contains(p.getPoint())) {
            Rectangle2D rect = Arith.scale(scaleEdit(), 0.9);
            Point2D click = new Point2D.Double((p.getX() - rect.getX()) / rect.getWidth(),
                    (p.getY() - rect.getY()) / rect.getHeight());
            if (p.getButton() == MouseEvent.BUTTON1) {
                if (moveid != -1) {
                    if (Math.abs(clickx - click.getX()) < 0.02 && Math.abs(clicky - click.getY()) < 0.02) {
                        c.net.remove(moveid);
                    } else {
                        c.net.nodes.get(moveid).x = click.getX();
                        c.net.nodes.get(moveid).y = click.getY();
                    }
                    moveid = -1;
                }
            } else if (p.getButton() == MouseEvent.BUTTON3) {
                if (Math.abs(start.x - p.getX()) < 10 && Math.abs(start.y - p.getY()) < 10) {
                    c.net.nodes.add(new NOR(false, new Integer[0], click.getX(), click.getY()));
                } else {
                    Point2D startclick = new Point2D.Double((start.getX() - rect.getX()) / rect.getWidth(),
                            (start.getY() - rect.getY()) / rect.getHeight());
                    for (NOR node : c.net.nodes) {
                        if (Math.abs(node.x - startclick.getX()) < 0.01
                                && Math.abs(node.y - startclick.getY()) < 0.01) {
                            for (int i1 = 0; i1 < c.net.nodes.size(); i1++) {
                                if (Math.abs(c.net.nodes.get(i1).x - click.getX()) < 0.01
                                        && Math.abs(c.net.nodes.get(i1).y - click.getY()) < 0.01) {
                                    node.in.add(i1);
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
            }
        } else {
        }
    }

    void drag(MouseEvent p) {
        if (scaleEdit().contains(p.getPoint())) {
            Rectangle2D rect = Arith.scale(scaleEdit(), 0.9);
            Point2D click = new Point2D.Double((p.getX() - rect.getX()) / rect.getWidth(),
                    (p.getY() - rect.getY()) / rect.getHeight());
            if (moveid != -1) {
                c.net.nodes.get(moveid).x = click.getX();
                c.net.nodes.get(moveid).y = click.getY();
            }
        } else {
        }
    }

    Rectangle2D scale() {
        return new Rectangle((int) (bounds.getX() * superwindow.width), (int) (bounds.getY() * superwindow.height),
                (int) (bounds.getWidth() * superwindow.width), (int) (bounds.getHeight() * superwindow.height));
    }

    Rectangle2D.Double scaleEdit() {
        if (draweditor)
            return new Rectangle2D.Double(superwindow.width / 8, superwindow.height / 8, superwindow.width * 3 / 4,
                    superwindow.height * 3 / 4);
        else
            return new Rectangle2D.Double(0, 0, 0, 0);
    }
}