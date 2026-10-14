import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProcessadorComandos {
    private static final ExecutorService POOL_IMAGENS = Executors.newFixedThreadPool(4);

    public static void processar(ConexaoCliente cliente, String comandoBruto) {
        if (comandoBruto == null || comandoBruto.trim().isEmpty()) {
            return;
        }

        String[] partes = comandoBruto.split(";", -1);
        String comando = partes[0];
        if ("0".equals(comando)) {
            encerrarCliente(cliente);
            return;
        }

        cliente.getLock().lock();
        try {
            switch (comando) {
                case "1":
                    cliente.enviarMensagem("RESPOSTA:+: Operacao 1 executada com sucesso.");
                    break;
                case "2":
                    cliente.enviarMensagem("RESPOSTA:-: Operacao 2 executada com sucesso.");
                    break;
                case "3":
                    cliente.enviarMensagem("RESPOSTA:x: Operacao 3 executada com sucesso.");
                    break;
                case "4":
                    String imagemBase64 = partes.length > 1 ? partes[1] : "";
                    POOL_IMAGENS.submit(() -> processarImagem(cliente, imagemBase64));
                    break;
                default:
                    cliente.enviarMensagem("Comando desconhecido.");
                    break;
            }
        } finally {
            cliente.getLock().unlock();
        }
    }

    private static void encerrarCliente(ConexaoCliente cliente) {
        cliente.enviarMensagem("Conexao encerrada.");
        cliente.setAtivo(false);
    }

    private static void processarImagem(ConexaoCliente cliente, String imagemBase64) {
        try {
            byte[] dados = Base64.getDecoder().decode(imagemBase64);
            BufferedImage imagemOriginal = ImageIO.read(new ByteArrayInputStream(dados));
            if (imagemOriginal == null) {
                cliente.enviarMensagem("4;ERRO: Imagem invalida.");
                return;
            }

            BufferedImage imagemReduzida =
                    new BufferedImage(30, 30, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = imagemReduzida.createGraphics();
            try {
                graphics.drawImage(imagemOriginal, 0, 0, 30, 30, null);
            } finally {
                graphics.dispose();
            }

            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            ImageIO.write(imagemReduzida, "png", saida);
            String resposta = Base64.getEncoder().encodeToString(saida.toByteArray());
            cliente.enviarMensagem("4;" + resposta);
        } catch (Exception e) {
            cliente.enviarMensagem("4;ERRO ao processar imagem: " + e.getMessage());
        }
    }
}