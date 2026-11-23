package cz.pobuda.paneckuzlobse.controller;

import cz.pobuda.paneckuzlobse.App;
import cz.pobuda.paneckuzlobse.logic.BoardPath;
import cz.pobuda.paneckuzlobse.model.HeroClass;
import cz.pobuda.paneckuzlobse.model.Pawn;
import cz.pobuda.paneckuzlobse.model.Player;
import cz.pobuda.paneckuzlobse.model.TilePosition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.util.Duration;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GameController {

    @FXML private StackPane selectionContainer;
    @FXML private VBox modeSelectionBox;
    @FXML private VBox heroSelectionBox;
    @FXML private Label selectionTitleLabel;

    @FXML private Button btnRytir, btnCarodejnice, btnBandita;
    @FXML private Button btnNekromant, btnElfka, btnKralovna;

    @FXML private HBox gameContent;
    @FXML private Pane boardContainer;
    @FXML private VBox playersContainer;
    @FXML private VBox bottomBar;
    @FXML private Label statusLabel;
    @FXML private Label roundLabel;

    @FXML private VBox victoryOverlay;
    @FXML private ImageView winnerHeroCardView;
    @FXML private Label winnerTitleLabel;
    @FXML private Label winnerStatsLabel;

    @FXML private ImageView activeHeroCardView;
    @FXML private ScrollPane logScrollPane;
    @FXML private VBox gameLogBox;

    private Pane tokenLayer;
    private ImageView diceImageView;
    private AudioClip diceRollSound;

    private int targetPlayerCount = 4;
    private final List<Player> players = new ArrayList<>();
    private final Map<Pawn, ImageView> pawnTokenMap = new HashMap<>();
    private int currentPlayerIndex = 0;
    private int roundNumber = 1;

    private boolean waitingForPawnSelection = false;
    private boolean hasRolledInTurn = false;
    private List<Pawn> movablePawnsCurrentTurn = new ArrayList<>();
    private boolean canSpawnCurrentTurn = false;
    private int lastRolledDiceValue = 0;

    private final Random random = new Random();

    private static final double BOARD_WIDTH = 800.0;
    private static final double BOARD_HEIGHT = 800.0;
    private static final double TOKEN_SIZE = 45.0;
    private static final double DICE_SIZE = 100.0;

    @FXML
    public void initialize() {
        if (gameContent != null) gameContent.setVisible(false);
        if (modeSelectionBox != null) modeSelectionBox.setVisible(true);
        if (heroSelectionBox != null) heroSelectionBox.setVisible(false);
        if (selectionContainer != null) {
            selectionContainer.setVisible(true);
            selectionContainer.setManaged(true);
        }
        if (bottomBar != null) {
            bottomBar.setVisible(false);
            bottomBar.setManaged(false);
        }
        if (victoryOverlay != null) {
            victoryOverlay.setVisible(false);
            victoryOverlay.setManaged(false);
        }

        loadSoundEffects();
        setupBoardUI();
    }

    private void loadSoundEffects() {
        try {
            URL soundUrl = getClass().getResource("/cz/pobuda/paneckuzlobse/sounds/dicerollsoundeffect.mp3");
            if (soundUrl != null) {
                diceRollSound = new AudioClip(soundUrl.toExternalForm());
                diceRollSound.setVolume(0.5);
            } else {
                System.err.println("Upozornění: Zvuk dicerollsoundeffect.mp3 nebyl nalezen ve složce /sounds/.");
            }
        } catch (Exception e) {
            System.err.println("Chyba při načítání zvuku kostky: " + e.getMessage());
        }
    }

    private void playDiceRollSound() {
        if (diceRollSound != null) {
            diceRollSound.play();
        }
    }

    private void addLog(String message) {
        if (gameLogBox == null) return;
        Label logEntry = new Label("• " + message);
        logEntry.setWrapText(true);
        logEntry.setStyle("-fx-text-fill: #DCDCDC; -fx-font-size: 12px;");
        gameLogBox.getChildren().add(logEntry);

        if (logScrollPane != null) {
            Platform.runLater(() -> logScrollPane.setVvalue(1.0));
        }
    }

    @FXML private void zvolitRezim2() { startHeroSelection(2); }
    @FXML private void zvolitRezim3() { startHeroSelection(3); }
    @FXML private void zvolitRezim4() { startHeroSelection(4); }

    private void startHeroSelection(int count) {
        this.targetPlayerCount = count;
        BoardPath.setPlayerCount(count);
        setupBoardUI();

        if (modeSelectionBox != null) modeSelectionBox.setVisible(false);
        if (heroSelectionBox != null) heroSelectionBox.setVisible(true);
        updateHeroSelectionUI();
    }

    @FXML private void zvolitRytire() { addPlayerHero(HeroClass.RYTIR); }
    @FXML private void zvolitCarodejnici() { addPlayerHero(HeroClass.CARODEJNICE); }
    @FXML private void zvolitBanditu() { addPlayerHero(HeroClass.BANDITA); }
    @FXML private void zvolitNekromanta() { addPlayerHero(HeroClass.NEKROMANT); }
    @FXML private void zvolitElfku() { addPlayerHero(HeroClass.ELFKA); }
    @FXML private void zvolitKralovnu() { addPlayerHero(HeroClass.KRALOVNA); }

    private void addPlayerHero(HeroClass heroClass) {
        int newPlayerId = players.size() + 1;
        Player p = new Player(newPlayerId, heroClass);
        players.add(p);

        for (Pawn pawn : p.getPawns()) {
            ImageView tokenView = new ImageView();
            tokenView.setFitWidth(TOKEN_SIZE);
            tokenView.setFitHeight(TOKEN_SIZE);
            tokenView.setPreserveRatio(true);

            InputStream is = getClass().getResourceAsStream("/cz/pobuda/paneckuzlobse/images/" + heroClass.getImageFileName());
            if (is != null) tokenView.setImage(new Image(is));

            tokenView.setOnMouseClicked(e -> handlePawnClick(pawn));

            pawnTokenMap.put(pawn, tokenView);
            if (tokenLayer != null) tokenLayer.getChildren().add(tokenView);

            updatePawnPosition(pawn);
        }

        if (players.size() < targetPlayerCount) {
            updateHeroSelectionUI();
        } else {
            if (selectionContainer != null) {
                selectionContainer.setVisible(false);
                selectionContainer.setManaged(false);
            }
            if (gameContent != null) gameContent.setVisible(true);
            if (bottomBar != null) {
                bottomBar.setVisible(true);
                bottomBar.setManaged(true);
            }
            currentPlayerIndex = 0;
            roundNumber = 1;
            addLog("Hra začala! (" + targetPlayerCount + " hráči)");
            updatePlayersPanel();
            startTurn();
        }
    }

    private void updateHeroSelectionUI() {
        int nextPlayerNum = players.size() + 1;
        if (selectionTitleLabel != null) {
            selectionTitleLabel.setText("Hráč " + nextPlayerNum + ": Vyber si svého hrdinu");
        }

        disableIfTaken(btnRytir, HeroClass.RYTIR);
        disableIfTaken(btnCarodejnice, HeroClass.CARODEJNICE);
        disableIfTaken(btnBandita, HeroClass.BANDITA);
        disableIfTaken(btnNekromant, HeroClass.NEKROMANT);
        disableIfTaken(btnElfka, HeroClass.ELFKA);
        disableIfTaken(btnKralovna, HeroClass.KRALOVNA);
    }

    private void disableIfTaken(Button btn, HeroClass heroClass) {
        if (btn == null) return;
        boolean taken = players.stream().anyMatch(p -> p.getHeroClass() == heroClass);
        btn.setDisable(taken);
    }

    private Player getCurrentPlayer() { return players.get(currentPlayerIndex); }

    private void startTurn() {
        Player p = getCurrentPlayer();
        p.resetTurn();

        waitingForPawnSelection = false;
        hasRolledInTurn = false;
        movablePawnsCurrentTurn.clear();
        canSpawnCurrentTurn = false;

        if (roundLabel != null) {
            roundLabel.setText(String.valueOf(roundNumber));
        }

        updateActiveHeroCard();
        updatePlayersPanel();

        if (statusLabel != null) {
            statusLabel.setText("Na tahu je Hráč " + p.getId());
        }
    }

    private void nextTurn() {
        int attempts = 0;
        do {
            int prevIndex = currentPlayerIndex;
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            if (currentPlayerIndex <= prevIndex) {
                roundNumber++;
            }
            attempts++;
            Player p = getCurrentPlayer();

            if (!p.isFinished()) {
                break;
            }
        } while (attempts < players.size() * 2);

        if (getCurrentPlayer().isFinished()) {
            if (statusLabel != null) statusLabel.setText("Hra skončila!");
            return;
        }

        startTurn();
    }

    @FXML
    private void hoditKostkou() {
        Player p = getCurrentPlayer();
        if (p.hasRolledThisTurn() || hasRolledInTurn || waitingForPawnSelection) {
            if (statusLabel != null) statusLabel.setText("V tomto tahu už jsi házel!");
            return;
        }

        p.setHasRolledThisTurn(true);
        hasRolledInTurn = true;
        playDiceRollSound();

        Timeline diceAnimation = new Timeline();
        for (int i = 0; i < 10; i++) {
            KeyFrame kf = new KeyFrame(Duration.millis(80 * (i + 1)), event -> setDiceImage(random.nextInt(6) + 1));
            diceAnimation.getKeyFrames().add(kf);
        }

        diceAnimation.setOnFinished(event -> {
            int diceValue = random.nextInt(6) + 1;
            setDiceImage(diceValue);
            this.lastRolledDiceValue = diceValue;
            addLog("Hráč " + p.getId() + " hodil " + diceValue + ".");

            canSpawnCurrentTurn = (diceValue == 6) && (p.getPawnInHouse() != null);
            movablePawnsCurrentTurn = getMovablePawns(p, diceValue);

            if (!canSpawnCurrentTurn && movablePawnsCurrentTurn.isEmpty()) {
                if (statusLabel != null) {
                    statusLabel.setText("Hráč " + p.getId() + " hodil " + diceValue + ", ale nemá žádný možný tah.");
                }
                nextTurn();
                return;
            }

            if (canSpawnCurrentTurn && movablePawnsCurrentTurn.isEmpty()) {
                executeSpawn(p.getPawnInHouse());
            } else if (!canSpawnCurrentTurn && movablePawnsCurrentTurn.size() == 1) {
                executePawnMove(movablePawnsCurrentTurn.get(0), diceValue);
            } else {
                waitingForPawnSelection = true;
                if (statusLabel != null) {
                    statusLabel.setText("Hodil jsi " + diceValue + "! Klikni na figurku v pravém panelu nebo na desce.");
                }
            }
        });

        diceAnimation.play();
    }

    private int getTargetFinishSlot(Player p, Pawn pawn, int steps) {
        if (pawn.isInHouse() || pawn.isInFinish()) {
            return -1;
        }

        int entranceSteps = BoardPath.getPathLength() - 1;
        int stepsToEntrance = entranceSteps - pawn.getTotalStepsMoved();
        if (steps <= stepsToEntrance) {
            return -2;
        }

        int finishStep = steps - stepsToEntrance;
        int targetSlot = finishStep - 1;
        if (targetSlot < 0 || targetSlot >= Player.TARGET_SCORE) {
            return -1;
        }

        if (p.isFinishSlotOccupied(targetSlot)) {
            return -1;
        }

        return targetSlot;
    }

    private boolean canPawnMove(Player p, Pawn pawn, int steps) {
        if (pawn.isInHouse() || pawn.isInFinish()) return false;
        return getTargetFinishSlot(p, pawn, steps) != -1;
    }

    private List<Pawn> getMovablePawns(Player p, int steps) {
        List<Pawn> movable = new ArrayList<>();
        for (Pawn pawn : p.getPawns()) {
            if (!pawn.isInHouse() && !pawn.isInFinish() && canPawnMove(p, pawn, steps)) {
                movable.add(pawn);
            }
        }
        return movable;
    }

    private void handlePawnClick(Pawn clickedPawn) {
        Player p = getCurrentPlayer();

        if (!waitingForPawnSelection) return;
        if (clickedPawn.getOwner() != p) return;

        if (clickedPawn.isInHouse()) {
            if (canSpawnCurrentTurn) {
                waitingForPawnSelection = false;
                executeSpawn(clickedPawn);
            }
        } else if (!clickedPawn.isInFinish() && movablePawnsCurrentTurn.contains(clickedPawn)) {
            waitingForPawnSelection = false;
            executePawnMove(clickedPawn, lastRolledDiceValue);
        }
    }

    private void executeSpawn(Pawn pawn) {
        Player p = pawn.getOwner();
        pawn.setInHouse(false);
        pawn.stepToTile(p.getStartTileIndex());
        pawn.setTotalStepsMoved(0);
        updatePawnPosition(pawn);
        addLog("Hráč " + p.getId() + " nasadil novou figurku.");
        checkKickout(pawn);
        updatePlayersPanel();

        p.setHasRolledThisTurn(false);
        hasRolledInTurn = false;
        if (statusLabel != null) {
            statusLabel.setText("Hráč " + p.getId() + " nasadil novou figurku! Hází znovu.");
        }
    }

    private void executePawnMove(Pawn pawn, int diceValue) {
        Player p = pawn.getOwner();
        if (pawn.isInHouse() || pawn.isInFinish()) {
            nextTurn();
            return;
        }

        int targetFinishSlot = getTargetFinishSlot(p, pawn, diceValue);
        if (targetFinishSlot == -1) {
            nextTurn();
            return;
        }

        int entranceSteps = BoardPath.getPathLength() - 1;

        if (targetFinishSlot >= 0) {
            int stepsToEntrance = Math.max(0, entranceSteps - pawn.getTotalStepsMoved());
            animatePawnMovement(pawn, stepsToEntrance, () -> {
                Timeline enterFinish = new Timeline(new KeyFrame(Duration.millis(200), event -> {
                    pawn.setInFinish(true);
                    pawn.setFinishSlotIndex(targetFinishSlot);
                    updatePawnPosition(pawn);
                }));
                enterFinish.setOnFinished(event -> {
                    updatePlayersPanel();
                    addLog("Hráč " + p.getId() + " zajel s figurkou na " + (targetFinishSlot + 1) + ". místo v domečku! (" + p.getScore() + "/4)");

                    if (p.isFinished()) {
                        zobrazitVitezneOkno(p);
                        return;
                    }

                    if (diceValue == 6) {
                        p.setHasRolledThisTurn(false);
                        hasRolledInTurn = false;
                        if (statusLabel != null) {
                            statusLabel.setText("Hodil jsi 6! Házíš znovu.");
                        }
                    } else {
                        nextTurn();
                    }
                });
                enterFinish.play();
            });
            return;
        }

        animatePawnMovement(pawn, diceValue, () -> {
            checkKickout(pawn);
            updatePlayersPanel();

            if (diceValue == 6) {
                p.setHasRolledThisTurn(false);
                hasRolledInTurn = false;
                if (statusLabel != null) {
                    statusLabel.setText("Hodil jsi 6! Házíš znovu.");
                }
            } else {
                nextTurn();
            }
        });
    }

    private void checkKickout(Pawn attackerPawn) {
        if (attackerPawn.isInHouse() || attackerPawn.isInFinish()) return;

        for (Player defender : players) {
            if (defender == attackerPawn.getOwner()) continue;

            for (Pawn defenderPawn : defender.getPawns()) {
                if (!defenderPawn.isInHouse() && !defenderPawn.isInFinish()) {
                    if (defenderPawn.getCurrentTileIndex() == attackerPawn.getCurrentTileIndex()) {
                        defenderPawn.resetToSpawn();
                        updatePawnPosition(defenderPawn);
                        addLog("Hráč " + attackerPawn.getOwner().getId() + " vyhodil figurku Hráče " + defender.getId() + "!");
                    }
                }
            }
        }
    }

    private void animatePawnMovement(Pawn pawn, int steps, Runnable onComplete) {
        if (steps <= 0) {
            if (onComplete != null) onComplete.run();
            return;
        }

        Timeline timeline = new Timeline();
        for (int i = 1; i <= steps; i++) {
            final int stepNum = i;
            KeyFrame kf = new KeyFrame(Duration.millis(200 * stepNum), event -> {
                pawn.stepToTile(pawn.getCurrentTileIndex() + 1);
                pawn.addSteps(1);
                updatePawnPosition(pawn);
            });
            timeline.getKeyFrames().add(kf);
        }
        timeline.setOnFinished(event -> {
            if (onComplete != null) onComplete.run();
        });
        timeline.play();
    }

    private void updatePawnPosition(Pawn pawn) {
        ImageView token = pawnTokenMap.get(pawn);
        if (token == null) return;

        if (pawn.isInHouse()) {
            token.setVisible(false);
            return;
        }

        token.setVisible(true);
        double offset = TOKEN_SIZE / 2.0;

        if (pawn.isInFinish()) {
            int slot = pawn.getFinishSlotIndex() >= 0 ? pawn.getFinishSlotIndex() : pawn.getId();
            TilePosition finishPos = BoardPath.getFinishPosition(pawn.getOwner().getSlotIndex(), slot);
            token.setLayoutX(finishPos.getX() - offset);
            token.setLayoutY(finishPos.getY() - offset);
        } else {
            TilePosition tile = BoardPath.getTile(pawn.getCurrentTileIndex());
            token.setLayoutX(tile.getX() - offset);
            token.setLayoutY(tile.getY() - offset);
        }
    }

    private void setDiceImage(int diceValue) {
        if (diceImageView == null) return;
        InputStream is = getClass().getResourceAsStream("/cz/pobuda/paneckuzlobse/images/d6_red_" + diceValue + ".png");
        if (is != null) diceImageView.setImage(new Image(is));
    }

    private void setupBoardUI() {
        if (boardContainer == null) return;
        boardContainer.getChildren().clear();

        String imageName;
        if (targetPlayerCount == 2) {
            imageName = "/cz/pobuda/paneckuzlobse/images/boardfor2.png";
        } else if (targetPlayerCount == 3) {
            imageName = "/cz/pobuda/paneckuzlobse/images/boardfor3.png";
        } else {
            imageName = "/cz/pobuda/paneckuzlobse/images/deskapro4-3.png";
        }

        InputStream is = getClass().getResourceAsStream(imageName);
        if (is == null) {
            is = getClass().getResourceAsStream("/cz/pobuda/paneckuzlobse/images/deskapro4-3.png");
        }

        ImageView boardImage = new ImageView();
        if (is != null) {
            boardImage.setImage(new Image(is));
        }
        boardImage.setFitWidth(BOARD_WIDTH);
        boardImage.setFitHeight(BOARD_HEIGHT);

        tokenLayer = new Pane();
        tokenLayer.setPrefSize(BOARD_WIDTH, BOARD_HEIGHT);
        tokenLayer.setMaxSize(BOARD_WIDTH, BOARD_HEIGHT);

        diceImageView = new ImageView();
        diceImageView.setFitWidth(DICE_SIZE);
        diceImageView.setFitHeight(DICE_SIZE);
        diceImageView.setLayoutX((BOARD_WIDTH - DICE_SIZE) / 2.0);
        diceImageView.setLayoutY((BOARD_HEIGHT - DICE_SIZE) / 2.0);

        setDiceImage(1);

        diceImageView.setCursor(Cursor.HAND);
        diceImageView.setOnMouseClicked(e -> hoditKostkou());

        tokenLayer.getChildren().add(diceImageView);
        boardContainer.getChildren().addAll(boardImage, tokenLayer);
    }

    private String getHeroCardFileName(HeroClass hc) {
        return switch (hc) {
            case RYTIR -> "rytirnahled.png";
            case CARODEJNICE -> "carodejnicenahled.png";
            case BANDITA -> "banditanahled.png";
            case NEKROMANT -> "nekromantnahled.png";
            case ELFKA -> "elfkanahled.png";
            case KRALOVNA -> "kralovnanahled.png";
        };
    }

    private String getHeroCzechName(HeroClass hc) {
        return switch (hc) {
            case RYTIR -> "Rytíř";
            case CARODEJNICE -> "Čarodějnice";
            case BANDITA -> "Bandita";
            case NEKROMANT -> "Nekromant";
            case ELFKA -> "Elfka";
            case KRALOVNA -> "Královna";
        };
    }

    private void updateActiveHeroCard() {
        if (activeHeroCardView == null || players.isEmpty()) return;
        Player p = getCurrentPlayer();
        HeroClass hc = p.getHeroClass();

        InputStream is = getClass().getResourceAsStream("/cz/pobuda/paneckuzlobse/images/" + getHeroCardFileName(hc));
        if (is != null) {
            activeHeroCardView.setImage(new Image(is));
        }
    }

    private void updatePlayersPanel() {
        if (playersContainer == null) return;
        playersContainer.getChildren().clear();

        Label title = new Label("HRÁČI A FIGURKY");
        title.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 18px; -fx-font-weight: bold;");
        playersContainer.getChildren().add(title);

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            boolean isCurrent = (i == currentPlayerIndex);

            VBox playerBox = new VBox(8);
            String borderStyle = isCurrent
                    ? "-fx-background-color: #2c2c2e; -fx-border-color: #FFD700; -fx-border-width: 2px; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 10px;"
                    : "-fx-background-color: #242426; -fx-border-color: #3a3a3c; -fx-border-width: 1px; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 10px;";
            playerBox.setStyle(borderStyle);

            Label playerLabel = new Label("Hráč " + p.getId());
            playerLabel.setStyle("-fx-text-fill: " + (isCurrent ? "#FFD700" : "#FFFFFF") + "; -fx-font-weight: bold; -fx-font-size: 13px;");

            HBox pawnsBox = new HBox(6);
            pawnsBox.setAlignment(Pos.CENTER_LEFT);

            for (Pawn pawn : p.getPawns()) {
                ImageView pawnIcon = new ImageView();
                pawnIcon.setFitWidth(28);
                pawnIcon.setFitHeight(28);
                pawnIcon.setPreserveRatio(true);

                InputStream is = getClass().getResourceAsStream("/cz/pobuda/paneckuzlobse/images/" + p.getHeroClass().getImageFileName());
                if (is != null) {
                    pawnIcon.setImage(new Image(is));
                }

                if (pawn.isInHouse()) {
                    pawnIcon.setOpacity(0.4);
                } else {
                    pawnIcon.setOpacity(1.0);
                }

                pawnIcon.setCursor(Cursor.HAND);
                Pawn currentPawn = pawn;
                pawnIcon.setOnMouseClicked(e -> handlePawnClick(currentPawn));

                pawnsBox.getChildren().add(pawnIcon);
            }

            playerBox.getChildren().addAll(playerLabel, pawnsBox);
            playersContainer.getChildren().add(playerBox);
        }
    }

    private void zobrazitVitezneOkno(Player vitez) {
        if (statusLabel != null) {
            statusLabel.setText("Hráč " + vitez.getId() + " (" + getHeroCzechName(vitez.getHeroClass()) + ") vyhrál hru!");
        }
        if (winnerTitleLabel != null) {
            winnerTitleLabel.setText("Hráč " + vitez.getId() + " (" + getHeroCzechName(vitez.getHeroClass()) + ") vyhrává!");
        }
        if (winnerStatsLabel != null) {
            winnerStatsLabel.setText("Celkový počet kol: " + roundNumber);
        }
        if (winnerHeroCardView != null) {
            InputStream is = getClass().getResourceAsStream("/cz/pobuda/paneckuzlobse/images/" + getHeroCardFileName(vitez.getHeroClass()));
            if (is != null) {
                winnerHeroCardView.setImage(new Image(is));
            }
        }
        if (victoryOverlay != null) {
            victoryOverlay.setManaged(true);
            victoryOverlay.setVisible(true);
        }
    }

    @FXML private void hratZnovu() { App.zmenScenu("/cz/pobuda/paneckuzlobse/fxml/Game.fxml"); }

    @FXML private void zpetDoMenu() { App.zmenScenu("/cz/pobuda/paneckuzlobse/fxml/Menu.fxml"); }
}