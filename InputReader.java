import java.io.InputStream;

class InputReader {

    InputStream in;
    boolean stream_end = false;
    private byte[] request_byte = new byte[128];
    private String request_string;
    private int request_len;
    boolean isClient;

    InputReader(InputStream in, boolean isClient) {
        this.in = in;
        this.isClient = isClient;
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
        if (!request_string.endsWith("\r\n\r\n")) {
            System.out.println("I'm here");
            return "".split("");
        }

        // remove special characters for server
        if (!isClient) request_string = request_string.replaceAll(
            "(\\r\\n)+$",
            ""
        );

        return request_string.split(" ");
    }
}
