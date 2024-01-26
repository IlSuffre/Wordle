import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Properties;
import java.util.Scanner;


public class MainClient {
    public static int port;
    public static String host;
    public static String group;
    public static int port_2;
    public static final String configFile = "client.properties";

    public static void main (String[] args) throws FileNotFoundException, IOException{
        //Leggo parametri configurazione dal file 
        readConfig();
        //Apro connessione con server
        try (Socket socket = new Socket(host, port)){
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            boolean open = true;
            //Apro scanner per input da tastiera
            Scanner scan = new Scanner(System.in);

            while(open){
                //Stampo a schermo il primo menù
                System.out.println("------------------------------------");
                System.out.println("\nREGISTER [1] \nLOGIN [2]\nEXIT [3]\n");
                int scelta_1=0; //inizializzo se no da problemi 
                try{
                    //leggo input che sarà un numero in formato stringa e lo converto in numero 
                    scelta_1 = Integer.parseInt(scan.nextLine()); //leggo int per scegliere se fare register o fare login
                }catch(NumberFormatException e){
                    //se la stringa non è un numero lo faccio presente all'utente e ripropongo lo stesso menu
                    System.out.println("\nYou must insert a number");
                    continue;
                }
                //se l'utente ha inserito un numero lo comunico al server
                dos.writeInt(scelta_1);


                //REGISTER [1] == se il numero inserito è 1
                if (scelta_1==1){
                    System.out.println("Insert a Username:");
                    //Attende username
                    String username = scan.nextLine();
                    //lo comunica al server
                    dos.writeUTF(username);
                    System.out.println("Insert a Password:");
                    //attende password
                    String password = scan.nextLine();
                    //la comunica al server 
                    dos.writeUTF(password);
                    //Attende una risposta dal server 
                    //server controlla esistenza username e risponde
                    int user_check = dis.readInt();  
                    //in base al valore della risposta si comporta in modo diverso
                    if (user_check==0){ // server risponde che l'username è gia utilizzato
                        System.out.println("This username already exists, please login or select another username");
                        continue; //nuova iterazione del while e si riparte dal primo menu
                    }
                    else if (user_check==1){ //server risponde che la password non è valida
                        System.out.println("Invalid password because it must have at least one character");
                        continue; //nuova iterazione del while e si riparte dal primo menu
                    }
                    else if (user_check==2){ //server risponde che la registrazione è avvenuta con successo
                        System.out.println("Registration was successful");
                    }

                }


                //LOGIN [2] == se il numero inserito è 2
                else if (scelta_1==2){
                    System.out.println("\nInsert your Username");
                    //Attende username
                    String username = scan.nextLine();
                    //lo comunica al server
                    dos.writeUTF(username);
                    //server risponde con un numero che rappresenta la presenza o no dell'username tra gli utenti registrati
                    int user_check = dis.readInt();
                    if (user_check==0){ //username registrato
                        System.out.println("\nInsert your Password");
                        //Attende password 
                        String password = scan.nextLine();
                        //la comunica al server
                        dos.writeUTF(password);
                        //server risponde con un valore che rappresenta se la password è corretta o no 
                        int password_check = dis.readInt();

                        if (password_check==0){ //password corretta
                            System.out.println("\nLOGIN effettuato con successso");
                            //Faccio partire in background un thread che attende messaggi sul multicast group
                            Mess_Receiver mess_reciver = new Mess_Receiver(port_2, group);
                            Thread t = new Thread(mess_reciver);
                            t.start();

                            //inizzializzo una nuova variabile che uso nella guardia del while per uscire dal prossimo menu 
                            //dato che uso uno switch e non posso fare break del while
                            boolean logout = false;
                            //inizializzo numero tentativi che mi serve per il messaggio udp
                            int n_tentativi = 0;
                            while(logout==false){ //entro nel secondo menu
                                System.out.println("------------------------------------");
                                System.out.println("\nPLAY [1] \nSTATISTICS [2] \nSHARE [3] \nSHOW ME SHARING [4]\nLOGOUT [5]\n");
                                int scelta_2 = 0; //inizzializzo se no da problemi
                                try{ //leggo input che sarà un numero in formato stringa e lo converto in numero 
                                    scelta_2 = Integer.parseInt(scan.nextLine()); 
                                }catch(NumberFormatException e){
                                    //se la stringa non è un numero lo faccio presente all'utente e ripropongo lo stesso menu
                                    System.out.println("\nYou must insert a number");
                                    continue;
                                }
                                //comunico al server la codifica dell'azione da eseguire
                                dos.writeInt(scelta_2);
                                switch(scelta_2){ //in base all'azione scelta eseguo uno switch case
                                    case 1: //PLAY
                                        //leggo risposta server che mi comunica se posso giocare o no con questa parola 
                                        int play = dis.readInt();
                                        if (play==0){ //posso giocare e allora avvia il gioco
                                            System.out.println("\nGame Started");
                                            //Chiama metodo che consente di giocare e salva i tentativi sfruttai per indovinare la parola
                                            n_tentativi = play_wordle(scan, dis, dos); 
                                        }
                                        else if (play==1){ //non puoi giocare con questa parola 
                                            System.out.println("\nYou've already played with this word, wait for the next one");
                                        }
                                        break;

                                    case 2: //chiama metodo che stampa statisctiche di un utente ricevute dal server
                                        print_statistics(dis);
                                        break;
                                        
                                    case 3: //condido esito partita
                                        // attendo dal server che controlli se l'utente ha giocato la parola attuale 
                                        int send_mess = dis.readInt();

                                        if (send_mess==0){
                                            //mando al server numero tentativi con cui è stata indovinata l'ultima parola 
                                            dos.writeInt(n_tentativi);
                                            System.out.println("\nMessage sended");
                                        }
                                        else { //l'utente non ha giocato l'ultima parola e non può condividere il messaggio 
                                            System.out.println("\nCannot send message becasuse you don't play this word or the word is changed");
                                        }
                                        break;
                                        

                                    case 4: //stampa tutti i messaggi ricevuti trmite udp
                                        //poichè i messaggi sono presenti nel client fa tutto da solo senza interrogare il server
                                        mess_reciver.print_mess();
                                        break;

                                    case 5: //logout 
                                        //setto variabile che controlla guardia while a true in modo da tornare al primo while quindi al primo menu
                                        logout=true;
                                        //chiudo task in attesa di messaggi udp
                                        mess_reciver.close_connection();
                                        break;

                                    default: //il numero inserito non è valido e non corrisponde ad un'azione 
                                        System.out.println("\nNo valid selection");
                                        break;

                                }

                            }

                        }
                        else if (password_check==1){ //server risponde che la password è sbagliata per quell'username
                            System.out.println("\nPassword is wrong try again");
                            continue; 
                        }

                    }
                    else if (user_check==1){ //server risponde che l'username non esiste tra gli utenti registrati
                        System.out.println("\nUsername is wrong try again or register");
                        continue;
                    }

                    
                }
                //EXIT [3] se numero inserito è 3 
                else if(scelta_1==3){break;}

                //Se il numero inserito in scleta_1 è diverso da 1,2 o 3
                else {
                    System.out.println("\nNo valid selection");
                    continue;
                }

            }
            //arrivo qui solo se faccio EXIT
            //chiudo lo scanner e termino
            scan.close(); 
        }catch(IOException e){
            //se client non riesce a connettersi al server o questo smette di ripsondere termina il processo
            System.out.println("\nError creation socket or Server is crashed !!!\n");
            System.exit(1);
        }
    }

