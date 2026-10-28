import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Scanner;

public class Cliente {
    private static final String HOST = "127.0.0.1";
    private static final int PORTA = 12345;

    public static void main(String[] args) {
        System.out.println("Conectando ao servidor em " + HOST + ":" + PORTA + "...");

        try (Socket socket = new Socket(HOST, PORTA)) {
            System.out.println("Conectado!");

            // Inicia uma thread virtual para ouvir as mensagens do servidor
            Thread tarefaLeitura = Thread.ofVirtual().start(() -> ouvirServidor(socket));

            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

            while (true) {
                System.out.println("Escolha a operacao (1: soma, 2: subtracao, 3: multiplicacao, 4: imagem, 0: sair):");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String comando = scanner.nextLine();

                if ("4".equals(comando)) {
                    String imgB64 = gerarBase64ImagemPequena();
                    writer.println("4;" + imgB64);
                } else if ("1".equals(comando) || "2".equals(comando) || "3".equals(comando)) {
                    Double primeiroNumero = lerNumero(scanner, "Digite o primeiro numero: ");
                    if (primeiroNumero == null) {
                        break;
                    }

                    Double segundoNumero = lerNumero(scanner, "Digite o segundo numero: ");
                    if (segundoNumero == null) {
                        break;
                    }

                    writer.println(comando + ";" + primeiroNumero + ";" + segundoNumero);
                } else {
                    writer.println(comando);
                }

                if ("0".equals(comando)) {
                    System.out.println("Encerrando cliente...");
                    tarefaLeitura.join();
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Erro na conexao do cliente: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Cliente interrompido.");
        }
    }

    private static Double lerNumero(Scanner scanner, String mensagem) {
        while (true) {
            System.out.print(mensagem);
            if (!scanner.hasNextLine()) {
                return null;
            }

            String entrada = scanner.nextLine();
            try {
                double numero = Double.parseDouble(entrada);
                if (Double.isFinite(numero)) {
                    return numero;
                }
            } catch (NumberFormatException ignored) {
                // Solicita novamente uma entrada numerica valida.
            }

            System.out.println("Entrada invalida. Digite um numero valido.");
        }
    }

    private static void ouvirServidor(Socket socket) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                System.out.println("\n[Servidor]: " + linha);
                if ("Conexao encerrada.".equals(linha)) {
                    break;
                }
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                System.err.println("Erro ao receber resposta do servidor: " + e.getMessage());
            }
        }
    }

    private static String gerarBase64ImagemPequena() {
        try {
            BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            System.err.println("Nao foi possivel preparar a imagem: " + e.getMessage());
            return "";
        }
    }
}