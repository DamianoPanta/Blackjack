import java.io.*;
import java.net.*;
import java.util.Scanner;

class Client {

    public static void main(String[] argv) {
        try (
            Socket socket = new Socket("localhost", 2624);
            Scanner input = new Scanner(System.in)
        ) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            InputReader inputReader = new InputReader(in);

            System.out.println("\n Connected.\n");
            print_help();

            String conversationString;
            String[] conversation;
            int remaining_chips = 100;

            // TODO : implement a more robust way to detect when a message is finished
            // (delimiter, fixed size, header)

            while (true) {
                // --- SERVER RESPOND WITH GAME STATUS ---
                conversation = input_reader.ReadLine();
                if (input_reader.stream_end) break;

                conversation_string = String.join(" ", conversation);

                // --- CLIENT UPDATE INFO (PRINT) ---
                System.out.print(conversation_string);

                // --- CLIENT ACTIOM ---
                // treat input
                while (true) {
                    System.out.print("> ");
                    conversation_string = input
                        .nextLine()
                        .trim()
                        .replaceAll(" {2,}", "")
                        .toLowerCase();
                    conversation = conversation_string.split(" ");

                    if (!conversation[0].equals("help")) break;

                    print_help();
                }

                if (conversation[0].equals("quit")) {
                    int score = remaining_chips - 100;
                    System.out.println(
                        "You quit the game " +
                            (score > 0 ? "winning " : "losing ") +
                            Math.abs(score) +
                            " chips. See you next time !"
                    );
                    break;
                }

                conversation_string = conversation_string.toUpperCase();
                out.write((conversation_string + "\r\n\r\n").getBytes());
                out.flush();
            }
        } catch (UnknownHostException error) {
            System.err.println("Unkown host: " + error.getMessage());
        } catch (IOException error) {
            System.err.println("Network error: " + error.getMessage());
        }
    }

    static void print_help() {
        System.out.println(
            """
            \033[4mCommands:\033[0m
            bet <amount>        place a bet and start a new round
            hit                 draw an additional card
            stand               end your turn and let the dealer play
            bank                check your current bankroll
            quit                leave the table
            help                display this help message
            """
        );
    }
}
