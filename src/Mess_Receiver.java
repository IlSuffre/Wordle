import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;


public class Mess_Receiver implements Runnable {

    int port;
    String host;
    ArrayList<String> repository;
    boolean exit;
    
    public Mess_Receiver(int port, String host){
        this.port = port;
        this.host = host;
        //Repository -> Struttura che conterrà messaggi che arrivano sul multicast 
        this.repository = new ArrayList<String>();
        //Variabile boolleana per uscire dal while 
        this.exit = false;
    }

    public void run(){
        byte[] buf = new byte[128];
        try {
            InetAddress ia = InetAddress.getByName(host);
            DatagramPacket dp = new DatagramPacket(buf, buf.length);
            //Apertura multicast
            MulticastSocket ms = new MulticastSocket(port);
            //Unione al multicast per ricevere messaggi che arrivano ad esso 
            //utilizzo del metodo spiegato a lezione benchè deprecato 
            ms.joinGroup(ia);
            //Timeout usato per creare una via d'uscita dal while dato che se no la receive sarebbe bloccante
            ms.setSoTimeout(200);
            //Aspetta di ricevere messaggi dal server degli utenti
            //Non appena l'utente loggato sul client effettua il logout e il timer scatta o arriva un ultimo messaggio termina 
            while(exit==false){
                try{
                    ms.receive(dp);
                    //converto messaggio da bytes in stringa 
                    String mess = new String(dp.getData(), 0, dp.getLength(), "US-ASCII");
                    //aggiungo messaggio a repository 
                    repository.add(mess);
                }
                catch(SocketTimeoutException e){
                    continue;
                }
            }
            //abbandono multicast group
            ms.leaveGroup(ia);
            //chiudo multicastSocket
            ms.close();
        } 
        
        catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //Usato per cambiare la variabile della guardi del while
    //fa uscire dal while e quindi il codice continua e chiude la connessione
    public void close_connection(){ 
        this.exit = true;
    }

    //Permette di stampare la lista di tutti i messaggi raccolti fino a quando viene chiamata
    public synchronized void print_mess(){
        Iterator<String> iter = repository.iterator();
        while (iter.hasNext()) {
            System.out.println(iter.next()+"\n");
        }
    }



    
}
