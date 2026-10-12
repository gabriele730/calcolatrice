 package calcolatriceserver;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Server TCP della calcolatrice.
 * Riceve una riga nel formato "numero operatore numero" (es. "10 + 5"),
 * calcola il risultato e lo rimanda al client.
 * I client vengono serviti uno alla volta (versione sequenziale).
 */
public class Calcolatriceserver {
    // Porta su cui il server si mette in ascolto
    private static final int PORTA = 12345;

    // Tempo massimo (in ms) di attesa della richiesta da un client.
    // Evita che un client che si connette e non scrive nulla blocchi il server.
    private static final int TIMEOUT_CLIENT = 10_000;

    // Un operando valido: segno opzionale, cifre, parte decimale opzionale
    // (accetta sia il punto sia la virgola, es. 3.5 oppure 3,5)
    private static final String REGEX_NUMERO = "[+-]?\\d+([.,]\\d+)?";

    public static void main(String[] args) {
        // try-with-resources: il ServerSocket viene chiuso automaticamente
        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            System.out.println("Server in ascolto sulla porta " + PORTA);

            // Ciclo infinito: dopo ogni client si torna in attesa del successivo
            while (true) {
                // accept() resta bloccato finché non arriva una connessione.
                // Socket, input e output vengono chiusi automaticamente a fine blocco.
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader input = new BufferedReader(
                             new InputStreamReader(clientSocket.getInputStream()));
                     PrintWriter output = new PrintWriter(
                             clientSocket.getOutputStream(), true)) { // true = autoflush

                    System.out.println("Connesso: " + clientSocket.getInetAddress());

                    // Se il client non invia nulla entro il timeout, readLine()
                    // lancia SocketTimeoutException (sottoclasse di IOException)
                    clientSocket.setSoTimeout(TIMEOUT_CLIENT);

                    // Legge la richiesta (una riga) ed elabora la risposta
                    String richiesta = input.readLine();
                    String risposta = calcola(richiesta);

                    // Invia la risposta al client
                    output.println(risposta);

                    System.out.println("Richiesta: " + richiesta);
                    System.out.println("Risposta: " + risposta);
                } catch (IOException e) {
                    // Un errore con un client non deve fermare il server
                    System.err.println("Errore nella comunicazione con un client: "
                            + e.getMessage());
                }
            }
        } catch (IOException e) {
            // Tipico caso: porta già occupata da un altro programma
            System.err.println("Impossibile avviare il server: " + e.getMessage());
        }
    }

    /**
     * Elabora la richiesta e restituisce il risultato oppure un messaggio d'errore.
     *
     * @param richiesta stringa nel formato "numero operatore numero"
     * @return risposta da inviare al client
     */
    private static String calcola(String richiesta) {
        // readLine() restituisce null se il client chiude senza inviare nulla
        if (richiesta == null || richiesta.trim().isEmpty()) {
            return "Errore: richiesta vuota.";
        }

        // Divide la stringa sugli spazi (anche multipli): deve avere 3 parti
        String[] parti = richiesta.trim().split("\\s+");
        if (parti.length != 3) {
            return "Errore: usa il formato numero operatore numero, ad esempio 10 + 5.";
        }

        // Controlla che gli operandi siano numeri validi
        // (scarta anche valori come "NaN", "Infinity" o "1e5" accettati da parseDouble)
        if (!parti[0].matches(REGEX_NUMERO) || !parti[2].matches(REGEX_NUMERO)) {
            return "Errore: uno o entrambi gli operandi non sono numeri validi.";
        }

        // Converte in double, sostituendo l'eventuale virgola decimale con il punto
        double primo = Double.parseDouble(parti[0].replace(',', '.'));
        String operatore = parti[1];
        double secondo = Double.parseDouble(parti[2].replace(',', '.'));
        double risultato;

        // Esegue l'operazione richiesta
        switch (operatore) {
            case "+":
                risultato = primo + secondo;
                break;
            case "-":
                risultato = primo - secondo;
                break;
            case "*":
                risultato = primo * secondo;
                break;
            case "/":
                // Errore matematico: divisione per zero
                if (secondo == 0) {
                    return "Errore: divisione per zero.";
                }
                risultato = primo / secondo;
                break;
            default:
                // Operatore diverso da + - * /
                return "Errore: operatore non riconosciuto. Usa +, -, * oppure /.";
        }

        // Con numeri molto grandi il risultato può uscire dal range dei double
        if (Double.isInfinite(risultato)) {
            return "Errore: risultato troppo grande.";
        }

        return "Risultato: " + formatta(risultato);
    }

    /**
     * Mostra i risultati interi senza ".0" (15 invece di 15.0);
     * lascia invariati quelli con la virgola (es. 3.3333333333333335).
     */
    private static String formatta(double valore) {
        if (valore == Math.rint(valore) && Math.abs(valore) < 1e15) {
            return String.valueOf((long) valore);
        }
        return String.valueOf(valore);
    }
}