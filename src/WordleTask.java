import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileWriter;


public class WordleTask implements Runnable {
    private Socket socket;
    private Info_Server info;
    private String host;
    private int port;
    
    public WordleTask(Socket socket,Info_Server info,String host,int port) {
        this.socket = socket;
        this.info = info;
        this.host = host;
        this.port = port;
    }

    //Codice che eseguira il Client per ogni thread che apre 
    public void run() {
        try {

            DataOutputStream dos = new DataOutputStream(this.socket.getOutputStream());
            DataInputStream dis = new DataInputStream(this.socket.getInputStream());

            while(true) { // gestisco il primo menu 
                int scelta_1 = dis.readInt();  //leggo scelta azione dal client      
                //REGISTER
                if (scelta_1==1){ //scelta di registrarsi
                    String username = dis.readUTF(); //leggo username da client
                    String password = dis.readUTF(); //leggo password da client
                    if(check_if_username_exists(username, info.users_list)){ //controllo se esiste già un utente con quell'username
                        dos.writeInt(0); //in caso positivo lo comunico al client e si rimetta in attesa di un azione dal client
                        continue;

                    }
                    else if (password.equals("")){ // controlla che la password sia diversa da ""
                        dos.writeInt(1); //comunica client che la password vuota non è valida 
                        continue;
                    }
                    else { // username e password validi quindi effettua la registrazione
                        dos.writeInt(2); //comunica al client che la registrazione è avvenuta con successo
                        User new_user = new User(username,password); //creazione nuovo utente
                        info.users_list.add(new_user); //aggiunta nuovo utente alla struttura dati 
                        backup_server_users(info.users_list); //salvataggio su file della struttura dati aggiornata
                        continue;
                    }
                }

                //LOGIN
                else if (scelta_1==2){ //richiesta di login 
                    String username = dis.readUTF(); //attende username dal client
                    if (check_if_username_exists(username, info.users_list)){ //se esiste chiede password al client
                        User log_user = search_user(username, info.users_list); //identifico user cercandolo con l'username
                        dos.writeInt(0); //comunica al client che può inviargli la password
                        String password = dis.readUTF(); //attende password dal client 
                        if (log_user.password.equals(password)){//comunica client che la password è corretta e effettua il login 
                            dos.writeInt(0); 
                            // while per operazioni con switch play-statistic-share-show_sahring-logout
                            boolean logout = false; // serve per uscire dal while (==logout utente)
                            while (logout==false){
                                int scelta_2 = dis.readInt(); //aspetto azione dal client
                                switch(scelta_2){ //in base all'azione eseguo uno switch case
                                    case 1:
                                        if (log_user.play_this_word==false){ //controlla che l'utente non abbia già giocato con questa parola 
                                            dos.writeInt(0); //comunica al client che la partita sta per iniziare 
                                            log_user.play_this_word=true; //con questo impedisce di rigiocare la stessa parola 
                                            play_wordle(info.secret_word, info.dictionary, dos, dis,log_user);// gioco la partita
                                            //salvo modifiche alle statistiche dopo la partita 
                                            backup_server_users(info.users_list);
                                        }
                                        else {
                                            dos.writeInt(1); //comunico al client che ha già giocato con questa parola e deve aspettare la successiva 
                                        }
                                        break;


                                    case 2:
                                        //mando al client tutte le statistiche dell'utente loggato
                                        print_statistics(log_user, dos);
                                        break;

                                    case 3:
                                        if (log_user.play_this_word==true){ // controlla che l'utente abbia giocato la parola attuale
                                            dos.writeInt(0); //comunico client che mando il messaggio 
                                            int n_tentativi = dis.readInt(); // aspetto numero tentativi impiegati dal client per formare il messaggio da inviare
                                            mess_sender(port, host, n_tentativi, log_user.username); //mando messaggio udp
                                        }
                                        else{
                                            dos.writeInt(1); //comunico al client che non posso inviare il messaggio perche o la parola è cambiata o ancora non ha giocato 
                                        }
                                        break;

                                    case 4: //ci pensa da solo il client ad occuparsi di questa richiesta 
                                        break;
                                        

                                    case 5:
                                        //cambio valore alla variabile in modo da uscire da while che gestisce questo menu
                                        logout=true;
                                        break;

                                    default:
                                        // azione inesistente 
                                        break;

                                }
                            }


                        } 
                        else dos.writeInt(1); //password sbagliata ma si chiede comunque di reinserire anche l'username 
                    }
                    else { //comunica al client che l'username non esiste riprova o registrati 
                        dos.writeInt(1);
                        continue;
                    }

                }
                else if(scelta_1==3){
                    //il client si è disconnesso quindi chiudo questo thread che era a lui dedicato
                    System.out.println("Client disconnected");
                    break;
                }

                else {//INPUT NON VALIDO ossia diverso da 0 e 1 
                    continue; 
                }

            }
            //chiudo i DataInputStream e DataOutputStream
            dos.close();
            dis.close();
        }catch(IOException e){e.printStackTrace();};

    }

 
    //Controlla che l'username esista nella struttura dati 
    public synchronized boolean check_if_username_exists(String username,ArrayList<User> users_list) {
            boolean exists = false;
            Iterator<User> iter = users_list.iterator();
            while(iter.hasNext()){
                User check_user = iter.next();
                if (check_user.username.equals(username)) exists = true;
            }
            return exists ;
        }
    // dopo aver verificato che username esiste estraggo l'utente dalla lista 
    public synchronized User search_user(String username,ArrayList<User> users_list){
        Iterator<User> iter = users_list.iterator();
        while(iter.hasNext()){
            User user = iter.next();
            if (user.username.equals(username)){
                return user;
            }
        }
        return null;
    }

