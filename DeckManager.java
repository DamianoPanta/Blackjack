import java.util.Arrays;
import java.util.List;

class DeckManager {

    int deck_index;
    String[] player_deck;

    DeckManager(String[] player_deck) {
        this.player_deck = player_deck;
        deck_index = player_deck.length - 1;
    }

    int pick_card(List<String> previous_card, int card_amount) {
        deck_index -= card_amount;
        String[] new_card = Arrays.copyOfRange(
            player_deck,
            deck_index + 1,
            deck_index + card_amount + 1
        );

        for (String card : new_card) {
            previous_card.add(card);
        }

        int total = 0;
        int aces = 0;
        for (String card : previous_card) {
            try {
                String rank = card.split("")[0];
                if (
                    rank.equals("T") ||
                    rank.equals("J") ||
                    rank.equals("Q") ||
                    rank.equals("K")
                ) total += 10;
                else if (rank.equals("A")) {
                    aces++;
                    total += 11;
                } else total += Integer.parseInt(rank);
            } catch (NumberFormatException e) {
                System.err.println(
                    "Error reading card number: " + e.getMessage()
                );
            }
        }

        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }

        return total;
    }
}
