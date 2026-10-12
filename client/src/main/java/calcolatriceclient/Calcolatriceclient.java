   package calcolatriceclient;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

/**
 * Client TCP della calcolatrice.
 * Chiede all'utente due numeri e un operatore, li invia al server
 * come stringa "numero operatore numero" e stampa la risposta.
 */
public class Calcolatriceclient {
    // Indirizzo e porta del server (devono coincidere con quelli del server)
    private static final String HOST = "localhost";
    private static final int PORTA = 12345;

    // Tempo massimo (in ms) per connettersi e per attendere la risposta
    private static final int TIMEOUT = 5_000;

    public static void main(String[] args) {

        // try-with-resources: lo Scanner viene chiuso automaticamente
        try (Scanner scanner = new Scanner(System.in)) {

            // --- Lettura dei dati da tastiera ---
            System.out.print("Primo numero: ");
            String primo = scanner.nextLine().trim();

            System.out.print("Operatore (+, -, *, /): ");
            String operatore = scanner.nextLine().trim();

            System.out.print("Secondo numero: ");
            String secondo = scanner.nextLine().trim();

            // Messaggio da inviare, ad esempio "10 + 5"
            String richiesta = primo + " " + operatore + " " + secondo;

            // --- Connessione al server ---
            // Il Socket viene creato "vuoto" e poi collegato con un timeout,
            // così il client non resta bloccato se il server non risponde.
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(HOST, PORTA), TIMEOUT);
                socket.setSoTimeout(TIMEOUT); // timeout anche sulla lettura della risposta

                // Flussi per scrivere e leggere (si chiudono insieme al socket)
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true); // true = autoflush
                BufferedReader input = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                // Invia la richiesta (println aggiunge il "a capo" che
                // il server aspetta con readLine())
                output.println(richiesta);

                // Attende la risposta del server (una riga)
                String risposta = input.readLine();

                if (risposta == null) {
                    // Il server ha chiuso la connessione senza rispondere
                    System.err.println("Il server ha chiuso la connessione senza rispondere.");
                } else {
                    System.out.println("Risposta del server: " + risposta);
                }
            }
            // La connessione si chiude automaticamente all'uscita dal try
        } catch (IOException e) {
            // Server spento, porta sbagliata, timeout, ecc.
            System.err.println("Errore di connessione: " + e.getMessage());
        }
    }
}