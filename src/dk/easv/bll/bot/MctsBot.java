package dk.easv.bll.bot;

import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;

import java.util.List;
import java.util.Random;

public class MctsBot implements IBot {

    private static final String BOT_NAME = "MCTS Bot (v0.1)";
    private final Random random = new Random();

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