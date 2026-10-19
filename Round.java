import java.util.ArrayList;
import java.util.List;

enum RoundOutcome {
    BLACKJACK,
    WIN,
    PUSH,
    LOSE;

    int settle(int bet) {
        return switch (this) {
            case BLACKJACK -> bet + (bet * 3) / 2;
            case WIN -> bet * 2;
            case PUSH -> bet;
            case LOSE -> 0;
        };
    }
}

class Round {

    private DeckManager deck;
    private int bet;
    private List<String> playerCard = new ArrayList<>();
    private List<String> dealerCard = new ArrayList<>();
    private int playerTotal;
    private int dealerTotal;
    private boolean dealerCardVisible = true;

    Round(DeckManager deck, int bet) {
        this.deck = deck;
        this.bet = bet;
    }

    int getBet() {
        return bet;
    }

    boolean getDealerCardVisible() {
        return dealerCardVisible;
    }

    List<String> getPlayerCard() {
        return playerCard;
    }

    List<String> getDealerCard() {
        return dealerCard;
    }

    int getPlayerTotal() {
        return playerTotal;
    }

    int getDealerTotal() {
        return dealerTotal;
    }

    RoundOutcome start() {
        playerTotal = deck.pick_card(playerCard, 2);
        dealerTotal = deck.pick_card(dealerCard, 2);

        if (playerTotal == 21 && dealerTotal == 21) {
            return RoundOutcome.PUSH;
        } else if (playerTotal == 21) {
            return RoundOutcome.BLACKJACK;
        } else if (dealerTotal == 21) return RoundOutcome.LOSE;

        dealerCardVisible = false;
        return null;
    }

    RoundOutcome hit() {
        playerTotal = deck.pick_card(playerCard, 1);

        if (playerTotal == 21) {
            DealerPlay();
            if (playerTotal == dealerTotal) return RoundOutcome.PUSH;

            return RoundOutcome.WIN;
        } else if (playerTotal > 21 || deck.deck_index < 0) {
            dealerCardVisible = true;
            return RoundOutcome.LOSE;
        }

        return null;
    }

    RoundOutcome stand() {
        dealerCardVisible = true;
        DealerPlay();

        if (playerTotal == dealerTotal) return RoundOutcome.PUSH;
        else if (
            playerTotal > dealerTotal || dealerTotal > 21
        ) return RoundOutcome.WIN;
        else return RoundOutcome.LOSE;
    }

    void DealerPlay() {
        while (dealerTotal <= 16) dealerTotal = deck.pick_card(dealerCard, 1);
    }
}
