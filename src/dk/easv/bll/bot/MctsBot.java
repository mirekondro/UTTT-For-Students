package dk.easv.bll.bot;

import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MctsBot implements IBot {

    private static final String BOT_NAME = "MCTS Bot (v0.2)";
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

        if (availableMoves.isEmpty()) {
            return null;
        }

        int randomIndex = random.nextInt(availableMoves.size());
        return availableMoves.get(randomIndex);
    }

    @Override
    public String getBotName() {
        return BOT_NAME;
    }
}