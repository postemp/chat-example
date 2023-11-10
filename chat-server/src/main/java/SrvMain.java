public class SrvMain {
    public static void main(String[] args) {
        int port = 8080;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        Server server = new Server(port, new InMemoryAuthenticationProvider());
        server.start();
        System.out.println("Сервер отключается, выход");
    }
}
