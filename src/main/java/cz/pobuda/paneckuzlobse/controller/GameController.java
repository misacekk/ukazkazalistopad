package cz.pobuda.paneckuzlobse.controller;

import cz.pobuda.paneckuzlobse.App;
import cz.pobuda.paneckuzlobse.logic.BoardPath;
import cz.pobuda.paneckuzlobse.model.HeroClass;
import cz.pobuda.paneckuzlobse.model.Item;
import cz.pobuda.paneckuzlobse.model.Pawn;
import cz.pobuda.paneckuzlobse.model.Player;
import cz.pobuda.paneckuzlobse.model.TilePosition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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
    @FXML private VBox itemSelectionOverlay;
    @FXML private HBox itemCardsContainer;
    @FXML private VBox bottomBar;
    @FXML private Label statusLabel;
    @FXML private Label roundLabel;

    @FXML private ImageView activeHeroCardView;
    @FXML private Label activeHeroAbilityStatus;
    @FXML private ScrollPane logScrollPane;
    @FXML private VBox gameLogBox;

    @FXML private ImageView itemSlot1, itemSlot2, itemSlot3;

    private Pane tokenLayer;
    private ImageView diceImageView;
    private AudioClip diceRollSound;

    private int targetPlayerCount = 4;
    private final List<Player> players = new ArrayList<>();
    private final Map<Pawn, ImageView> pawnTokenMap = new HashMap<>();
    private int currentPlayerIndex = 0;
    private int roundNumber = 1;

    private boolean waitingForPawnSelection = false;
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
        if (itemSelectionOverlay != null) itemSelectionOverlay.setVisible(false);

        loadSoundEffects();
        setupBoardUI();
    }

    private void loadSoundEffects() {
        try {
            URL soundUrl = getClass().getResource("/cz/pobuda/paneckuzlobse/sounds/dicerollsoundeffect.mp3");
            if (soundUrl != null) {
                diceRollSound = new AudioClip(soundUrl.toExternalForm());
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
        movablePawnsCurrentTurn.clear();
        canSpawnCurrentTurn = false;

        if (roundLabel != null) {
            roundLabel.setText(String.valueOf(roundNumber));
        }

        updateActiveHeroCard();
        updateInventoryUI();
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

            if (p.isSkipped()) {
                p.setSkipped(false);
                addLog("Hráč " + p.getId() + " vynechává kolo.");
                if (statusLabel != null) {
                    statusLabel.setText("Hráč " + p.getId() + " vynechává kolo!");
                }
            } else if (!p.isFinished()) {
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
        if (p.hasRolledThisTurn()) {
            if (statusLabel != null) statusLabel.setText("V tomto tahu už jsi házel nebo použil předmět!");
            return;
        }

        p.setHasRolledThisTurn(true);
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
            movablePawnsCurrentTurn = p.getPawnsOnBoard();

            if (!canSpawnCurrentTurn && movablePawnsCurrentTurn.isEmpty()) {
                if (statusLabel != null) {
                    statusLabel.setText("Hráč " + p.getId() + " hodil " + diceValue + ", ale nemá žádný možný tah.");
                }
                nextTurn();
                return;
            }

            if (canSpawnCurrentTurn && movablePawnsCurrentTurn.isEmpty()) {
                executeSpawn(p.getPawnInHouse(), diceValue);
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

    private void handlePawnClick(Pawn clickedPawn) {
        if (!waitingForPawnSelection) return;

        Player p = getCurrentPlayer();
        if (clickedPawn.getOwner() != p) return;

        if (clickedPawn.isInHouse()) {
            if (canSpawnCurrentTurn) {
                waitingForPawnSelection = false;
                executeSpawn(clickedPawn, lastRolledDiceValue);
            }
        } else if (!clickedPawn.isInFinish()) {
            if (movablePawnsCurrentTurn.contains(clickedPawn)) {
                waitingForPawnSelection = false;
                executePawnMove(clickedPawn, lastRolledDiceValue);
            }
        }
    }

    private void executeSpawn(Pawn pawn, int diceValue) {
        Player p = pawn.getOwner();
        pawn.setInHouse(false);
        pawn.stepToTile(p.getStartTileIndex());
        pawn.setTotalStepsMoved(0);
        updatePawnPosition(pawn);
        addLog("Hráč " + p.getId() + " nasadil novou figurku.");
        checkKickout(pawn);
        updatePlayersPanel();

        if (statusLabel != null) {
            statusLabel.setText("Hráč " + p.getId() + " nasadil novou figurku! Hází znovu.");
        }
        p.setHasRolledThisTurn(false);
    }

    private void executePawnMove(Pawn pawn, int diceValue) {
        Player p = pawn.getOwner();

        if (pawn.isAtEntranceTile()) {
            if (diceValue == 1) {
                pawn.setInFinish(true);
                updatePawnPosition(pawn);
                updatePlayersPanel();
                addLog("Hráč " + p.getId() + " dostal figurku do domečku! (" + p.getScore() + "/4)");

                if (p.isFinished()) {
                    zobrazitVitezneOkno(p);
                    return;
                }
            }
            nextTurn();
            return;
        }

        int steps = diceValue;
        if (p.isDoubleRollActive()) {
            steps *= 2;
            p.setDoubleRollActive(false);
            updateActiveHeroCard();
        }

        int stepsToEntrance = pawn.getStepsToEntrance();
        int actualSteps = Math.min(steps, stepsToEntrance);

        animatePawnMovement(pawn, actualSteps, () -> {
            checkKickout(pawn);
            checkItemPickUp(pawn);
            updatePlayersPanel();

            if (diceValue == 6) {
                p.setHasRolledThisTurn(false);
                if (statusLabel != null && !BoardPath.isItemTile(pawn.getCurrentTileIndex())) {
                    statusLabel.setText("Hodil jsi 6! Házíš znovu.");
                }
            } else {
                nextTurn();
            }
        });
    }

    private void checkItemPickUp(Pawn pawn) {
        if (pawn.isInHouse() || pawn.isInFinish()) return;

        if (BoardPath.isItemTile(pawn.getCurrentTileIndex())) {
            Player owner = pawn.getOwner();
            if (owner.getInventory().size() < 3) {
                Item reward = Item.getRandomItem();
                owner.addItem(reward);
                updateInventoryUI();
                updatePlayersPanel();
                addLog("Hráč " + owner.getId() + " získal z truhly: " + reward.getName() + ".");
                if (statusLabel != null) {
                    statusLabel.setText("Hráč " + owner.getId() + " otevřel truhlu a získal: " + reward.getName() + "!");
                }
            } else {
                if (statusLabel != null) {
                    statusLabel.setText("Hráč " + owner.getId() + " stoupl na truhlu, ale má plný inventář (3/3)!");
                }
            }
        }
    }

    private void checkKickout(Pawn attackerPawn) {
        if (attackerPawn.isInHouse() || attackerPawn.isInFinish()) return;

        for (Player defender : players) {
            if (defender == attackerPawn.getOwner()) continue;

            for (Pawn defenderPawn : defender.getPawns()) {
                if (!defenderPawn.isInHouse() && !defenderPawn.isInFinish()) {
                    if (defenderPawn.getCurrentTileIndex() == attackerPawn.getCurrentTileIndex()) {
                        if (defender.getHeroClass() == HeroClass.RYTIR) {
                            addLog("Rytíř (Hráč " + defender.getId() + ") zablokoval vyhození štítem!");
                            continue;
                        }

                        if (defender.getHeroClass() == HeroClass.NEKROMANT) {
                            defenderPawn.setInHouse(false);
                            defenderPawn.stepToTile(defender.getStartTileIndex());
                            defenderPawn.setTotalStepsMoved(0);
                            updatePawnPosition(defenderPawn);
                            addLog("Nekromant (Hráč " + defender.getId() + ") byl vyhozen, ale ožil na startu!");
                        } else {
                            defenderPawn.resetToSpawn();
                            updatePawnPosition(defenderPawn);
                            addLog("Hráč " + attackerPawn.getOwner().getId() + " vyhodil figurku Hráče " + defender.getId() + "!");
                        }
                    }
                }
            }
        }
    }

    @FXML
    private void pouzitSchopnost() {
        Player p = getCurrentPlayer();
        if (p.hasUsedAbility()) return;

        switch (p.getHeroClass()) {
            case CARODEJNICE:
                p.setDoubleRollActive(true);
                p.setAbilityUsed(true);
                addLog("Čarodějnice (Hráč " + p.getId() + ") aktivovala Dvojitý hod!");
                if (statusLabel != null) statusLabel.setText("Čarodějnice aktivovala Dvojitý hod!");
                break;
            case BANDITA:
                int nextIdx = (currentPlayerIndex + 1) % players.size();
                Player victim = players.get(nextIdx);
                if (victim.getHeroClass() != HeroClass.KRALOVNA) {
                    victim.setSkipped(true);
                    p.setHasRolledThisTurn(false);
                    p.setAbilityUsed(true);
                    addLog("Bandita (Hráč " + p.getId() + ") ukradl tah Hráči " + victim.getId() + "!");
                    if (statusLabel != null) statusLabel.setText("Bandita ukradl tah Hráči " + victim.getId() + "!");
                } else if (statusLabel != null) {
                    statusLabel.setText("Královna je imunní vůči schopnosti Bandity!");
                }
                break;
            case ELFKA:
                Pawn targetEnemyPawn = findTargetForElfka(p);
                if (targetEnemyPawn != null && targetEnemyPawn.getOwner().getHeroClass() != HeroClass.KRALOVNA) {
                    int newTile = (targetEnemyPawn.getCurrentTileIndex() - 3 + BoardPath.getPathLength()) % BoardPath.getPathLength();
                    targetEnemyPawn.stepToTile(newTile);
                    targetEnemyPawn.setTotalStepsMoved(Math.max(0, targetEnemyPawn.getTotalStepsMoved() - 3));
                    updatePawnPosition(targetEnemyPawn);
                    p.setAbilityUsed(true);
                    addLog("Elfka (Hráč " + p.getId() + ") odrazila figurku Hráče " + targetEnemyPawn.getOwner().getId() + " o 3 pole zpět!");
                    if (statusLabel != null) {
                        statusLabel.setText("Elfka odrazila figurku Hráče " + targetEnemyPawn.getOwner().getId() + " o 3 pole zpět!");
                    }
                } else if (statusLabel != null) {
                    statusLabel.setText("V dosahu 1–3 polí za tvou figurkou není žádný platný soupeř!");
                }
                break;
            default:
                break;
        }
        updateActiveHeroCard();
    }

    private Pawn findTargetForElfka(Player elfkaPlayer) {
        for (Pawn myPawn : elfkaPlayer.getPawnsOnBoard()) {
            for (Player other : players) {
                if (other == elfkaPlayer) continue;
                for (Pawn enemyPawn : other.getPawnsOnBoard()) {
                    int distBehind = (myPawn.getCurrentTileIndex() - enemyPawn.getCurrentTileIndex() + BoardPath.getPathLength()) % BoardPath.getPathLength();
                    if (distBehind >= 1 && distBehind <= 3) {
                        return enemyPawn;
                    }
                }
            }
        }
        return null;
    }

    private void animatePawnMovement(Pawn pawn, int steps, Runnable onComplete) {
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
            TilePosition finishPos = BoardPath.getFinishPosition(pawn.getOwner().getSlotIndex(), pawn.getId());
            token.setLayoutX(finishPos.getX() - offset);
            token.setLayoutY(finishPos.getY() - offset);
        } else {
            TilePosition tile = BoardPath.getTile(pawn.getCurrentTileIndex());
            token.setLayoutX(tile.getX() - offset);
            token.setLayoutY(tile.getY() - offset);
        }
    }

    @FXML
    private void pouzitPredmet() {
        if (itemSelectionOverlay != null) {
            populateItemSelectionOverlay();
            itemSelectionOverlay.setVisible(true);
        }
    }

    @FXML private void zavritVyberPredmetu() { if (itemSelectionOverlay != null) itemSelectionOverlay.setVisible(false); }

    private void populateItemSelectionOverlay() {
        if (itemCardsContainer == null) return;
        itemCardsContainer.getChildren().clear();

        Player p = getCurrentPlayer();
        List<Item> inventory = p.getInventory();

        if (inventory.isEmpty()) return;

        for (Item item : inventory) {
            VBox card = new VBox(8);
            card.setAlignment(Pos.CENTER);
            card.setPrefSize(200, 240);
            card.setStyle("-fx-background-color: #2c2c2e; -fx-border-color: #FFD700; -fx-border-radius: 8px; -fx-padding: 10px;");

            Label nameLabel = new Label(item.getName());
            nameLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold; -fx-font-size: 14px;");

            Label descLabel = new Label(item.getDescription());
            descLabel.setStyle("-fx-text-fill: #CCCCCC; -fx-font-size: 11px;");
            descLabel.setWrapText(true);

            Button useBtn = new Button("Použít");
            useBtn.setOnAction(e -> {
                boolean success = applyItemEffect(item);
                if (success) {
                    p.getInventory().remove(item);
                    updateInventoryUI();
                    updatePlayersPanel();
                    zavritVyberPredmetu();
                }
            });

            card.getChildren().addAll(nameLabel, descLabel, useBtn);
            itemCardsContainer.getChildren().add(card);
        }
    }

    private boolean applyItemEffect(Item item) {
        Player p = getCurrentPlayer();

        switch (item) {
            case TELEPORT:
                List<Pawn> myPawns = p.getPawnsOnBoard();
                if (myPawns.isEmpty()) {
                    if (statusLabel != null) statusLabel.setText("Nemáš žádnou figurku na desce pro teleportaci!");
                    return false;
                }

                Pawn myPawn = myPawns.get(0);
                Pawn closestEnemyPawn = null;
                int minDistance = Integer.MAX_VALUE;

                for (Player enemy : players) {
                    if (enemy == p) continue;
                    for (Pawn enemyPawn : enemy.getPawnsOnBoard()) {
                        int dist = Math.abs(enemyPawn.getCurrentTileIndex() - myPawn.getCurrentTileIndex());
                        if (dist < minDistance) {
                            minDistance = dist;
                            closestEnemyPawn = enemyPawn;
                        }
                    }
                }

                if (closestEnemyPawn == null) {
                    if (statusLabel != null) statusLabel.setText("Žádný soupeř nemá figurku na hrací desce!");
                    return false;
                }

                int myTile = myPawn.getCurrentTileIndex();
                int mySteps = myPawn.getTotalStepsMoved();

                myPawn.stepToTile(closestEnemyPawn.getCurrentTileIndex());
                myPawn.setTotalStepsMoved(closestEnemyPawn.getTotalStepsMoved());

                closestEnemyPawn.stepToTile(myTile);
                closestEnemyPawn.setTotalStepsMoved(mySteps);

                updatePawnPosition(myPawn);
                updatePawnPosition(closestEnemyPawn);

                addLog("Hráč " + p.getId() + " použil Teleport na Hráče " + closestEnemyPawn.getOwner().getId() + ".");
                if (statusLabel != null) {
                    statusLabel.setText("Prohodil sis pozici s figurkou Hráče " + closestEnemyPawn.getOwner().getId() + "!");
                }
                return true;

            case STASTNA_SEDMA:
                if (p.hasRolledThisTurn()) {
                    if (statusLabel != null) statusLabel.setText("V tomto tahu už jsi házel!");
                    return false;
                }

                List<Pawn> movablePawns = p.getPawnsOnBoard();
                if (movablePawns.isEmpty()) {
                    if (statusLabel != null) statusLabel.setText("Nemáš na desce žádnou figurku pro posun o 7!");
                    return false;
                }

                p.setHasRolledThisTurn(true);
                this.lastRolledDiceValue = 7;
                addLog("Hráč " + p.getId() + " použil Šťastnou sedmu (+7 polí).");

                if (movablePawns.size() == 1) {
                    executePawnMove(movablePawns.get(0), 7);
                } else {
                    waitingForPawnSelection = true;
                    movablePawnsCurrentTurn = movablePawns;
                    if (statusLabel != null) {
                        statusLabel.setText("Šťastná sedma! Klikni na figurku, se kterou se posuneš o 7 políček.");
                    }
                }
                return true;

            case CMAJZ:
                List<Player> validVictims = new ArrayList<>();
                for (Player enemy : players) {
                    if (enemy != p && !enemy.getInventory().isEmpty()) {
                        validVictims.add(enemy);
                    }
                }

                if (validVictims.isEmpty()) {
                    if (statusLabel != null) statusLabel.setText("Žádný soupeř nemá žádné předměty v inventáři!");
                    return false;
                }

                Player victim = validVictims.get(random.nextInt(validVictims.size()));
                Item stolenItem = victim.getInventory().remove(random.nextInt(victim.getInventory().size()));
                p.addItem(stolenItem);

                addLog("Hráč " + p.getId() + " použil Čmajz a ukradl " + stolenItem.getName() + " Hráči " + victim.getId() + ".");
                if (statusLabel != null) {
                    statusLabel.setText("Ukradl jsi předmět " + stolenItem.getName() + " Hráči " + victim.getId() + "!");
                }
                return true;
        }

        return false;
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
            imageName = "/cz/pobuda/paneckuzlobse/images/boardfor4.png";
        }

        InputStream is = getClass().getResourceAsStream(imageName);
        if (is == null) {
            is = getClass().getResourceAsStream("/cz/pobuda/paneckuzlobse/images/boardfor4.png");
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

    private void updateActiveHeroCard() {
        if (activeHeroCardView == null || players.isEmpty()) return;
        Player p = getCurrentPlayer();
        HeroClass hc = p.getHeroClass();

        String cardFile = switch (hc) {
            case RYTIR -> "rytirnahled.png";
            case CARODEJNICE -> "carodejnicenahled.png";
            case BANDITA -> "banditanahled.png";
            case NEKROMANT -> "nekromantnahled.png";
            case ELFKA -> "elfkanahled.png";
            case KRALOVNA -> "kralovnanahled.png";
        };

        InputStream is = getClass().getResourceAsStream("/cz/pobuda/paneckuzlobse/images/" + cardFile);
        if (is != null) {
            activeHeroCardView.setImage(new Image(is));
        }

        if (activeHeroAbilityStatus != null) {
            boolean isPassive = (hc == HeroClass.RYTIR || hc == HeroClass.NEKROMANT || hc == HeroClass.KRALOVNA);
            if (isPassive) {
                activeHeroAbilityStatus.setText("Pasivní schopnost (stále aktivní)");
                activeHeroAbilityStatus.setStyle("-fx-text-fill: #5AC8FA; -fx-font-size: 12px; -fx-font-weight: bold;");
            } else if (p.hasUsedAbility()) {
                activeHeroAbilityStatus.setText("Schopnost: Vyčerpána v tomto tahu");
                activeHeroAbilityStatus.setStyle("-fx-text-fill: #FF453A; -fx-font-size: 12px; -fx-font-weight: bold;");
            } else {
                activeHeroAbilityStatus.setText("Schopnost: Připravena k použití");
                activeHeroAbilityStatus.setStyle("-fx-text-fill: #4CD964; -fx-font-size: 12px; -fx-font-weight: bold;");
            }
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

    private void updateInventoryUI() {
        Player p = getCurrentPlayer();
        ImageView[] slots = {itemSlot1, itemSlot2, itemSlot3};
        List<Item> inv = p.getInventory();

        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null) continue;
            if (i < inv.size()) {
                InputStream is = getClass().getResourceAsStream("/cz/pobuda/paneckuzlobse/images/" + inv.get(i).getImageFileName());
                if (is != null) slots[i].setImage(new Image(is));
            } else {
                slots[i].setImage(null);
            }
        }
    }

    private void zobrazitVitezneOkno(Player vitez) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Konec hry!");
        alert.setHeaderText("🎉 VÍTĚZSTVÍ! 🎉");
        alert.setContentText("Hráč " + vitez.getId() + " VYHRÁVÁ HRU!");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) App.zmenScenu("/cz/pobuda/paneckuzlobse/fxml/Menu.fxml");
        });
    }

    @FXML private void zpetDoMenu() { App.zmenScenu("/cz/pobuda/paneckuzlobse/fxml/Menu.fxml"); }
}