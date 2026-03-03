package dk.easv.bll.bot;

import dk.easv.bll.field.IField;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class MctsBot implements IBot {

    private static final String BOT_NAME = "MCTS Bot (v0.3)";
    private final Random random = new Random();

    private static class Node {
        IGameState state;
        Node parent;
        IMove move;
        List<Node> children;
        int visits;
        double wins;

        public Node(IGameState state, Node parent, IMove move) {
            this.state = state;
            this.parent = parent;
            this.move = move;
            this.children = new ArrayList<>();
            this.visits = 0;
            this.wins = 0;
        }
    }

    @Override
    public IMove doMove(IGameState state) {
        List<IMove> availableMoves = state.getField().getAvailableMoves();
        if (availableMoves.isEmpty()) return null;

        int result = simulateRandomPlayout(state);

        int randomIndex = random.nextInt(availableMoves.size());
        return availableMoves.get(randomIndex);
    }

    @Override
    public String getBotName() {
        return BOT_NAME;
    }

    private int simulateRandomPlayout(IGameState initialState) {
        GameSimulator simulator = createSimulator(initialState);
        int myPlayerId = initialState.getMoveNumber() % 2;

        while (simulator.getGameOver() == GameOverState.Active) {
            List<IMove> moves = simulator.getCurrentState().getField().getAvailableMoves();
            if (moves.isEmpty()) break;
            IMove randomMove = moves.get(random.nextInt(moves.size()));
            simulator.updateGame(randomMove);
        }

        if (simulator.getGameOver() == GameOverState.Tie) {
            return 0;
        } else if (simulator.getGameOver() == GameOverState.Win) {
            int winnerId = (simulator.currentPlayer == 0) ? 1 : 0;
            return (winnerId == myPlayerId) ? 1 : -1;
        }
        return 0;
    }

    private GameSimulator createSimulator(IGameState state) {
        GameSimulator simulator = new GameSimulator(new GameState());
        simulator.setGameOver(GameOverState.Active);
        simulator.setCurrentPlayer(state.getMoveNumber() % 2);
        simulator.getCurrentState().setRoundNumber(state.getRoundNumber());
        simulator.getCurrentState().setMoveNumber(state.getMoveNumber());
        simulator.getCurrentState().getField().setBoard(state.getField().getBoard());
        simulator.getCurrentState().getField().setMacroboard(state.getField().getMacroboard());
        return simulator;
    }

    public enum GameOverState { Active, Win, Tie }

    public class Move implements IMove {
        int x, y;
        public Move(int x, int y) { this.x = x; this.y = y; }
        @Override public int getX() { return x; }
        @Override public int getY() { return y; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Move move = (Move) o;
            return x == move.x && y == move.y;
        }
        @Override public int hashCode() { return Objects.hash(x, y); }
    }

    class GameSimulator {
        private final IGameState currentState;
        private int currentPlayer = 0;
        private volatile GameOverState gameOver = GameOverState.Active;

        public GameSimulator(IGameState currentState) { this.currentState = currentState; }
        public void setGameOver(GameOverState state) { gameOver = state; }
        public GameOverState getGameOver() { return gameOver; }
        public void setCurrentPlayer(int player) { currentPlayer = player; }
        public IGameState getCurrentState() { return currentState; }

        public Boolean updateGame(IMove move) {
            updateBoard(move);
            currentPlayer = (currentPlayer + 1) % 2;
            return true;
        }

        private void updateBoard(IMove move) {
            String[][] board = currentState.getField().getBoard();
            board[move.getX()][move.getY()] = currentPlayer + "";
            currentState.setMoveNumber(currentState.getMoveNumber() + 1);
            if (currentState.getMoveNumber() % 2 == 0) {
                currentState.setRoundNumber(currentState.getRoundNumber() + 1);
            }
            checkAndUpdateIfWin(move);
            updateMacroboard(move);
        }

        private void checkAndUpdateIfWin(IMove move) {
            String[][] macroBoard = currentState.getField().getMacroboard();
            int macroX = move.getX() / 3;
            int macroY = move.getY() / 3;

            if (macroBoard[macroX][macroY].equals(IField.EMPTY_FIELD) || macroBoard[macroX][macroY].equals(IField.AVAILABLE_FIELD)) {
                String[][] board = getCurrentState().getField().getBoard();
                if (isWin(board, move, "" + currentPlayer)) macroBoard[macroX][macroY] = currentPlayer + "";
                else if (isTie(board, move)) macroBoard[macroX][macroY] = "TIE";

                if (isWin(macroBoard, new Move(macroX, macroY), "" + currentPlayer)) gameOver = GameOverState.Win;
                else if (isTie(macroBoard, new Move(macroX, macroY))) gameOver = GameOverState.Tie;
            }
        }

        private boolean isTie(String[][] board, IMove move) {
            int localX = move.getX() % 3, localY = move.getY() % 3;
            int startX = move.getX() - localX, startY = move.getY() - localY;
            for (int i = startX; i < startX + 3; i++) {
                for (int k = startY; k < startY + 3; k++) {
                    if (board[i][k].equals(IField.AVAILABLE_FIELD) || board[i][k].equals(IField.EMPTY_FIELD)) return false;
                }
            }
            return true;
        }

        public boolean isWin(String[][] board, IMove move, String currentPlayer) {
            int localX = move.getX() % 3, localY = move.getY() % 3;
            int startX = move.getX() - localX, startY = move.getY() - localY;

            for (int i = startY; i < startY + 3; i++) {
                if (!board[move.getX()][i].equals(currentPlayer)) break;
                if (i == startY + 3 - 1) return true;
            }
            for (int i = startX; i < startX + 3; i++) {
                if (!board[i][move.getY()].equals(currentPlayer)) break;
                if (i == startX + 3 - 1) return true;
            }
            if (localX == localY) {
                int y = startY;
                for (int i = startX; i < startX + 3; i++) {
                    if (!board[i][y++].equals(currentPlayer)) break;
                    if (i == startX + 3 - 1) return true;
                }
            }
            if (localX + localY == 3 - 1) {
                int less = 0;
                for (int i = startX; i < startX + 3; i++) {
                    if (!board[i][(startY + 2) - less++].equals(currentPlayer)) break;
                    if (i == startX + 3 - 1) return true;
                }
            }
            return false;
        }

        private void updateMacroboard(IMove move) {
            String[][] macroBoard = currentState.getField().getMacroboard();
            for (int i = 0; i < macroBoard.length; i++)
                for (int k = 0; k < macroBoard[i].length; k++) {
                    if (macroBoard[i][k].equals(IField.AVAILABLE_FIELD)) macroBoard[i][k] = IField.EMPTY_FIELD;
                }
            int xTrans = move.getX() % 3, yTrans = move.getY() % 3;
            if (macroBoard[xTrans][yTrans].equals(IField.EMPTY_FIELD)) macroBoard[xTrans][yTrans] = IField.AVAILABLE_FIELD;
            else {
                for (int i = 0; i < macroBoard.length; i++)
                    for (int k = 0; k < macroBoard[i].length; k++) {
                        if (macroBoard[i][k].equals(IField.EMPTY_FIELD)) macroBoard[i][k] = IField.AVAILABLE_FIELD;
                    }
            }
        }
    }
}