    // PLAY WORDLE

    public static void play_wordle(String secret_word,ArrayList<String>dictionary,DataOutputStream dos, DataInputStream dis,User user) throws IOException{
        int n_tentativi = 1;
        //SETTARE SUBITO VARIABILIE DI GIOCO A TRUE COSI IN CASO ARRIVI DURANTE IL GIOCO UNA NUOVA PAROLA L'UTENTE PUO SUBITO RIGIOCARE 

        while (n_tentativi<=12){
            //mappo la parola segreta perchè la sua mappa mi serve per la creazione dell'indizio 
            HashMap<Character,Integer> map = map_secret_word(secret_word);
            String tentativo = dis.readUTF(); //legge parola inserita dall'utente, inviata dal client
            if (check_length(tentativo)){ //controllo lunghezza parola 
                //controllo se la parola appartinene al dizionario 
                if (check_exist_dictionary(tentativo,dictionary)){// se arrivo qui devo controllare parola e mandare indizi 
                    //transformo le stringhe in array di char per scorrerle 
                    char[] s_word = secret_word.toCharArray(); 
                    char[] tent = tentativo.toCharArray();
                    //creo array di string (poiche + e ? non sono char) per accumulare gli indizzi
                    String[] indizzi = new String[10];
                    for (int i=0; i<10;i++){
                        if (s_word[i]==tent[i]) { //controllo a coppie le lettere della stessa posizione della parola
                            indizzi[i] = "+"; //se sono uguali metto nella stessa posizione dell'array indizzi il +
                            //e rimuovo o decremento quella lettera dalla mappa
                            int occur = map.get(s_word[i]); 
                            if (occur == 1) map.remove(s_word[i]);
                            else {map.replace(s_word[i], occur, occur-1);}
                        }
                        else {indizzi[i] = "X";} //altrimenti metto in quella posizione dell'array indizzi la X
                    }
                    //adesso vado a verificare in base ai valori rimasti nella mappa se posso mettere delle leettere gialle quindi con ?
                    for (int i=0; i<10; i++){ //scorro nuovamente 
                        if (indizzi[i]=="X"){ //se nell'array indizzi trovo una X 
                            //controllo che la lettera in quella posizione nella parola inserita dall'utente sia presente nella mappa
                            if (map.containsKey(tent[i])){  
                                indizzi[i] = "?";  //se è cosi sostituisco alla X il ?
                                int occur = map.get(tent[i]); // elimino lettera dalla mappa o decremento la sua occorrenza
                                if (occur == 1) map.remove(s_word[i]);
                                else {map.replace(s_word[i], occur, occur-1);}
                            }
                        }
                    }
                    //transformo l'array di stringhe in una stringa 
                    StringBuilder sb = new StringBuilder(""); 
                    for (int i=0; i<indizzi.length; i++)sb.append(indizzi[i]);
                    String indizio = sb.toString();

                    dos.writeInt(0); //comunico al client che la parola è valida e che aspetti l'indizio
                    dos.writeUTF(indizio);//invio al client il messaggio che rappresenta gli indizzi 
                    if (indizio.equals("++++++++++")){ //se indizio è tutti + la parola è stata indovinata 
                        //esco dal while 
                        break;
                    }

                }
                else {//messaggio al client che la stringa non è presente nel dizionario 
                    dos.writeInt(2);
                    continue; //perchè non lo considerò un tentativo valido
                }
            }
            else {//messaggio a client stringa lunghezza errata
                dos.writeInt(1);
                continue; //perchè non lo considerò un tentativo valido
            }

            n_tentativi++;
        }
        // a prescindere dall'esito della partita se arrivo qui è conclusa e incremento di 1 il numero di partite giocate
        up_games_played(user);
        if (n_tentativi==13){//modifico statistiche successive ad una sconfitta
            calc_vittory_rate(user);
            reset_vittory_streak(user);
        }
        else {//modifico statistiche successive ad una vittoria 
            up_games_won(user);
            calc_vittory_rate(user);
            up_vittory_streak(user);
            up_max_vittory_streak(user);
            up_guess_distribution(user, n_tentativi);
        }


    }


