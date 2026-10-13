import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class Server {
    public static void main(String argv[]) {
        try (ServerSocket server_socket = new ServerSocket(2624)) {

            final int INITIAL_BANKROLL = 100;

            // create card deck
            char[] card_rank = { '2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K', 'A' };
            char[] card_suit = { 'S', 'H', 'D', 'C' };
            String[] deck = new String[52];
            Short index = 0;

            for (char rank : card_rank) {
                for (char suit : card_suit) {
                    deck[index] = new StringBuilder().append(rank).append(suit).toString();
                    index++;
                }
            }

            while (true) {
                System.out.println("[main] Server waiting for connection");

                Socket client_socket = server_socket.accept();
                Game client_thread = new Game(client_socket, deck, INITIAL_BANKROLL);
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
    String[] player_deck;
    DeckManager deck_manager;
    int player_bankroll;
    String response_to_client = new String();
    boolean in_round = false;
    boolean game_over = false;
    Round current_round;

    Game(Socket _s, String[] deck, int bankroll) {
        try {
            client_socket = _s;
            out = client_socket.getOutputStream();
            in = client_socket.getInputStream();
            player_deck = shuffle_deck(deck);
            deck_manager = new DeckManager(player_deck);
            player_bankroll = bankroll;
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

            InputReader input_reader = new InputReader(in);
            String[] input_string;
            int amount;

            while (!game_over) {
                System.out.println("[Game]: wait for client request");
                input_string = input_reader.ReadLine();
                if (input_reader.stream_end)
                    break;

                if (input_string[0].equals("BET")) {
                    if (input_string.length < 2) {
                        out.write("INVALID BET\r\n\r\n".getBytes());
                        continue;
                    }

                    try {
                        amount = Integer.parseInt(input_string[1]);
                        if (amount < 0)
                            throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        out.write("INVALID BET\r\n\r\n".getBytes());
                        continue;
                    }

                    if (amount > player_bankroll) {
                        out.write("INSUFFICIENT FUNDS\r\n\r\n".getBytes());
                        continue;
                    }

                    if (in_round) {
                        out.write("ROUND IN PROGRESS\r\n\r\n".getBytes());
                        continue;
                    }

                    current_round = new Round(this, amount);
                    current_round.Start();
                } else if (input_string[0].equals("HIT")) {
                    if (!in_round) {
                        out.write("NO ACTIVE ROUND\r\n\r\n".getBytes());
                        continue;
                    } else
                        current_round.Hit();
                } else if (input_string[0].equals("STAND")) {
                    if (!in_round) {
                        out.write("NO ACTIVE ROUND\r\n\r\n".getBytes());
                        continue;
                    } else
                        current_round.Stand();
                } else if (input_string[0].equals("BANK")) {
                    add_bank_response();
                } else if (input_string[0].equals("QUIT")) {
                    game_over = true;
                    client_socket.close();
                    continue;
                } else {
                    out.write("WRONG\r\n\r\n".getBytes());
                    continue;
                }

                send_response();
            }

            if (!client_socket.isClosed())
                client_socket.close();

            System.out.println("[Game]: client_socket closed");
        } catch (Exception e) {
            System.err.println("Player issue " + e);
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
        response_to_client += "BANKROLL " + player_bankroll + "\r\n";
    }

    void add_hand_response(boolean dealer_card_visible, List<String> player_card, List<String> dealer_card,
            int player_total,
            int dealer_total) {
        response_to_client += "PLAYER " + String.join(" ", player_card) + " " + player_total + "\r\n";
        if (!dealer_card_visible) {
            String temp = dealer_card.get(1);
            dealer_card.set(1, "??");
            response_to_client += "DEALER " + String.join(" ", dealer_card) + "\r\n";
            dealer_card.set(1, temp);
        } else
            response_to_client += "DEALER " + String.join(" ", dealer_card) + " " + dealer_total + "\r\n";
    }

    void add_roundstatus_response(int amount, boolean blackjack) {
        in_round = false;
        if (blackjack) {
            int gain = (int) (amount * 1.5);
            response_to_client += "BLACKJACK " + gain + "\r\n";
            player_bankroll += gain;
        } else if (amount > 0) {
            response_to_client += "WIN " + amount + "\r\n";
            player_bankroll += amount;
        } else if (amount < 0) {
            response_to_client += "LOSE " + Math.abs(amount) + "\r\n";
            player_bankroll += amount;
        } else
            response_to_client += "PUSH 0\r\n";
    }

    void add_gameover_response() {
        response_to_client += "GAME OVER\r\n";
        add_bank_response();
        in_round = false;
        game_over = true;
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

// TODO: check that string concatenation is not too bad in term of efficiency
class InputReader {
    InputStream in;
    boolean stream_end = false;
    private byte[] request_byte = new byte[64];
    private String request_string;
    private int request_len;

    InputReader(InputStream in) {
        this.in = in;
    }

    String[] ReadLine() {
        try {
            request_len = in.read(request_byte);
        } catch (Exception e) {
            System.err.println("Error reading from socket: " + e.getMessage());
        }
        if (request_len <= 0) {
            stream_end = true;
            return "".split("");
        }
        request_string = new String(request_byte, 0, request_len);
        System.out.println("[InputReader]: request string " + request_string.replace("\n", "\\n").replace("\r", "\\r"));

        if (!request_string.matches(".*\r\n\r\n")) {
            return "".split("");
        }

        request_string = request_string.replaceAll("(\\r\\n)+$", "");
        return request_string.split(" ");
    }
}

class DeckManager {
    int deck_index;
    String[] player_deck;

    DeckManager(String[] player_deck) {
        this.player_deck = player_deck;
        deck_index = player_deck.length - 1;
    }

    int pick_card(List<String> previous_card, int card_amount) {
        deck_index -= card_amount;
        String[] new_card = Arrays.copyOfRange(player_deck, deck_index + 1, deck_index + card_amount + 1);

        for (String card : new_card) {
            previous_card.add(card);
        }

        int total = 0;
        int aces = 0;
        for (String card : previous_card) {
            try {
                String rank = card.split("")[0];
                if (rank.equals("T") || rank.equals("J") || rank.equals("Q") || rank.equals("K"))
                    total += 10;
                else if (rank.equals("A")) {
                    aces++;
                    total += 11;
                } else
                    total += Integer.parseInt(rank);
            } catch (NumberFormatException e) {
                System.err.println("Error reading card number: " + e.getMessage());
            }
        }

        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }

        return total;
    }
}

class Round {
    Game game;
    int bet;
    List<String> player_card = new ArrayList<>();
    List<String> dealer_card = new ArrayList<>();
    int player_total;
    int dealer_total;
    boolean dealer_card_visible = false;

    Round(Game game, int bet) {
        this.game = game;
        this.bet = bet;
    }

    void Start() {
        game.in_round = true;
        player_total = game.deck_manager.pick_card(player_card, 2);
        dealer_total = game.deck_manager.pick_card(dealer_card, 2);

        game.add_hand_response(dealer_card_visible, player_card, dealer_card, player_total, dealer_total);
        System.out.println("in the round");

        if (player_total == 21 && dealer_total == 21) {
            game.add_roundstatus_response(0, false);
        } else if (player_total == 21) {
            game.add_roundstatus_response(bet, true);
        } else if (dealer_total == 21)
            game.add_roundstatus_response(-bet, false);

        if ((game.deck_manager.deck_index < 3 || game.player_bankroll <= 0) && !game.in_round) {
            game.add_gameover_response();
        }
    }

    void Hit() {
        player_total = game.deck_manager.pick_card(player_card, 1);

        game.add_hand_response(dealer_card_visible, player_card, dealer_card, player_total, dealer_total);

        if (player_total == 21) {
            DealerPlay();
            if (player_total == dealer_total)
                game.add_roundstatus_response(0, false);

            game.add_roundstatus_response(bet, false);

            if (game.deck_manager.deck_index < 3 || game.player_bankroll <= 0) {
                game.add_gameover_response();
            }
        } else if (player_total > 21 || game.deck_manager.deck_index < 0) {
            dealer_card_visible = true;
            game.add_roundstatus_response(-bet, false);

            if ((game.deck_manager.deck_index < 3 || game.player_bankroll <= 0) && !game.in_round) {
                game.add_gameover_response();
            }
        }
    }

    void Stand() {
        dealer_card_visible = true;
        DealerPlay();
        game.add_hand_response(dealer_card_visible, player_card, dealer_card, player_total, dealer_total);

        if (player_total == dealer_total)
            game.add_roundstatus_response(0, false);
        else if (player_total > dealer_total || dealer_total > 21)
            game.add_roundstatus_response(bet, false);
        else
            game.add_roundstatus_response(-bet, false);

        if (game.deck_manager.deck_index < 3 || game.player_bankroll <= 0) {
            game.add_gameover_response();
        }
    }

    void DealerPlay() {
        while (dealer_total <= 16)
            dealer_total = game.deck_manager.pick_card(dealer_card, 1);
    }
}
