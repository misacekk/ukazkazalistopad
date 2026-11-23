package cz.pobuda.paneckuzlobse.model;

import cz.pobuda.paneckuzlobse.logic.BoardPath;

public class Pawn {
    private final int id;
    private final Player owner;
    private int currentTileIndex;
    private int totalStepsMoved = 0;
    private boolean inHouse = true;
    private boolean inFinish = false;
    private int finishSlotIndex = -1;

    public Pawn(int id, Player owner) {
        this.id = id;
        this.owner = owner;
        this.currentTileIndex = owner.getStartTileIndex();
    }

    public int getId() { return id; }
    public Player getOwner() { return owner; }
    public int getCurrentTileIndex() { return currentTileIndex; }

    public void stepToTile(int index) {
        this.currentTileIndex = (index % BoardPath.getPathLength() + BoardPath.getPathLength()) % BoardPath.getPathLength();
    }

    public int getTotalStepsMoved() { return totalStepsMoved; }
    public void setTotalStepsMoved(int totalStepsMoved) { this.totalStepsMoved = totalStepsMoved; }
    public void addSteps(int steps) { this.totalStepsMoved += steps; }

    public boolean isInHouse() { return inHouse; }
    public void setInHouse(boolean inHouse) {
        this.inHouse = inHouse;
        if (inHouse) {
            resetToSpawn();
        }
    }

    public void resetToSpawn() {
        this.inHouse = true;
        this.inFinish = false;
        this.finishSlotIndex = -1;
        this.currentTileIndex = owner.getStartTileIndex();
        this.totalStepsMoved = 0;
    }

    public boolean isInFinish() { return inFinish; }
    public void setInFinish(boolean inFinish) {
        this.inFinish = inFinish;
        if (!inFinish) {
            this.finishSlotIndex = -1;
        }
    }

    public int getFinishSlotIndex() { return finishSlotIndex; }
    public void setFinishSlotIndex(int finishSlotIndex) { this.finishSlotIndex = finishSlotIndex; }

    public boolean isAtEntranceTile() {
        return !inHouse && !inFinish && totalStepsMoved >= BoardPath.getPathLength() - 1;
    }

    public int getStepsToEntrance() {
        return Math.max(0, (BoardPath.getPathLength() - 1) - totalStepsMoved);
    }
}