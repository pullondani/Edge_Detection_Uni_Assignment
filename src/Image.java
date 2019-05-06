import ecs100.*;
import javafx.util.Pair;
import java.awt.*;
import java.io.*;
import java.util.*;

public class Image {
    private int pic[][];
    private int edges[][];
    private int cleanImage[][];

    private int rows, cols;
    private ArrayList<Color> colourImage = new ArrayList<>();

    private ArrayList<Pair<Integer,Integer>> lines = new ArrayList<>();

    private static final int THRESHOLD = 150;
    private static final int MIN_LINE = 5;

    /**
     * Flags
     */
    private boolean loaded = false;
    private boolean cannyLoaded = false;

    public Image() {
        UI.addButton("Load Image", this::loadPicture);
        UI.addButton("Show Image", this::displayImage);
        UI.addButton("Show Edges", this::showEdges);
        UI.addButton("Canny", this::runCanny);
        UI.addButton("Quit", UI::quit);

    }

    public void loadPicture() {
        Scanner sc;
        try {
            sc = new Scanner(new File(UIFileChooser.open()));
        } catch (IOException e) {
            UI.println("File not found " + e);
            return;
        }
        String P3 = sc.next();
        cols = sc.nextInt();
        rows = sc.nextInt();
        int depth = sc.nextInt();
        pic = new int[rows][cols];
        while (sc.hasNext()) {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    int r = sc.nextInt();
                    int g = sc.nextInt();
                    int b = sc.nextInt();
                    int pixel = (r + g + b) / 3;
                    colourImage.add(new Color(r, g, b));

                    pic[i][j] = pixel;
                }
            }
        }
        pictureProcessing();
        loaded = true;
        UI.printMessage("Image loaded");
    }

    public void displayImage() {
        if (loaded) {
            UI.clearGraphics();
            int c = 0;
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    UI.setColor(colourImage.get(c++));
                    UI.fillRect(100 + 2 * j, 50 + 2 * i, 2, 2);
                    //UI.fillRect(LEFT + PIXEL_SIZE * j, TOP + PIXEL_SIZE * i, PIXEL_SIZE, PIXEL_SIZE);
                }
            }
        }

    }

    public void showEdges() {
        if (loaded) {
            UI.clearGraphics();
            drawEdges(edges);
        }
    }

    public void drawEdges(int im[][]) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (im[i][j] >= THRESHOLD) {
                    int pixel = 255;
                    UI.setColor(new Color(pixel, pixel, pixel));
                    UI.fillRect(100 + 2 * j, 50 + 2 * i, 2, 2);
                }
                else{
                    int pixel = 0;
                    UI.setColor(new Color(pixel, pixel, pixel));
                    UI.fillRect(100 + 2 * j, 50 + 2 * i, 2, 2);
                }
            }
        }
        //UI.sleep(50);
    }

    /**
     * pictureProcessing is the function that goes through an image and finds all of the edges in the image
     * it does this by using a 3x3 kernel that is applied on top of the image, this kernel detects vertical
     * and horizontal lines separately and then and them together at the end, which gives us an image that
     * is just the basic outline of the image, this outline can be tweaked by the var THRESHOLD, the lower the
     * THRESHOLD the more detail will be retained
     */

    public void pictureProcessing() {
        int xKernel[][] = new int[][]{{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
        int yKernel[][] = new int[][]{{1, 2, 1}, {0, 0, 0}, {-1, -2, -1}};

        edges = new int[rows][cols];
        for (int i = 1; i < rows - 1; i++) { //start one in, finish one in
            for (int j = 1; j < cols - 1; j++) { //start one in, finish one in

                int hor =
                        xKernel[0][0] * pic[i - 1][j - 1] + //top
                                xKernel[0][1] * pic[i - 1][j] +
                                xKernel[0][2] * pic[i - 1][j + 1] +

                                xKernel[1][0] * pic[i][j - 1] + //middle
                                xKernel[1][2] * pic[i][j + 1] +

                                xKernel[2][0] * pic[i + 1][j - 1] + //bottom
                                xKernel[2][1] * pic[i + 1][j] +
                                xKernel[2][2] * pic[i + 1][j + 1];

                int vert =
                        yKernel[0][0] * pic[i - 1][j - 1] +
                                yKernel[0][1] * pic[i - 1][j] +
                                yKernel[0][2] * pic[i - 1][j + 1] +

                                yKernel[1][0] * pic[i][j - 1] +
                                yKernel[1][2] * pic[i][j + 1] +

                                yKernel[2][0] * pic[i + 1][j - 1] +
                                yKernel[2][1] * pic[i + 1][j] +
                                yKernel[2][2] * pic[i + 1][j + 1];

                int pixOut = (int) Math.sqrt(Math.pow(hor, 2) + Math.pow(vert, 2));

                edges[i][j] = pixOut;
            }
        }
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (edges[i][j] < THRESHOLD) {
                    edges[i][j] = 0;
                }
            }
        }
    }

    /**
     * canny is a function that will take a 2D array that represents an image and go through the whole thing,
     * it will search for where there are any lines that need to be drawn, once a line has been found, the
     * algorithm will continue to follow the line, painting it as it goes to ensure it doesn't go back
     * on itself.
     */
    public void runCanny() {
        if (loaded && !cannyLoaded) {
            canny();
        }
        else if (loaded){
            drawEdges(cleanImage);
        }
    }

    public void canny() {
        int copy[][] = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                copy[i][j] = edges[i][j];
            }

        }
        ArrayList<Pair<Integer,Integer>> coordinates = new ArrayList<>();
        for (int x = 1; x < rows - 1; x++) { //start one in, finish one in
            for (int y = 1; y < cols - 1; y++) {
                if (edges[x][y] >= THRESHOLD) {
                    boolean follow = true;
                    int xLine = x;
                    int yLine = y;
                    while (follow) { // while I am on a line therefore I will get off this line once there is not an edge around
                        int dir = checkDir(xLine, yLine);
                        edges[xLine][yLine] = 0;
                        if (dir == -1) {/*UI.println("End of the line");*/
                            if (lines.size() >= MIN_LINE) {
                                convertList(lines);
                                UI.sleep(100);

                                coordinates.addAll(lines);
                                //pass you the array
                                //reset the array
                                //scara.write(lines);
                            }
                            lines = new ArrayList<>();
                            follow = false;
                        } else if (dir == 0) {
                            edges[xLine-1][yLine] = 0;
                            edges[xLine][yLine-1] = 0;
                            xLine -= 1;
                            yLine -= 1;
                        } else if (dir == 1) {
                            edges[xLine-1][yLine-1] = 0;
                            edges[xLine-1][yLine+1] = 0;
                            xLine -= 1;
                        } else if (dir == 2) {
                            edges[xLine-1][yLine] = 0;
                            edges[xLine][yLine+1] = 0;
                            xLine -= 1;
                            yLine += 1;
                        } else if (dir == 3) {
                            edges[xLine-1][yLine-1] = 0;
                            edges[xLine+1][yLine-1] = 0;
                            yLine -= 1;
                        } else if (dir == 5) {
                            edges[xLine-1][yLine+1] = 0;
                            edges[xLine+1][yLine+1] = 0;
                            yLine += 1;
                        } else if (dir == 6) {
                            edges[xLine][yLine-1] = 0;
                            edges[xLine+1][yLine] = 0;
                            xLine += 1;
                            yLine -= 1;
                        } else if (dir == 7) {
                            edges[xLine+1][yLine-1] = 0;
                            edges[xLine+1][yLine+1] = 0;
                            xLine += 1;
                        } else if (dir == 8) {
                            edges[xLine+1][yLine] = 0;
                            edges[xLine][yLine+1] = 0;
                            xLine += 1;
                            yLine += 1;
                        }

                        if (follow) {
                            lines.add(new Pair<>(xLine, yLine));
                            //coordinates.add(new Pair<>(xLine, yLine));
                        }
                        //drawEdges(edges);
                        //UI.sleep(1);
                    }
                }
            }
        }
        edges = copy;
        convertList(coordinates);
        cannyLoaded = true;
        UI.printMessage("finished the canny algorithm");

    }

    public void convertList(ArrayList<Pair<Integer,Integer>> coord){
        cleanImage = new int[rows][cols];
        for (Pair pair:coord) {
            cleanImage[(int)pair.getKey()][(int)pair.getValue()]=THRESHOLD;
        }
        drawEdges(cleanImage);
    }

    /**
     * checkDir gets passed the current pixel co-ordinates
     * It then goes through and finds the maximum (brightest) pixel value in the surrounding 8 pixels
     * The max is then returned as a direction in regards to the input
     * <p>
     * 0 1 2
     * 3 X 5
     * 6 7 8
     * <p>
     * where X is the input pixel
     * <p>
     * The maximum is used to determine where the line continues, so that the algorithm can follow the line
     * @return if -1 is returned there is an error
     */

    public int checkDir(int x, int y) {
        int max = 0;
        int dir = -1;
        int count = 0;
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if (edges[x + i][y + j] > max && !(i == 0 && j == 0)) {
                    //max = edges[x + i][y + j];
                    dir = count;
                }
                count++;

            }

        }
        return dir;
    }

    /**
     * Prints the image to a file to be saved for later
     */
    public void outputPicture(int picture[][]) {
        PrintWriter out;
        try {
            out = new PrintWriter(new File("edgeDetection.ppm"));
        } catch (IOException e) {
            UI.println("File not found " + e);
            return;
        }


        for (int i = 0; i < picture.length; i++) {
            for (int j = 0; j < picture[0].length; j++) {
                out.print(picture[i][j] + " ");
            }
            out.println();
        }
        out.flush();
        out.close();
    }

    public static void main(String[] args) {
        //RobotArm scara = new RobotArm();
        new Image();
    }
}
