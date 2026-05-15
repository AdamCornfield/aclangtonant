package com.aclangtonant;


public class Ant {
    private int xPos;
    private int yPos;
    private String direction;

    public Ant(int startX, int startY, String startDirection) {
        this.xPos = startX;
        this.yPos = startY;
        this.direction = startDirection;
    }

    /**
     * Roates the ant either clockwise (CW) or counter clockwise (CCW)
     * @param spinDirection
     */
    public void rotateAnt(String spinDirection) {
        if ("CW".equals(spinDirection)) {
            switch (this.direction) {
                case "UP":
                    this.direction = "RIGHT";
                    break;
                case "RIGHT":
                    this.direction = "DOWN";
                    break;
                case "DOWN":
                    this.direction = "LEFT";
                    break;
                case "LEFT":
                    this.direction = "UP";
                    break;
                default:
                    this.direction = "UP";
                    break;
            }
            
        } else {
            switch (this.direction) {
                case "UP":
                    this.direction = "LEFT";
                    break;
                case "RIGHT":
                    this.direction = "UP";
                    break;
                case "DOWN":
                    this.direction = "RIGHT";
                    break;
                case "LEFT":
                    this.direction = "DOWN";
                    break;
                default:
                    this.direction = "UP";
                    break;
            }
        }
    }

    /**
     * Moves the ant a set distance, based on the direction it is currently facing will depend on which coordinate is incremented or decremented.
     * There is logic to make the ant do a full 180 degree turn if it hits the outside wall.
     * @param distance
     */
    public void moveAnt(int distance) {
        switch (this.direction) {
            case "UP":
                // hits top wall
                if ((this.yPos - distance) >= 0) {
                    this.yPos -= distance;
                } else {
                    // Flips ant around
                    rotateAnt("RIGHT");
                    rotateAnt("RIGHT");
                }
                break;
            case "RIGHT":
                // hits right wall
                if ((this.xPos + distance) < gridController.GRID_WIDTH) {
                    this.xPos += distance;
                } else {
                    // Flips ant around
                    rotateAnt("RIGHT");
                    rotateAnt("RIGHT");
                }
                break;
            case "DOWN":
                // hits bottom wall
                if ((this.yPos + distance) < gridController.GRID_HEIGHT) {
                    this.yPos += distance;
                } else {
                    // Flips ant around
                    rotateAnt("RIGHT");
                    rotateAnt("RIGHT");
                }
                break;
            case "LEFT":
                // hits left wall
                if ((this.xPos - distance) >= 0) {
                    this.xPos -= distance;
                } else {
                    // Flips ant around
                    rotateAnt("RIGHT");
                    rotateAnt("RIGHT");
                }
                
                break;
            default:
                
                break;
        }
    }

    public int getXPos() {
        return xPos;
    }

    public int getYPos() {
        return yPos;
    }
}
