import socket
import threading
import math

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

def ler_numero(mensagem):
    while True:
        entrada = input(mensagem)
        try:
            numero = float(entrada)
            if math.isfinite(numero):
                return numero
        except ValueError:
            pass

        print("Entrada invalida. Digite um numero valido.")

def main():
    print(f"Conectando ao servidor Python em {HOST}:{PORTA}...")
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        sock.connect((HOST, PORTA))
    except Exception as e:
        print(f"Erro ao conectar ao servidor: {e}")
        return

    print("Conectado!")

    # Inicia thread de escuta
    t = threading.Thread(target=thread_leitura, args=(sock,), daemon=True)
    t.start()

    try:
        while True:
            print("Escolha a operacao (1: soma, 2: subtracao, 3: multiplicacao, 4: imagem, 0: sair):")
            comando = input()
            if comando == "4":
                # PNG mínimo de 1x1 pixel em Base64 para teste
                img_b64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
                msg = f"4;{img_b64}\n"
            elif comando in ("1", "2", "3"):
                primeiro_numero = ler_numero("Digite o primeiro numero: ")
                segundo_numero = ler_numero("Digite o segundo numero: ")
                msg = f"{comando};{primeiro_numero};{segundo_numero}\n"
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