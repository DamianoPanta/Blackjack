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
        System.out.println(
            "[InputReader]: request string " +
                request_string.replace("\n", "\\n").replace("\r", "\\r")
        );

        if (!request_string.matches(".*\r\n\r\n")) {
            return "".split("");
        }

        request_string = request_string.replaceAll("(\\r\\n)+$", "");
        return request_string.split(" ");
    }
}