    //METODO USATO DA REGISTER
    //Sovrascrive il file users.json con l'users_list aggiornata 
    public synchronized void backup_server_users(ArrayList<User> users_list) throws IOException{
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File users_file = new File("users.json");
        FileWriter fw = new FileWriter(users_file);
        String s_json = gson.toJson(users_list);
        fw.write(s_json);
        fw.close();
    }

    //METODI USATI DA PLAY WORDLE

    //controlla lunghezza parola se ha 10 lettere true altrimenti false
    public static boolean check_length(String tentativo){
        if (tentativo.length()==10)return true;
        else return false;
    }

    //controlla se la parola esiste nel dizionario e restituisce true o false
    public static boolean check_exist_dictionary(String tentativo, ArrayList<String> dizionario){
        if (dizionario.contains(tentativo))return true;
        else return false;
    }

    //CREA MAPPA CON LETTERE COME CHIAVI E COME VALORI LE LORO OCCORRENZE IN UNA STRINGA PASSATA
    public static HashMap<Character,Integer> map_secret_word(String secret_word){
        //trasformo parola in un array di caratteri
        char[]  arr = secret_word.toCharArray();
        HashMap<Character,Integer> map = new HashMap<Character,Integer>();
        for (int i=0; i<arr.length; i++){
            if (map.containsKey(arr[i])){ //se chiave gia presente la aggiorno aumentando occorrenza di 1
                int v = map.get(arr[i]);
                map.replace(arr[i],v,v+1);
            }
            else map.put(arr[i],1); //altrimenti aggiungo chiave ed elemento
        }
        return map;
    }

    //DOPO OGNI PARTITA DEVO SALVARE LA MODIFICA ALLE STATISTICHE FACENDO UN BACKUP AL SERVER
    //METODI PER AGGIORNARE LE STATISTICHE DELL'UTENTE DOPO UNA PARTITA 

    public static void up_games_played(User user){
        user.games_played++;
    }

    public static void up_games_won(User user){
        user.games_won++;
    }

    public static void calc_vittory_rate(User user){
        user.victory_rate = (int) (((float)user.games_won/(float)user.games_played)*100);
    }

    public static void up_vittory_streak(User user){
        user.victory_streak++;
    }

    public static void reset_vittory_streak(User user){
        user.victory_streak = 0;
    }

    public static void up_max_vittory_streak(User user){
        if (user.victory_streak>user.max_victory_streak){
            user.max_victory_streak=user.victory_streak;
        }
    }

    public static void up_guess_distribution(User user,int tentativo){
        int old_value = user.guess_distribution.get(tentativo);
        user.guess_distribution.replace(tentativo,old_value,old_value+1);
    }

    //PRINT STATISTICS
    //comunica al client tutte le statistiche di un utente 
    public static void print_statistics(User user,DataOutputStream dos) throws IOException{
        dos.writeInt(user.games_played);
        dos.writeInt(user.games_won);
        dos.writeInt(user.victory_rate);
        dos.writeInt(user.victory_streak);
        dos.writeInt(user.max_victory_streak);
        for (int i=1; i<=12; i++){
            dos.writeInt(user.guess_distribution.get(i));
        }
    }

    // METODO PER INVIARE UN MESSAGGIO AL GRUPPO MULTICAST
    //ogni volta che qualcuno vuole mandare un messaggio si collegga al gruppo e lo invia e poi si disconnette
    //efficiente se non tutti pubblicano inefficiente se tutti pubblicano e piu volte 
    public static void mess_sender(int port, String host, int n_tentativi,String username) throws UnknownHostException {
        InetAddress ia = InetAddress.getByName(host);
        
        try (DatagramSocket ds = new DatagramSocket(0)) {
            String mess;
            //in base al numero di tentativi costruisce il messaggio
            if(n_tentativi==0){ // 0 tentativi significa che l'utente non ha indovinato la parola
                mess = username+" didn't guess this word";
            }
            else{
                mess = username+" guessed this word in "+n_tentativi+" attempts";
            }
            byte[] data = mess.getBytes("US-ASCII");
            //metto stringa in un un pacchetto e la invio 
            DatagramPacket dp = new DatagramPacket(data, data.length, ia, port);
            ds.send(dp);
            //non chiudo il datagramsocket tanto ci pensa il try 
        }
        catch(SocketException e){
            e.printStackTrace();
        }
        catch(UnsupportedEncodingException e){
            e.printStackTrace();
        }
        catch(IOException e){
            e.printStackTrace();
        }

    }

    //METODO AUSILIARI PER CONTROLLARE IL CODICE 
    public void print_list(ArrayList<User> users_list){
        Iterator<User> iter = users_list.iterator();
        while(iter.hasNext()){
            User user = iter.next();
            System.out.println(user.username + user.password);
        }
    }

}
