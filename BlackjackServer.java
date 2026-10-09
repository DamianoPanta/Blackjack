import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
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
                Player client_thread = new Player(client_socket, deck, INITIAL_BANKROLL);
                client_thread.start();

                System.out.println("[main] Client thread started");
            }
        } catch (IOException error) {
            System.err.print("Network error: " + error.getMessage());
        }
    }
}

class Player extends Thread {
    Socket client_socket;
    OutputStream out;
    InputStream in;
    String[] player_deck;
    int player_bankroll = 100;
    String response_to_client = new String();

    Player(Socket _s, String[] deck, int bankroll) {
        try {
            client_socket = _s;
            out = client_socket.getOutputStream();
            in = client_socket.getInputStream();
            player_deck = shuffle_deck(deck);
            player_bankroll = bankroll;
        } catch (Exception e) {
            System.err.println("Error creating player " + e.getMessage());
            client_socket.close();
        }
    }

    @Override
    public void run() {
        try {
            // Initial message
            out.write(bank_response(player_bankroll).getBytes());

            while (true) {
                int msg_len = in.read(msg);
                System.out.println("Server msg buffer after read: " + new String(msg).replace('\0', '.'));
                if (msg_len <= 0)
                    break; // received nothing
                System.out.println("Message received by server: " + new String(msg, 0, msg_len));
                out.write(("Hi client, you send me: " + new String(msg, 0, msg_len)).getBytes());
                out.flush();
            }
            client_socket.close();
            System.out.println("Client thread closed");
        } catch (Exception e) {
            System.err.println("Worker issue " + e);
        }
    }

    void send_response() {
        out.flush();
        out.write(response_to_client + "\r\n");
    }

    void add_bank_response() {
        response_to_client += "BANKROLL " + player_bankroll + "\r\n";
    }

    void add_hand_response(String[] player_card, String[] dealer_card) {
        int player_total = 0;
        int dealer_total = 0;

        for (String card : player_card) {
            player_total += Integer.parseInt(card.split("")[0]);
        }

        for (String card : dealer_card) {
            dealer_total += Integer.parseInt(card.split("")[0]);
        }
        response_to_client += "PLAYER " + String.join(" ", player_card) + " " + player_total + "\r\n";
        response_to_client += "DEALER " + String.join(" ", dealer_card) + " " + dealer_total + "\r\n";
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

class Round {
    String[] player_card;
    String[] dealer_card;
    bool dealer_card2_visible = false;

    Round(String[] current_deck, int current_bankroll, int bet) {

    }
}
