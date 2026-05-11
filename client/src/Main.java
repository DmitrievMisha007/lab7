import core.App;

public class Main {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 8047;
        if (args.length >= 2) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        }
        new App(host, port).start();
    }
}
