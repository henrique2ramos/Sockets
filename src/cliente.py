import socket
import threading
import sys

HOST = '127.0.0.1'
PORTA = 12345

def thread_leitura(sock):
    """Thread dedicada a escutar o servidor continuamente."""
    try:
        while True:
            dados = sock.recv(4096)
            if not dados:
                break
            mensagem = dados.decode('utf-8', errors='ignore').strip()
            print(f"\n[Servidor]: {mensagem}")
            if mensagem == "Conexao encerrada.":
                break
    except Exception:
        pass

def main():
    print(f"Conectando ao servidor Python em {HOST}:{PORTA}...")
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        sock.connect((HOST, PORTA))
    except Exception as e:
        print(f"Erro ao conectar ao servidor: {e}")
        return

    print("Conectado! Digite o comando (1, 2, 3, 4 para imagem, 0 para sair):")

    # Inicia thread de escuta
    t = threading.Thread(target=thread_leitura, args=(sock,), daemon=True)
    t.start()

    try:
        while True:
            comando = input()
            if comando == "4":
                # PNG mínimo de 1x1 pixel em Base64 para teste
                img_b64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
                msg = f"4;{img_b64}\n"
            else:
                msg = f"{comando}\n"

            sock.sendall(msg.encode('utf-8'))

            if comando == "0":
                break
    except KeyboardInterrupt:
        pass
    finally:
        sock.close()

if __name__ == '__main__':
    main()