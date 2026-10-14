import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class Servidor {
    private static final int PORTA = 12345;
    private static final int MAX_CONEXOES = 1000;
    private static final int BACKLOG = 1000;

    private static final Semaphore controleAcesso = new Semaphore(MAX_CONEXOES, true);
    private static final ConcurrentHashMap<String, ConexaoCliente> conexoesAtivas =
            new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Iniciando Servidor na porta " + PORTA + " com backlog de " + BACKLOG + "...");

        try (ServerSocket serverSocket = new ServerSocket(PORTA, BACKLOG)) {
            long proximoId = 1;

            while (true) {
                Socket socket = serverSocket.accept();
                if (!controleAcesso.tryAcquire()) {
                    rejeitarConexao(socket);
                    continue;
                }

                String clienteId = "CLI-" + proximoId++;
                ConexaoCliente conexao = new ConexaoCliente(socket, clienteId);
                conexoesAtivas.put(clienteId, conexao);
                conexao.iniciar();

                System.out.println(
                        "Cliente conectado: " + clienteId + " | Total ativos: " + conexoesAtivas.size());
            }
        } catch (IOException e) {
            System.err.println("Erro no Servidor: " + e.getMessage());
        }
    }

    private static void rejeitarConexao(Socket socket) {
        try (socket;
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            out.println("Servidor cheio. Tente novamente mais tarde.");
        } catch (IOException e) {
            System.err.println("Nao foi possivel rejeitar a conexao: " + e.getMessage());
        }
    }

    public static void removerCliente(String id) {
        if (conexoesAtivas.remove(id) == null) {
            return;
        }

        controleAcesso.release();
        System.out.println("Cliente desconectado: " + id + " | Total ativos: " + conexoesAtivas.size());
    }
}