    public static int play_wordle(Scanner scan, DataInputStream dis, DataOutputStream dos) throws IOException{
        boolean indovinato = false; // serve per uscire dal while se si indovian la parola con meno di 12 tentativi
        int tentativi = 1; //tengo traccia del numero di tentativi per le stampe e per il while
        while(indovinato==false && tentativi<=12){
            if (tentativi==12)System.out.println("You have one LAST attempt left");
            else {System.out.println("You have "+(13-tentativi)+" attempts left");}
            //aspetto che l'utente scriva una parola
            String tentativo = scan.nextLine();
            //la comunico al server
            dos.writeUTF(tentativo);
            //attendo risposta server che fa controlli sulla validità della parola
            int response = dis.readInt();
            switch (response) {
                case 0: //la parola è valida
                    //server manda l'indizio con la codifica +?X delle lettere
                    String indizzio = dis.readUTF();
                    //controllo se l'indizio corrisponde a solo + la parola è stata indovinata
                    if (indizzio.equals("++++++++++")){
                        indovinato = true;
                        System.out.println(indizzio);
                        System.out.println("You guessed with "+tentativi+" attempts");
                    }
                    else {System.out.println(indizzio);}
                    break;
                case 1: //la parola inserita è troppo corta o troppo lunga
                    System.out.println("Incorrect word length");
                    continue;
                    
                case 2: //la parola inserita non esiste nel dizionario
                    System.out.println("Word not found in the dictionary");
                    continue;
            }
            tentativi++;
        }
        //una volta indovinata o meno la parola ritorno il numero di tentativi consumati per il messaggio udp
        if (tentativi==13 && indovinato==false) {
            System.out.println("You didn't guess the word");
            return 0; //la partita è stata persa e restituisco 0 tentativi effetuati per distinguere
        } 
        return tentativi-1; //partita vinta 
    }


    // Stampa le statistiche di un untente ricevute dal server 
    public static void print_statistics(DataInputStream dis) throws IOException {
        int games_played = dis.readInt();
        System.out.println("Games played : "+games_played);
        int games_won = dis.readInt();
        System.out.println("Games won : "+games_won);
        int victory_rate = dis.readInt();
        System.out.println("Victory rate : "+victory_rate);
        int victory_streak = dis.readInt();
        System.out.println("Victory streak : "+victory_streak);
        int max_victory_streak = dis.readInt();
        System.out.println("Record victory streak : "+max_victory_streak);
        System.out.println("Guess Distribution :");
        for (int i=1;i<=12;i++){
            int words = dis.readInt();
            System.out.print("Words guessed in "+i+" attempts -> "+words+"\n");
        }

    }

    public static void readConfig() throws FileNotFoundException, IOException {
		InputStream input = new FileInputStream(configFile);
        Properties prop = new Properties();
        prop.load(input);
        port = Integer.parseInt(prop.getProperty("port"));
        host = prop.getProperty("host");
        group = prop.getProperty("group");
        port_2 = Integer.parseInt(prop.getProperty("port_2"));
        input.close();
	}

}