import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

class Server {

    public static void main(String[] argv) {
        try (ServerSocket server_socket = new ServerSocket(2624)) {
            final int INITIAL_BANKROLL = 100;

            // create card deck
            char[] card_rank = {
                '2',
                '3',
                '4',
                '5',
                '6',
                '7',
                '8',
                '9',
                'T',
                'J',
                'Q',
                'K',
                'A',
            };
            char[] card_suit = { 'S', 'H', 'D', 'C' };
            String[] deck = new String[52];
            Short index = 0;

            for (char rank : card_rank) {
                for (char suit : card_suit) {
                    deck[index] = new StringBuilder()
                        .append(rank)
                        .append(suit)
                        .toString();
                    index++;
                }
            }

            while (true) {
                System.out.println("[main] Server waiting for connection");

                Socket client_socket = server_socket.accept();
                Game client_thread = new Game(
                    client_socket,
                    deck,
                    INITIAL_BANKROLL
                );
                client_thread.start();

                System.out.println("[main] Client thread started");
            }
        } catch (IOException error) {
            System.err.print("Network error: " + error.getMessage());
        }
    }
}

class Game extends Thread {

    Socket client_socket;
    OutputStream out;
    InputStream in;
    String[] playerDeck;
    DeckManager deckManager;
    int playerBankroll;
    String response_to_client = new String();
    boolean in_round = false;
    boolean game_over = false;
    Round round;

    Game(Socket _s, String[] deck, int bankroll) {
        try {
            client_socket = _s;
            out = client_socket.getOutputStream();
            in = client_socket.getInputStream();
            playerDeck = shuffle_deck(deck);
            deckManager = new DeckManager(playerDeck);
            playerBankroll = bankroll;
        } catch (Exception e) {
            System.err.println("Error creating player " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            // Initial message to send initial bankroll
            add_bank_response();
            send_response();

            InputReader input_reader = new InputReader(in, false);
            String[] input_string;
            int amount;

            while (!game_over) {
                System.out.println("[Game]: wait for client request");
                input_string = input_reader.ReadLine();
                if (input_reader.stream_end) break;

                if (input_string[0].equals("BET")) {
                    if (input_string.length < 2) {
                        out.write("INVALID BET\r\n\r\n".getBytes());
                        continue;
                    }

                    try {
                        amount = Integer.parseInt(input_string[1]);
                        if (amount < 0) throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        out.write("INVALID BET\r\n\r\n".getBytes());
                        continue;
                    }

                    if (amount > playerBankroll) {
                        out.write("INSUFFICIENT FUNDS\r\n\r\n".getBytes());
                        continue;
                    }

                    if (in_round) {
                        out.write("ROUND IN PROGRESS\r\n\r\n".getBytes());
                        continue;
                    }

                    playerBankroll -= amount;
                    round = new Round(deckManager, amount);
                    in_round = true;
                    handleRoundOutcome(round, round.start());
                } else if (input_string[0].equals("HIT")) {
                    if (!in_round) {
                        out.write("NO ACTIVE ROUND\r\n\r\n".getBytes());
                        continue;
                    } else {
                        handleRoundOutcome(round, round.hit());
                    }
                } else if (input_string[0].equals("STAND")) {
                    if (!in_round) {
                        out.write("NO ACTIVE ROUND\r\n\r\n".getBytes());
                        continue;
                    } else handleRoundOutcome(round, round.stand());
                } else if (input_string[0].equals("BANK")) {
                    add_bank_response();
                } else if (input_string[0].equals("QUIT")) {
                    game_over = true;
                    continue;
                } else {
                    out.write("WRONG\r\n\r\n".getBytes());
                    continue;
                }

                send_response();
            }

            if (!client_socket.isClosed()) client_socket.close();

            System.out.println("[Game]: client_socket closed");
        } catch (Exception e) {
            System.err.println("Player issue " + e);
        }
    }

    void handleRoundOutcome(Round round, RoundOutcome outcome) {
        add_hand_response(round);
        if (outcome != null) {
            int delta = outcome.settle(round.getBet());
            playerBankroll += delta;
            in_round = false;
            add_roundstatus_response(round, outcome);

            if (deckManager.deck_index < 3 || playerBankroll <= 0) {
                game_over = true;
                add_gameover_response();
            }
        }
    }

    void send_response() {
        try {
            out.write((response_to_client + "\r\n").getBytes());
            out.flush();
            response_to_client = "";
        } catch (IOException e) {
            System.err.println("Error writing to socket: " + e.getMessage());
        }
    }

    void add_bank_response() {
        response_to_client += "BANKROLL " + playerBankroll + "\r\n";
    }

    void add_hand_response(Round round) {
        List<String> playerCard = round.getPlayerCard();
        List<String> dealerCard = round.getDealerCard();
        int playerTotal = round.getPlayerTotal();
        int dealerTotal = round.getDealerTotal();

        response_to_client +=
            "PLAYER " +
            String.join(" ", playerCard) +
            " " +
            playerTotal +
            "\r\n";
        if (!round.getDealerCardVisible()) {
            String temp = dealerCard.get(1);
            dealerCard.set(1, "??");
            response_to_client +=
                "DEALER " + String.join(" ", dealerCard) + "\r\n";
            dealerCard.set(1, temp);
        } else response_to_client +=
            "DEALER " +
            String.join(" ", dealerCard) +
            " " +
            dealerTotal +
            "\r\n";
    }

    void add_roundstatus_response(Round round, RoundOutcome outcome) {
        int bet = round.getBet();
        switch (outcome) {
            case BLACKJACK:
                response_to_client += "BLACKJACK " + bet * 1.5 + "\r\n";
                break;
            case WIN:
                response_to_client += "WIN " + bet + "\r\n";
                break;
            case LOSE:
                response_to_client += "LOSE " + bet + "\r\n";
                break;
            case PUSH:
                response_to_client += "PUSH 0\r\n";
                break;
        }
    }

    void add_gameover_response() {
        response_to_client += "GAME OVER\r\n";
        add_bank_response();
    }

    static String[] shuffle_deck(String[] deck) {
        int index;
        String temp;
        Random random = new Random();
        String[] player_deck = Arrays.copyOf(deck, deck.length); // need new object

        for (int i = player_deck.length - 1; i > 0; i--) {
            index = random.nextInt(i + 1); // nextInt(int bound) with bound exclusive, 0<=index<=i
            if (index != i) {
                temp = player_deck[i];
                player_deck[i] = player_deck[index];
                player_deck[index] = temp;
            }
        }
        return player_deck;
    }
}
