import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class ConexaoCliente {
    private final Socket socket;
    private final String id;
    private final BlockingQueue<String> filaEscrita = new LinkedBlockingQueue<>();
    private final ReentrantLock lock = new ReentrantLock();
    private volatile boolean ativo = true;
    private boolean encerrada;

    public ConexaoCliente(Socket socket, String id) {
        this.socket = socket;
        this.id = id;

        try {
            socket.setSoTimeout(120_000);
        } catch (IOException e) {
            System.err.println("Nao foi possivel configurar o timeout de " + id + ": " + e.getMessage());
        }
    }

    public void enviarMensagem(String mensagem) {
        filaEscrita.offer(mensagem);
    }

    public void iniciar() {
        Thread.ofVirtual().start(this::lerMensagens);
        Thread.ofVirtual().start(this::enviarMensagens);
    }

    private void lerMensagens() {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String linha;
            while (ativo && (linha = reader.readLine()) != null) {
                ProcessadorComandos.processar(this, linha);
            }
        } catch (IOException e) {
            if (ativo) {
                System.err.println("Erro ao ler dados de " + id + ": " + e.getMessage());
            }
        } finally {
            fechar();
        }
    }

    private void enviarMensagens() {
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            while (ativo || !filaEscrita.isEmpty()) {
                String mensagem = filaEscrita.poll(500, TimeUnit.MILLISECONDS);
                if (mensagem == null) {
                    continue;
                }

                lock.lock();
                try {
                    writer.println(mensagem);
                    writer.flush();
                } finally {
                    lock.unlock();
                }

                if ("Conexao encerrada.".equals(mensagem)) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            if (ativo) {
                System.err.println("Erro ao enviar dados para " + id + ": " + e.getMessage());
            }
        } finally {
            fecharSocket();
        }
    }

    public synchronized void fechar() {
        if (encerrada) {
            return;
        }

        encerrada = true;
        ativo = false;
        Servidor.removerCliente(id);
        fecharSocket();
    }

    private void fecharSocket() {
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Erro ao fechar a conexao de " + id + ": " + e.getMessage());
        }
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
